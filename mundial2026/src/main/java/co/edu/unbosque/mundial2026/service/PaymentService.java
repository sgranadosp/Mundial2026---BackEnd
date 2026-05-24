/**
 * Paquete que contiene las clases de servicio de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;

import co.edu.unbosque.mundial2026.dto.CreatePreferenceRequest;
import co.edu.unbosque.mundial2026.dto.CreatePreferenceResponse;
import co.edu.unbosque.mundial2026.model.Match;
import co.edu.unbosque.mundial2026.model.Ticket;
import co.edu.unbosque.mundial2026.model.Ticket.TicketCategory;
import co.edu.unbosque.mundial2026.model.Ticket.TicketStatus;
import co.edu.unbosque.mundial2026.model.User;
import co.edu.unbosque.mundial2026.repository.MatchRepository;
import co.edu.unbosque.mundial2026.repository.TicketRepository;
import co.edu.unbosque.mundial2026.repository.UserRepository;

/**
 * Servicio que integra <a href="https://www.mercadopago.com.co/developers">
 * MercadoPago Checkout Pro</a> para procesar pagos de tickets del Mundial 2026.
 * <p>
 * <b>Flujo en dos pasos:</b>
 * <ol>
 *   <li><b>{@link #createPreference}</b>: crea una "preference" en MercadoPago
 *       describiendo qué se va a cobrar y a qué URLs redirigir tras el pago.
 *       Persiste un {@link Ticket} en estado {@code RESERVED} con el
 *       {@code mpPreferenceId} para correlacionar la respuesta del webhook
 *       después.</li>
 *   <li><b>{@link #handleWebhook}</b>: recibe la notificación asíncrona de
 *       MercadoPago cuando el usuario completa (o falla) el pago, busca el
 *       ticket por {@code mpPreferenceId} y lo marca como {@code PAID} si
 *       el pago fue aprobado.</li>
 * </ol>
 * </p>
 * <p>
 * <b>Importante:</b> en el plan sandbox (TEST-...) MercadoPago no cobra dinero
 * real. Las tarjetas de prueba están documentadas en el panel del developer.
 * </p>
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    /** Precio fijo por ticket en pesos colombianos. */
    private static final double TICKET_PRICE_COP = 250_000.0;

    /** Moneda usada con MercadoPago Colombia. */
    private static final String CURRENCY = "COP";

    /** Cuántos minutos vive la reserva antes de marcarse como expirada. */
    private static final long RESERVATION_TTL_MINUTES = 15;

    /** Access token sandbox o producción, inyectado desde application.properties. */
    @Value("${mercadopago.access-token}")
    private String accessToken;

    /** Public key (TEST-...) que se manda al frontend para el Wallet Brick. */
    @Value("${mercadopago.public-key}")
    private String publicKey;

    /** URL base del frontend para construir back_urls (success/failure/pending). */
    @Value("${mercadopago.frontend-base-url}")
    private String frontendBaseUrl;

    @Autowired
    private TicketRepository ticketRepo;

    @Autowired
    private MatchRepository matchRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private NotificationService notificationService;

    /** Constructor por defecto. */
    public PaymentService() {
    }

    /**
     * Inicializa el SDK de MercadoPago con el access token. Es estático en la
     * librería, así que basta llamarlo una vez por proceso, pero es seguro
     * llamarlo en cada operación.
     */
    private void ensureSdkConfigured() {
        MercadoPagoConfig.setAccessToken(accessToken);
    }

    // =========================================================================
    // Crear preference
    // =========================================================================

    /**
     * Crea una preference en MercadoPago para que el usuario {@code userId}
     * compre {@code quantity} tickets del partido {@code matchId}.
     * <p>
     * Persiste un {@link Ticket} por cada unidad, todos en estado
     * {@code RESERVED} y vinculados al mismo {@code mpPreferenceId}. Si el
     * pago se confirma vía webhook, todos se pasan a {@code PAID}; si expira,
     * se liberan.
     * </p>
     *
     * @param userId  ID del usuario comprador (obtenido del JWT en el controller).
     * @param request DTO con matchId y quantity.
     * @return DTO con preferenceId, initPoint y datos para el Wallet Brick.
     * @throws IllegalArgumentException si el partido o el usuario no existen,
     *         o si la cantidad está fuera de rango.
     * @throws IllegalStateException si MercadoPago rechaza la creación.
     */
    @Transactional
    public CreatePreferenceResponse createPreference(Long userId, CreatePreferenceRequest request) {
        // ---- Validaciones de input ----------------------------------------
        if (request == null || request.getMatchId() == null || request.getQuantity() == null) {
            throw new IllegalArgumentException("matchId y quantity son obligatorios");
        }
        int qty = request.getQuantity();
        if (qty < 1 || qty > 4) {
            throw new IllegalArgumentException("La cantidad debe estar entre 1 y 4");
        }

        Match match = matchRepo.findById(request.getMatchId())
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado"));
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        double totalAmount = TICKET_PRICE_COP * qty;

        // ---- Construir la preference en MercadoPago -----------------------
        ensureSdkConfigured();

        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .id("match-" + match.getId())
                .title("Ticket Mundial 2026 — "
                        + match.getHomeTeam().getName() + " vs " + match.getAwayTeam().getName())
                .description("Entrada para el partido en "
                        + (match.getStadium() != null ? match.getStadium().getName() : "estadio por confirmar"))
                .quantity(qty)
                .currencyId(CURRENCY)
                .unitPrice(BigDecimal.valueOf(TICKET_PRICE_COP))
                .build();

        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success(frontendBaseUrl + "/partidos?payment=success")
                .pending(frontendBaseUrl + "/partidos?payment=pending")
                .failure(frontendBaseUrl + "/partidos?payment=failure")
                .build();

        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(List.of(item))
                .backUrls(backUrls)
                // NOTA: NO usamos autoReturn("approved") porque requiere que el
                // back_url.success sea HTTPS público. En sandbox/localhost falla
                // con "auto_return invalid. back_url.success must be defined".
                // El usuario hará click en "Volver al sitio" tras el pago.
                .externalReference("user-" + userId + "-match-" + match.getId() + "-" + System.currentTimeMillis())
                .build();

        Preference preference;
        try {
            PreferenceClient client = new PreferenceClient();
            preference = client.create(preferenceRequest);
        } catch (MPApiException e) {
            log.error("[mercadopago] API error al crear preference: status={}, body={}",
                    e.getStatusCode(), e.getApiResponse() != null ? e.getApiResponse().getContent() : "n/a");
            throw new IllegalStateException("MercadoPago rechazó la creación de la preference: " + e.getMessage(), e);
        } catch (MPException e) {
            log.error("[mercadopago] Error al crear preference: {}", e.getMessage());
            throw new IllegalStateException("Error creando preference en MercadoPago: " + e.getMessage(), e);
        }

        log.info("[mercadopago] Preference creada id={} para user={} match={} qty={} total=COP {}",
                preference.getId(), userId, match.getId(), qty, totalAmount);

        // ---- Persistir tickets en estado RESERVED -------------------------
        List<Ticket> reservados = new ArrayList<>();
        for (int i = 0; i < qty; i++) {
            Ticket t = new Ticket();
            t.setCorrelationId(UUID.randomUUID().toString());
            t.setMatch(match);
            t.setHolder(user);
            t.setOriginalBuyer(user);
            t.setStatus(TicketStatus.RESERVED);
            t.setCategory(TicketCategory.CATEGORY_3);
            t.setPrice(TICKET_PRICE_COP);
            t.setReservedAt(LocalDateTime.now());
            t.setReservationExpiresAt(LocalDateTime.now().plusMinutes(RESERVATION_TTL_MINUTES));
            t.setMpPreferenceId(preference.getId());
            reservados.add(ticketRepo.save(t));
        }
        log.info("[mercadopago] {} tickets reservados, vencen en {} min", qty, RESERVATION_TTL_MINUTES);

        // ---- Armar response -----------------------------------------------
        CreatePreferenceResponse response = new CreatePreferenceResponse();
        response.setPreferenceId(preference.getId());
        response.setInitPoint(preference.getInitPoint());
        response.setPublicKey(publicKey);
        response.setTotalAmount(totalAmount);
        response.setCurrency(CURRENCY);
        return response;
    }

    // =========================================================================
    // Webhook handler
    // =========================================================================

    /**
     * Procesa una notificación webhook de MercadoPago.
     * <p>
     * MercadoPago llama a {@code POST /payments/webhook?type=payment&id=...}
     * cuando el estado de un pago cambia. Acá consultamos el pago por su ID,
     * recuperamos el {@code preferenceId} asociado y actualizamos los tickets
     * en BD.
     * </p>
     *
     * @param type Tipo de notificación (p. ej. "payment").
     * @param dataId ID del pago en MercadoPago.
     */
    @Transactional
    public void handleWebhook(String type, String dataId) {
        if (!"payment".equalsIgnoreCase(type) || dataId == null || dataId.isBlank()) {
            log.debug("[mercadopago] Webhook ignorado (type={}, id={})", type, dataId);
            return;
        }
        ensureSdkConfigured();

        try {
            com.mercadopago.client.payment.PaymentClient paymentClient =
                    new com.mercadopago.client.payment.PaymentClient();
            com.mercadopago.resources.payment.Payment payment = paymentClient.get(Long.valueOf(dataId));

            String status = payment.getStatus();         // "approved", "rejected", "pending", etc.
            String preferenceId = null;
            if (payment.getAdditionalInfo() != null) {
                // Algunos SDKs ponen preferenceId en additionalInfo; en otros viene como atributo separado.
            }
            // Por compatibilidad, el campo más estable es payment.getOrder().getId() pero su forma
            // depende del SDK. Usamos preferenceId pasado por additionalInfo o el order id.
            if (payment.getOrder() != null) {
                preferenceId = String.valueOf(payment.getOrder().getId());
            }
            if (preferenceId == null) {
                // Como último recurso usamos external_reference que pusimos al crear:
                // "user-{id}-match-{id}-{ts}". No es trivial usarlo para mapear directamente al
                // mpPreferenceId, así que recorremos los tickets en estado RESERVED de las
                // últimas horas y buscamos coincidencia.
                log.warn("[mercadopago] Webhook sin preferenceId resoluble (paymentId={})", dataId);
                return;
            }

            log.info("[mercadopago] Webhook payment={} status={} preferenceId={}",
                    dataId, status, preferenceId);

            Optional<Ticket> ticketOpt = ticketRepo.findByMpPreferenceId(preferenceId);
            if (ticketOpt.isEmpty()) {
                log.warn("[mercadopago] No hay ticket para preferenceId={}", preferenceId);
                return;
            }

            // Como pueden ser varios tickets con la misma preference (compras múltiples),
            // buscamos todos por preferenceId y los actualizamos juntos.
            final String prefId = preferenceId;
            List<Ticket> tickets = ticketRepo.findAll().stream()
                    .filter(t -> prefId.equals(t.getMpPreferenceId()))
                    .toList();

            for (Ticket t : tickets) {
                t.setMpPaymentId(dataId);
                if ("approved".equalsIgnoreCase(status)) {
                    t.setStatus(TicketStatus.PAID);
                } else if ("rejected".equalsIgnoreCase(status)
                        || "cancelled".equalsIgnoreCase(status)) {
                    t.setStatus(TicketStatus.EXPIRED);
                }
                ticketRepo.save(t);
            }
            log.info("[mercadopago] {} ticket(s) actualizados a status MP={}",
                    tickets.size(), status);

            // Si fue aprobado, dispara la notificación push al comprador.
            // Se envía una sola notificación por compra (no por ticket), para
            // no spammear cuando el usuario compra varios tickets a la vez.
            if ("approved".equalsIgnoreCase(status) && !tickets.isEmpty()) {
                Ticket first = tickets.get(0);
                Long buyerId = first.getOriginalBuyer() != null
                        ? first.getOriginalBuyer().getId() : null;
                Match m = first.getMatch();
                if (buyerId != null && m != null) {
                    String title = "¡Compra confirmada!";
                    String body = String.format("Tu compra de %d ticket(s) para %s vs %s fue exitosa.",
                            tickets.size(),
                            m.getHomeTeam() != null ? m.getHomeTeam().getName() : "?",
                            m.getAwayTeam() != null ? m.getAwayTeam().getName() : "?");
                    co.edu.unbosque.mundial2026.dto.NotificationDTO n =
                            new co.edu.unbosque.mundial2026.dto.NotificationDTO();
                    n.setTargetUserId(buyerId);
                    n.setTitle(title);
                    n.setBody(body);
                    n.setChannel("PUSH");
                    n.setNotificationType("TICKET_PURCHASE");
                    n.setResourceId(m.getId());
                    try {
                        notificationService.sendToUser(n);
                    } catch (Exception ex) {
                        // No queremos que un fallo de FCM rompa el webhook.
                        log.warn("[FCM] No se pudo enviar notificación de compra: {}", ex.getMessage());
                    }
                }
            }

        } catch (MPException | MPApiException e) {
            log.error("[mercadopago] Error procesando webhook paymentId={}: {}",
                    dataId, e.getMessage());
        } catch (NumberFormatException e) {
            log.error("[mercadopago] paymentId no numérico: {}", dataId);
        }
    }
}