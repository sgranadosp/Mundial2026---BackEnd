package co.edu.unbosque.mundial2026;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import co.edu.unbosque.mundial2026.dto.PredictionDTO;
import co.edu.unbosque.mundial2026.model.Match;
import co.edu.unbosque.mundial2026.model.Match.MatchStatus;
import co.edu.unbosque.mundial2026.model.PollGroup;
import co.edu.unbosque.mundial2026.model.Ticket;
import co.edu.unbosque.mundial2026.model.Ticket.TicketCategory;
import co.edu.unbosque.mundial2026.model.Ticket.TicketStatus;
import co.edu.unbosque.mundial2026.model.User;
import co.edu.unbosque.mundial2026.repository.MatchRepository;
import co.edu.unbosque.mundial2026.repository.PollGroupRepository;
import co.edu.unbosque.mundial2026.repository.PredictionRepository;
import co.edu.unbosque.mundial2026.repository.TicketRepository;
import co.edu.unbosque.mundial2026.repository.UserRepository;
import co.edu.unbosque.mundial2026.service.AuditEventService;
import co.edu.unbosque.mundial2026.service.NotificationService;
import co.edu.unbosque.mundial2026.service.PackService;
import co.edu.unbosque.mundial2026.service.PollService;
import co.edu.unbosque.mundial2026.service.TicketService;
import co.edu.unbosque.mundial2026.util.AESUtil;
import co.edu.unbosque.mundial2026.util.DateTimeUtil;
import co.edu.unbosque.mundial2026.util.ScoringUtil;

/**
 * Suite de pruebas unitarias críticas de la plataforma Mundial 2026 Hub.
 *
 * <p>
 * <b>Estrategia general.</b> Se utilizan pruebas unitarias puras (JUnit 5) y
 * pruebas con mocks (Mockito) en lugar de {@code @SpringBootTest}. La razón es
 * que el contexto completo de Spring exige conexión a la base de datos MySQL en
 * GCP y a servicios externos (Firebase, SMTP, MercadoPago), por lo que un test
 * "unitario" que levante el contexto se vuelve lento y frágil. Aquí cada prueba
 * se ejecuta en milisegundos y aísla una sola unidad de comportamiento.
 * </p>
 *
 * <p>
 * <b>Cobertura.</b> Las 10 pruebas atacan la lógica más crítica del sistema:
 * </p>
 * <ul>
 * <li>Reglas de puntuación de pollas ({@link ScoringUtil}, pruebas 1-4): son el
 * corazón del módulo de pronósticos y un fallo aquí afecta a todos los
 * rankings.</li>
 * <li>Cifrado de datos sensibles ({@link AESUtil}, pruebas 5-6): protege los
 * emails y nombres de los usuarios en la base de datos.</li>
 * <li>Lógica temporal de reservas ({@link DateTimeUtil}, prueba 7): gobierna la
 * expiración automática de tickets.</li>
 * <li>Reglas de seguridad y ciclo de vida de tickets ({@link TicketService},
 * pruebas 8-9): evitan transferencias y cancelaciones no autorizadas.</li>
 * <li>Reglas de negocio de pronósticos ({@link PollService}, prueba 10): impide
 * pronosticar partidos ya finalizados.</li>
 * </ul>
 */
class Mundial2026ApplicationTests {

	// =========================================================================
	// ScoringUtil - reglas de puntuación de pollas (HU16, HU17)
	// =========================================================================

	/**
	 * Pruebas sobre {@link ScoringUtil}. La utilidad es {@code @Component} pero no
	 * tiene dependencias, así que se instancia directamente con {@code new} para
	 * tests puros y rápidos.
	 */
	@Nested
	@DisplayName("ScoringUtil — reglas de puntuación")
	class ScoringUtilTests {

		private final ScoringUtil scoringUtil = new ScoringUtil();

