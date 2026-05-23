/**
 * Paquete que contiene las clases de servicio de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Servicio de envío de correos electrónicos vía SMTP para la plataforma Mundial
 * 2026 Hub.
 * <p>
 * Encapsula el uso de {@link JavaMailSender} configurado a través de las
 * propiedades {@code spring.mail.*} en {@code application.properties}. Provee
 * plantillas HTML responsivas para dos flujos:
 * <ul>
 * <li>Código de verificación al registrarse.</li>
 * <li>Código de recuperación de contraseña.</li>
 * </ul>
 * Ambas comparten un layout estilo "tarjeta" con header dorado, código
 * destacado en un recuadro con borde punteado, y footer institucional. Si SMTP
 * falla, los errores se loggean pero no se propagan al caller para que el flujo
 * de registro / recuperación no se interrumpa.
 * </p>
 */
@Service
public class EmailService {

	/**
	 * Logger para registrar éxitos y fallos del envío de correos.
	 */
	private static final Logger log = LoggerFactory.getLogger(EmailService.class);

	/**
	 * Color dorado principal de la marca (usado en headers y bordes).
	 */
	private static final String COLOR_DORADO = "#d6a049";

	/**
	 * Color dorado claro para gradiente del header.
	 */
	private static final String COLOR_DORADO_CLARO = "#e8c068";

	/**
	 * Color de fondo de la tarjeta principal (gris muy oscuro).
	 */
	private static final String COLOR_FONDO_TARJETA = "#1a1a1a";

	/**
	 * Color de fondo del recuadro del código.
	 */
	private static final String COLOR_FONDO_CODIGO = "#0f0f0f";

	/**
	 * Color del texto principal sobre fondo oscuro.
	 */
	private static final String COLOR_TEXTO = "#e8e8e8";

	/**
	 * Color del texto secundario (notas, footer, confidencialidad).
	 */
	private static final String COLOR_TEXTO_SECUNDARIO = "#888888";

	/**
	 * Sender de correos provisto automáticamente por Spring a partir de la
	 * configuración en {@code application.properties}.
	 */
	@Autowired
	private JavaMailSender mailSender;

	/**
	 * Cuenta remitente. Se inyecta desde {@code spring.mail.username} para mantener
	 * consistencia con la cuenta autenticada en SMTP.
	 */
	@Value("${spring.mail.username}")
	private String remitente;

	/**
	 * Constructor por defecto requerido por Spring.
	 */
	public EmailService() {
	}

	/**
	 * Envía un código de 6 dígitos al correo del destinatario en el contexto del
	 * flujo de verificación de cuenta tras un registro.
	 *
	 * @param destinatario  Correo del usuario que acaba de registrarse.
	 * @param nombreUsuario Nombre del usuario para personalizar el saludo.
	 * @param codigo        Código de 6 dígitos en texto plano.
	 */
	public void enviarCodigoVerificacion(String destinatario, String nombreUsuario, String codigo) {
		String asunto = "Verifica tu cuenta de Mundial 2026 Hub";
		String html = construirHtmlVerificacion(nombreUsuario, codigo);
		enviarHtml(destinatario, asunto, html);
	}

	/**
	 * Envía un código de 6 dígitos al correo del destinatario en el contexto del
	 * flujo de recuperación de contraseña.
	 *
	 * @param destinatario  Correo del usuario que solicitó la recuperación.
	 * @param nombreUsuario Nombre del usuario para personalizar el saludo.
	 * @param codigo        Código de 6 dígitos en texto plano.
	 */
	public void enviarCodigoRecuperacion(String destinatario, String nombreUsuario, String codigo) {
		String asunto = "Restablece tu contraseña de Mundial 2026 Hub";
		String html = construirHtmlRecuperacion(nombreUsuario, codigo);
		enviarHtml(destinatario, asunto, html);
	}

	/**
	 * Envía al creador de un intercambio el correo de confirmación cuando otro
	 * usuario aceptó su solicitud y se completó la transferencia de láminas.
	 *
	 * @param destinatario     Correo del creador de la solicitud.
	 * @param nombreUsuario    Nombre del creador para personalizar el saludo.
	 * @param accepterUsername Username del usuario que aceptó.
	 * @param entregada        Nombre legible de la lámina que el creador entregó.
	 * @param recibida         Nombre legible de la lámina que el creador recibió.
	 */
	public void enviarIntercambioCompletado(String destinatario, String nombreUsuario, String accepterUsername,
			String entregada, String recibida) {
		String asunto = "¡Tu intercambio se completó! — Mundial 2026 Hub";
		String html = construirHtmlIntercambio(nombreUsuario, accepterUsername, entregada, recibida);
		enviarHtml(destinatario, asunto, html);
	}

	// =========================================================================
	// Construcción de plantillas HTML
	// =========================================================================