		/**
		 * Test 1 — Marcador exacto otorga 3 puntos.
		 *
		 * <p>
		 * Caso central de la tabla de puntuación: cuando el usuario acierta exactamente
		 * los goles de local y visitante, debe recibir el valor máximo
		 * ({@code POINTS_EXACT_SCORE = 3}). Si esta prueba falla, todo el ranking de
		 * pollas reporta puntajes inflados o deflacionados.
		 * </p>
		 */
		@Test
		@DisplayName("Marcador exacto → 3 puntos")
		void exactScoreReturnsThreePoints() {
			int points = scoringUtil.calculatePoints(2, 1, 2, 1);
			assertEquals(ScoringUtil.POINTS_EXACT_SCORE, points,
					"Predicción 2-1 contra resultado real 2-1 debe valer 3 puntos");
		}

		/**
		 * Test 2 — Resultado correcto (ganador acertado pero marcador inexacto) otorga
		 * 1 punto.
		 *
		 * <p>
		 * El usuario predice que gana el local 3-0 y el partido termina 1-0: acertó al
		 * ganador pero no el marcador. Debe recibir {@code POINTS_CORRECT_RESULT = 1}.
		 * Si se devolviera 0 o 3 aquí, la regla "intermedia" del scoring quedaría rota.
		 * </p>
		 */
		@Test
		@DisplayName("Resultado correcto (sin marcador exacto) → 1 punto")
		void correctResultReturnsOnePoint() {
			int points = scoringUtil.calculatePoints(3, 0, 1, 0);
			assertEquals(ScoringUtil.POINTS_CORRECT_RESULT, points,
					"Predicción 3-0 con resultado real 1-0 (mismo ganador) debe valer 1 punto");
		}

		/**
		 * Test 3 — Predicción incorrecta (ganador equivocado) otorga 0 puntos.
		 *
		 * <p>
		 * El usuario predice que gana el local 2-0 y el partido termina 0-3 (gana el
		 * visitante). No acertó nada y debe recibir {@code POINTS_NO_MATCH = 0}. Esta
		 * prueba captura cualquier bug donde una predicción totalmente errada otorgara
		 * puntos por error (por ejemplo, una mala comparación de signos en
		 * {@code isCorrectResult}).
		 * </p>
		 */
		@Test
		@DisplayName("Ganador equivocado → 0 puntos")
		void wrongWinnerReturnsZeroPoints() {
			int points = scoringUtil.calculatePoints(2, 0, 0, 3);
			assertEquals(ScoringUtil.POINTS_NO_MATCH, points,
					"Predicción 2-0 contra resultado 0-3 no debe otorgar puntos");
		}

		/**
		 * Test 4 — Predicción {@code null} no rompe y devuelve 0.
		 *
		 * <p>
		 * El servicio puede recibir predicciones donde el usuario no haya registrado
		 * uno o ambos marcadores (por ejemplo, si el partido fue pospuesto antes de que
		 * ingresara su pronóstico). El método debe tolerar {@code null} sin lanzar
		 * {@code NullPointerException} y devolver 0. Si esta prueba falla, la
		 * evaluación batch de predicciones tras un partido aborta a mitad de proceso.
		 * </p>
		 */
		@Test
		@DisplayName("Predicción null → 0 puntos sin NullPointerException")
		void nullPredictionReturnsZeroPointsSafely() {
			int pointsHomeNull = scoringUtil.calculatePoints(null, 1, 0, 0);
			int pointsAwayNull = scoringUtil.calculatePoints(1, null, 0, 0);
			int pointsBothNull = scoringUtil.calculatePoints(null, null, 0, 0);

			assertEquals(ScoringUtil.POINTS_NO_MATCH, pointsHomeNull, "predictedHome null debe retornar 0 puntos");
			assertEquals(ScoringUtil.POINTS_NO_MATCH, pointsAwayNull, "predictedAway null debe retornar 0 puntos");
			assertEquals(ScoringUtil.POINTS_NO_MATCH, pointsBothNull, "ambos null debe retornar 0 puntos");
		}
	}

	// =========================================================================
	// AESUtil - cifrado de datos sensibles del usuario
	// =========================================================================

	/**
	 * Pruebas sobre {@link AESUtil}, la utilidad estática que cifra los campos
	 * sensibles del usuario (email, nombre, código de verificación) antes de
	 * persistirlos. Cualquier regresión aquí compromete la integridad de los datos
	 * en MySQL y rompe el login (que decodifica el email para resolver usuarios).
	 */
	@Nested
	@DisplayName("AESUtil — cifrado AES/GCM")
	class AESUtilTests {

		/**
		 * Test 5 — El ciclo encrypt → decrypt devuelve el texto original.
		 *
		 * <p>
		 * Es la garantía fundamental del cifrado simétrico: lo que se guarda cifrado
		 * debe poder leerse como el plaintext original. Si esta prueba falla, los
		 * usuarios no pueden volver a iniciar sesión y los emails guardados quedan
		 * ilegibles.
		 * </p>
		 *
		 * <p>
		 * Adicionalmente verifico que el texto cifrado SÍ es distinto del plaintext: si
		 * fueran iguales, el método estaría devolviendo el mismo texto y no cifrando
		 * nada.
		 * </p>
		 */
		@Test
		@DisplayName("Round-trip encrypt/decrypt preserva el plaintext")
		void encryptDecryptRoundTripPreservesPlaintext() {
			String plaintext = "usuario.correo@unbosque.edu.co";

			String encrypted = AESUtil.encrypt(plaintext);
			String decrypted = AESUtil.decrypt(encrypted);

			assertNotNull(encrypted, "El cifrado no debe devolver null para texto válido");
			assertNotEquals(plaintext, encrypted,
					"El texto cifrado no puede ser igual al plaintext (no estaría cifrando)");
			assertEquals(plaintext, decrypted, "El round-trip debe devolver exactamente el texto original");
		}

		/**
		 * Test 6 — La utilidad tolera {@code null} sin lanzar excepción.
		 *
		 * <p>
		 * En el flujo de registro y actualización de usuario, algunos campos opcionales
		 * pueden venir en {@code null}. La utilidad debe propagar {@code null} en vez
		 * de lanzar {@code NullPointerException}. Esta prueba protege contra un crash
		 * silencioso en el endpoint de registro cuando, por ejemplo, un cliente no
		 * envía el campo {@code name}.
		 * </p>
		 */
		@Test
		@DisplayName("Encrypt/decrypt de null devuelve null sin excepción")
		void encryptAndDecryptHandleNullGracefully() {
			assertNull(AESUtil.encrypt(null), "encrypt(null) debe devolver null");
			assertNull(AESUtil.decrypt(null), "decrypt(null) debe devolver null");
		}
	}

	// =========================================================================
	// DateTimeUtil - lógica temporal del torneo
	// =========================================================================

	/**
	 * Pruebas sobre {@link DateTimeUtil}, que gobierna la lógica de expiración de
	 * reservas de tickets y el bloqueo automático de pronósticos. Un bug aquí
	 * permite vender la misma silla dos veces o que usuarios pronostiquen partidos
	 * ya iniciados.
	 */
	@Nested
	@DisplayName("DateTimeUtil — expiración temporal")
	class DateTimeUtilTests {

		private final DateTimeUtil util = new DateTimeUtil();

		/**
		 * Test 7 — Una reserva con fecha pasada se considera expirada; una con fecha
		 * futura no.
		 *
		 * <p>
		 * Este método se invoca desde el job batch de expiración de reservas y desde el
		 * flujo de pago. Si una reserva pasada NO se reporta como expirada, el cupo
		 * nunca se libera para otros compradores. Si una reserva futura SÍ se reporta
		 * como expirada, el usuario titular pierde su asiento antes de tiempo.
		 * </p>
		 */
		@Test
		@DisplayName("isReservationExpired clasifica correctamente pasado/futuro")
		void reservationExpiresOnlyWhenPast() {
			// DateTimeUtil compara contra UTC; el test construye las fechas en la misma
			// zona para no ser sensible al timezone del sistema (Bogotá es UTC-5).
			LocalDateTime past = LocalDateTime.now(DateTimeUtil.UTC).minusMinutes(1);
			LocalDateTime future = LocalDateTime.now(DateTimeUtil.UTC).plusMinutes(10);

			assertTrue(util.isReservationExpired(past), "Una reserva con fecha hace 1 min debe estar expirada");
			assertFalse(util.isReservationExpired(future),
					"Una reserva con fecha dentro de 10 min NO debe estar expirada");
		}