	/**
	 * Construye el HTML del correo de verificación de cuenta tras el registro.
	 *
	 * @param nombre Nombre del usuario para personalizar el saludo.
	 * @param codigo Código de 6 dígitos a mostrar destacado.
	 * @return Cadena HTML completa lista para enviar como cuerpo del correo.
	 */
	private String construirHtmlVerificacion(String nombre, String codigo) {
		String saludo = (nombre == null || nombre.isBlank()) ? "¡Bienvenido a Mundial 2026 Hub!"
				: "¡Bienvenido a Mundial 2026 Hub, <strong>" + escapar(nombre) + "</strong>!";

		String introduccion = "Gracias por crear una cuenta. Estamos a un paso de tenerte adentro: solo nos falta "
				+ "confirmar que este correo te pertenece. Para completar tu registro y empezar a vivir "
				+ "el Mundial, introduce el siguiente código de verificación:";

		String instrucciones = "Ingresa este código en la pantalla de verificación que se abrió después del registro. "
				+ "Una vez verificado, podrás iniciar sesión y disfrutar de toda la experiencia del " + "Mundial 2026.";

		String avisos = "El código es de un solo uso. Si no fuiste tú quien creó esta cuenta, simplemente ignora "
				+ "este correo: la cuenta no se activará sin la verificación.";

		return construirPlantillaBase("Verificación de cuenta", "⚽", saludo, introduccion, codigo, instrucciones,
				avisos);
	}

	/**
	 * Construye el HTML del correo de recuperación de contraseña.
	 *
	 * @param nombre Nombre del usuario para personalizar el saludo.
	 * @param codigo Código de 6 dígitos a mostrar destacado.
	 * @return Cadena HTML completa lista para enviar como cuerpo del correo.
	 */
	private String construirHtmlRecuperacion(String nombre, String codigo) {
		String saludo = (nombre == null || nombre.isBlank()) ? "Recuperación de contraseña"
				: "Hola, <strong>" + escapar(nombre) + "</strong>";

		String introduccion = "Recibimos una solicitud para restablecer la contraseña de tu cuenta de Mundial 2026 Hub. "
				+ "Si tú la pediste, usa el siguiente código para continuar con el proceso:";

		String instrucciones = "Vuelve a la pantalla de recuperación en la aplicación, ingresa el código de arriba y "
				+ "define tu nueva contraseña. El código queda invalidado tras completar el cambio.";

		String avisos = "Si no solicitaste este cambio, puedes ignorar este correo: tu contraseña actual seguirá "
				+ "funcionando. Si crees que alguien intenta entrar a tu cuenta, cambia tu contraseña "
				+ "inmediatamente cuando recuperes el acceso.";

		return construirPlantillaBase("Recuperación de contraseña", "🔐", saludo, introduccion, codigo, instrucciones,
				avisos);
	}

	/**
	 * Construye el HTML del correo de notificación al creador cuando un intercambio
	 * se completó exitosamente.
	 */
	private String construirHtmlIntercambio(String nombre, String accepter, String entregada, String recibida) {
		String saludo = (nombre != null && !nombre.isBlank()) ? nombre : "amigo";
		return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
				+ "<body style='margin:0;padding:0;font-family:Arial,Helvetica,sans-serif;background:#1a1a1a;color:#e8e8e8;'>"
				+ "<div style='max-width:560px;margin:0 auto;background:#121212;border:2px solid #d6a049;border-radius:12px;overflow:hidden;'>"
				+ "  <div style='background:#d6a049;padding:20px;text-align:center;'>"
				+ "    <h1 style='margin:0;color:#1a1a1a;font-size:22px;'>¡Intercambio Completado!</h1>" + "  </div>"
				+ "  <div style='padding:24px;'>"
				+ "    <p style='font-size:15px;color:#e8e8e8;margin-bottom:18px;'>Hola, <strong style='color:#d6a049;'>"
				+ escapeHtml(saludo) + "</strong>:</p>"
				+ "    <p style='font-size:14px;color:#cccccc;line-height:1.5;margin-bottom:18px;'>"
				+ "      Tu solicitud de intercambio fue aceptada por <strong style='color:#d6a049;'>"
				+ escapeHtml(accepter) + "</strong>. ¡La transferencia ya se realizó!" + "    </p>"
				+ "    <div style='background:#1a1a1a;border:1px dashed #d6a049;border-radius:8px;padding:16px;margin:18px 0;'>"
				+ "      <p style='margin:0 0 8px 0;font-size:13px;color:#888;'>Entregaste:</p>"
				+ "      <p style='margin:0 0 14px 0;font-size:15px;color:#e8e8e8;font-weight:600;'>"
				+ escapeHtml(entregada) + "</p>"
				+ "      <p style='margin:0 0 8px 0;font-size:13px;color:#888;'>Recibiste:</p>"
				+ "      <p style='margin:0;font-size:15px;color:#6ee887;font-weight:600;'>" + escapeHtml(recibida)
				+ "</p>" + "    </div>" + "    <p style='font-size:13px;color:#999;line-height:1.5;'>"
				+ "      Revisa tu álbum: la nueva lámina ya está pegada en su lugar." + "    </p>" + "  </div>"
				+ "  <div style='background:#1a1a1a;padding:14px;text-align:center;font-size:11px;color:#666;'>"
				+ "    Mundial 2026 Hub — equipo Universidad El Bosque" + "  </div>" + "</div>" + "</body></html>";
	}

	/** Escapa caracteres HTML básicos para evitar inyección en plantillas. */
	private String escapeHtml(String s) {
		if (s == null)
			return "";
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'",
				"&#39;");
	}

	/**
	 * Construye la plantilla HTML compartida por todos los correos transaccionales.
	 * <p>
	 * La plantilla está diseñada para verse bien en clientes de correo como Gmail,
	 * Outlook, Apple Mail y Yahoo. Usa estilos inline (los clientes web suelen
	 * filtrar {@code <style>} en {@code <head>}) y layout basado en tablas con
	 * anchos absolutos para máxima compatibilidad. Es responsiva hasta cierto
	 * punto: en móviles se ajusta al ancho de la pantalla manteniendo el padding
	 * interno.
	 * </p>
	 *
	 * @param tituloHeader  Título mostrado en el header dorado.
	 * @param emoji         Emoji que acompaña al título (ej. ⚽, 🔐).
	 * @param saludo        Saludo personalizado al usuario (HTML permitido para
	 *                      &lt;strong&gt;).
	 * @param introduccion  Párrafo de introducción que explica por qué llega el
	 *                      correo.
	 * @param codigo        Código de 6 dígitos a mostrar destacado.
	 * @param instrucciones Párrafo con los pasos a seguir.
	 * @param avisos        Párrafo de avisos de seguridad e ignorar si no fue
	 *                      solicitado.
	 * @return Cadena HTML completa lista para enviar como cuerpo del correo.
	 */
	private String construirPlantillaBase(String tituloHeader, String emoji, String saludo, String introduccion,
			String codigo, String instrucciones, String avisos) {
		return "" + "<!DOCTYPE html>" + "<html lang=\"es\">" + "<head>" + "<meta charset=\"UTF-8\">"
				+ "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
				+ "<title>Mundial 2026 Hub</title>" + "</head>"
				+ "<body style=\"margin:0; padding:0; background-color:#0a0a0a;"
				+ " font-family:'Segoe UI', Arial, sans-serif; color:" + COLOR_TEXTO + ";\">"

				+ "<table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\""
				+ " align=\"center\" width=\"100%\" style=\"background-color:#0a0a0a; padding:30px 10px;\">"
				+ "<tr><td align=\"center\">"

				// Tarjeta principal
				+ "<table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\""
				+ " width=\"600\" style=\"max-width:600px; width:100%;" + " background-color:" + COLOR_FONDO_TARJETA
				+ ";" + " border:2px solid " + COLOR_DORADO + ";" + " border-radius:12px; overflow:hidden;"
				+ " box-shadow:0 4px 20px rgba(214,160,73,0.15);\">"

				// ─── Header dorado con gradiente ─────────────────────────────────────
				+ "<tr>" + "<td style=\"background:linear-gradient(135deg," + COLOR_DORADO + " 0%," + COLOR_DORADO_CLARO
				+ " 100%);" + " padding:28px 30px; text-align:center;\">"
				+ "<h1 style=\"margin:0; font-size:26px; font-weight:700; color:#1a1a1a;" + " letter-spacing:0.5px;\">"
				+ emoji + " &nbsp; Mundial 2026 Hub &nbsp; " + emoji + "</h1>"
				+ "<p style=\"margin:8px 0 0 0; font-size:14px; color:#3a2a10;"
				+ " font-weight:600; letter-spacing:1px; text-transform:uppercase;\">" + tituloHeader + "</p>" + "</td>"
				+ "</tr>"

				// ─── Cuerpo del mensaje ──────────────────────────────────────────────
				+ "<tr>" + "<td style=\"padding:36px 36px 20px 36px;\">"

				// Saludo
				+ "<p style=\"margin:0 0 20px 0; font-size:16px; line-height:1.5;" + " color:" + COLOR_TEXTO + ";\">"
				+ saludo + "</p>"

				// Introducción
				+ "<p style=\"margin:0 0 28px 0; font-size:15px; line-height:1.6;" + " color:" + COLOR_TEXTO + ";\">"
				+ introduccion + "</p>"

				// ─── Caja del código (estilo Advance Wars) ───────────────────────
				+ "<table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\""
				+ " width=\"100%\" style=\"margin:0 auto 28px auto;\">" + "<tr>" + "<td align=\"center\""
				+ " style=\"background-color:" + COLOR_FONDO_CODIGO + ";" + " border:2px dashed " + COLOR_DORADO + ";"
				+ " border-radius:10px; padding:24px 20px;\">" + "<p style=\"margin:0 0 8px 0; font-size:11px; color:"
				+ COLOR_TEXTO_SECUNDARIO + ";" + " text-transform:uppercase; letter-spacing:2px; font-weight:600;\">"
				+ "Tu código" + "</p>" + "<p style=\"margin:0; font-size:38px; font-weight:700;" + " color:"
				+ COLOR_DORADO + ";" + " letter-spacing:10px; font-family:'Courier New', monospace;\">"
				+ escapar(codigo) + "</p>" + "</td>" + "</tr>" + "</table>"

				// Mensaje de confidencialidad (estilo Advance Wars)
				+ "<p style=\"margin:0 0 24px 0; font-size:13px; line-height:1.5;" + " color:" + COLOR_DORADO
				+ "; font-style:italic; text-align:center;\">"
				+ "Este código es confidencial. No lo compartas con nadie." + "</p>"

				// Instrucciones
				+ "<p style=\"margin:0 0 20px 0; font-size:14px; line-height:1.6;" + " color:" + COLOR_TEXTO + ";\">"
				+ instrucciones + "</p>"

				// Avisos de seguridad
				+ "<p style=\"margin:0 0 20px 0; font-size:13px; line-height:1.6;" + " color:" + COLOR_TEXTO_SECUNDARIO
				+ ";\">" + avisos + "</p>"

				// Cierre
				+ "<p style=\"margin:30px 0 0 0; font-size:14px; line-height:1.5;" + " color:" + COLOR_TEXTO + ";\">"
				+ "Un saludo,<br>" + "<strong style=\"color:" + COLOR_DORADO
				+ ";\">El equipo de Mundial 2026 Hub</strong>" + "</p>"

				+ "</td>" + "</tr>"

				// ─── Footer ──────────────────────────────────────────────────────────
				+ "<tr>" + "<td style=\"background-color:#0f0f0f; border-top:1px solid #2a2a2a;"
				+ " padding:18px 30px; text-align:center;\">" + "<p style=\"margin:0 0 6px 0; font-size:12px;"
				+ " color:" + COLOR_DORADO + "; font-weight:600;\">" + "🏆 ¡Que viva el Mundial 2026! 🏆" + "</p>"
				+ "<p style=\"margin:0; font-size:11px; color:#555555; line-height:1.5;\">"
				+ "Mensaje automático. Nunca compartas tu código con nadie,<br>"
				+ "ni siquiera con personal de Mundial 2026 Hub: nunca te lo pediremos." + "</p>" + "</td>" + "</tr>"

				+ "</table>"

				+ "</td></tr>" + "</table>"

				+ "</body>" + "</html>";
	}

	// =========================================================================
	// Envío y utilidades
	// =========================================================================

	/**
	 * Envía un correo HTML.
	 * <p>
	 * Loggea éxito o fallo, pero NO lanza excepción al caller si el envío falla: el
	 * flujo de registro/recuperación no debe quebrarse porque SMTP esté caído. El
	 * usuario puede solicitar reenvío.
	 * </p>
	 *
	 * @param destinatario Dirección del receptor.
	 * @param asunto       Asunto del correo.
	 * @param htmlBody     Cuerpo del correo en HTML.
	 */
	private void enviarHtml(String destinatario, String asunto, String htmlBody) {
		try {
			MimeMessage mensaje = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
			helper.setFrom(remitente, "Mundial 2026 Hub");
			helper.setTo(destinatario);
			helper.setSubject(asunto);
			helper.setText(htmlBody, true);
			mailSender.send(mensaje);
			log.info("Correo HTML enviado correctamente a {}", destinatario);
		} catch (MessagingException | java.io.UnsupportedEncodingException e) {
			log.error("Error al enviar correo a {}: {}", destinatario, e.getMessage());
		} catch (Exception e) {
			log.error("Error inesperado al enviar correo a {}: {}", destinatario, e.getMessage());
		}
	}

	/**
	 * Escapa caracteres HTML peligrosos en una cadena para evitar inyección
	 * accidental en la plantilla.
	 *
	 * @param texto Texto a escapar.
	 * @return Texto con los caracteres HTML especiales reemplazados por entidades.
	 */
	private String escapar(String texto) {
		if (texto == null)
			return "";
		return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	/**
	 * Genera un código numérico aleatorio de 6 dígitos como cadena.
	 *
	 * @return Cadena de 6 dígitos entre "100000" y "999999".
	 */
	public static String generarCodigo6Digitos() {
		int codigo = 100000 + (int) (Math.random() * 900000);
		return String.valueOf(codigo);
	}
}