		// =========================================================================
		// TicketService - seguridad y ciclo de vida de tickets
		// =========================================================================

		/**
		 * Pruebas sobre {@link TicketService} con mocks de Mockito. No se levanta
		 * Spring; los colaboradores se inyectan vía {@code @InjectMocks}. El foco está
		 * en las reglas de autorización y transición de estados, no en la persistencia.
		 */
		@ExtendWith(MockitoExtension.class)
		@Nested
		@DisplayName("TicketService — seguridad y ciclo de vida")
		class TicketServiceTests {

			@Mock
			private TicketRepository ticketRepo;
			@Mock
			private MatchRepository matchRepo;
			@Mock
			private UserRepository userRepo;
			@Mock
			private AuditEventService auditService;
			@Mock
			private ModelMapper modelMapper;

			@InjectMocks
			private TicketService ticketService;

			/**
			 * Test 8 — {@code transferTicket} rechaza la operación si el usuario que pide
			 * la transferencia NO es el titular actual del ticket.
			 *
			 * <p>
			 * Esta es una regla de seguridad crítica: sin esta validación, cualquier
			 * usuario autenticado podría transferir tickets de otros usuarios con solo
			 * conocer el ID. La prueba simula el escenario: el ticket pertenece al usuario
			 * 10 pero el usuario 99 pide transferirlo al usuario 20. Debe devolver el
			 * código 4 (operación no permitida) y NUNCA llamar a {@code ticketRepo.save()}.
			 * </p>
			 */
			@Test
			@DisplayName("transferTicket rechaza si el solicitante no es el titular")
			void transferTicketRejectsNonHolder() {
				// Arrange — ticket cuyo holder es el usuario 10, en estado PAID
				User trueHolder = buildUser(10L);
				User newHolder = buildUser(20L);

				Ticket ticket = new Ticket();
				ticket.setId(1L);
				ticket.setHolder(trueHolder);
				ticket.setStatus(TicketStatus.PAID);

				when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));
				when(userRepo.findById(20L)).thenReturn(Optional.of(newHolder));

				// Act — un atacante (id 99) intenta transferir el ticket
				int result = ticketService.transferTicket(1L, 99L, 20L);

				// Assert — debe devolver 4 (no permitido) y no persistir nada
				assertEquals(4, result, "Un usuario que no es el titular no puede transferir; debe retornar 4");
				verify(ticketRepo, never()).save(any(Ticket.class));
				verify(auditService, never()).logTicketTransferred(anyLong(), anyLong(), anyString());
			}

			/**
			 * Test 9 — {@code cancelReservation} hecha por el titular sobre un ticket
			 * RESERVED lo marca como EXPIRED y registra auditoría.
			 *
			 * <p>
			 * Cubre el flujo feliz de cancelación voluntaria: el usuario titular libera su
			 * propia reserva antes de pagar. Se verifica: (a) el código de retorno es 0
			 * (éxito), (b) el estado del ticket pasa de RESERVED a EXPIRED, (c) se persiste
			 * el cambio, (d) se registra el evento de auditoría {@code TICKET_EXPIRED} con
			 * el {@code correlationId} del ticket.
			 * </p>
			 */
			@Test
			@DisplayName("cancelReservation por el titular pasa de RESERVED a EXPIRED")
			void cancelReservationSuccessfullyExpiresTicket() {
				// Arrange — ticket RESERVED del usuario 10
				User holder = buildUser(10L);

				Ticket ticket = new Ticket();
				ticket.setId(5L);
				ticket.setHolder(holder);
				ticket.setStatus(TicketStatus.RESERVED);
				ticket.setCorrelationId("CID-TEST-1");

				when(ticketRepo.findById(5L)).thenReturn(Optional.of(ticket));

				// Act
				int result = ticketService.cancelReservation(5L, 10L);

				// Assert — éxito + transición + auditoría
				assertEquals(0, result, "Cancelación exitosa debe retornar 0");
				assertEquals(TicketStatus.EXPIRED, ticket.getStatus(),
						"El ticket debe quedar marcado como EXPIRED tras cancelar");
				verify(ticketRepo, times(1)).save(ticket);
				verify(auditService, times(1)).logTicketExpired(10L, "CID-TEST-1");
			}

			/**
			 * Helper para construir usuarios mínimos con un id, sin tener que inicializar
			 * todos los campos de {@link User}.
			 */
			private User buildUser(Long id) {
				User u = new User();
				u.setId(id);
				return u;
			}
		}

		// =========================================================================
		// PollService - reglas de negocio de pronósticos
		// =========================================================================

		/**
		 * Pruebas sobre {@link PollService} con mocks. Como antes, no se carga Spring;
		 * los repositorios y servicios colaboradores son simulados.
		 */
		@ExtendWith(MockitoExtension.class)
		@Nested
		@DisplayName("PollService — reglas de pronósticos")
		class PollServiceTests {

			@Mock
			private PollGroupRepository pollGroupRepo;
			@Mock
			private PredictionRepository predictionRepo;
			@Mock
			private MatchRepository matchRepo;
			@Mock
			private UserRepository userRepo;
			@Mock
			private ScoringUtil scoringUtil;
			@Mock
			private ModelMapper modelMapper;
			@Mock
			private PackService packService;
			@Mock
			private NotificationService notificationService;

			@InjectMocks
			private PollService pollService;

			/**
			 * Test 10 — {@code submitPrediction} NO permite registrar un pronóstico sobre
			 * un partido que ya finalizó.
			 *
			 * <p>
			 * Es una regla de negocio crítica para la integridad del ranking: sin ella, un
			 * usuario podría pronosticar el resultado real DESPUÉS de que el partido
			 * termina y sumar 3 puntos automáticos. La regla está implementada como
			 * {@code if (match.status != SCHEDULED) return 4}. La prueba verifica: (a) el
			 * método retorna 4 (operación no permitida), (b) NO se persiste la predicción,
			 * (c) NO se notifica a PackService (no debe haber side effects).
			 * </p>
			 */
			@Test
			@DisplayName("submitPrediction rechaza partidos en estado FINISHED")
			void submitPredictionRejectsFinishedMatch() {
				// Arrange — partido FINISHED (ya jugado), usuario y grupo válidos
				User user = new User();
				user.setId(7L);

				Match finishedMatch = new Match();
				finishedMatch.setId(100L);
				finishedMatch.setStatus(MatchStatus.FINISHED);

				PollGroup group = new PollGroup();
				group.setId(50L);

				PredictionDTO dto = new PredictionDTO();
				dto.setMatchId(100L);
				dto.setPollGroupId(50L);
				dto.setPredictedHomeScore(2);
				dto.setPredictedAwayScore(1);

				when(matchRepo.findById(100L)).thenReturn(Optional.of(finishedMatch));
				when(pollGroupRepo.findById(50L)).thenReturn(Optional.of(group));
				when(userRepo.findById(7L)).thenReturn(Optional.of(user));

				// Act
				int result = pollService.submitPrediction(dto, 7L);

				// Assert — código 4 (no permitido), sin persistir, sin side effects
				assertEquals(4, result, "Pronóstico sobre partido FINISHED debe retornar 4 (no permitido)");
				verify(predictionRepo, never()).save(any());
				verify(packService, never()).onPredictionSubmitted(anyLong());
			}
		}
	}
}
