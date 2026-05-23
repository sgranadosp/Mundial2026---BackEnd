/**
 * Paquete que contiene las clases de controlador REST de la aplicación.
 */
package co.edu.unbosque.mundial2026.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.mundial2026.dto.CreatePreferenceRequest;
import co.edu.unbosque.mundial2026.dto.CreatePreferenceResponse;
import co.edu.unbosque.mundial2026.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Endpoints REST de la integración con MercadoPago.
 * <p>
 * Provee dos endpoints:
 * <ul>
 *   <li>{@code POST /payments/create-preference}: el frontend lo llama justo
 *       antes de abrir el modal de pago. Crea la preference en MercadoPago,
 *       guarda los tickets en estado RESERVED y devuelve el preferenceId que
 *       el Wallet Brick necesita.</li>
 *   <li>{@code POST /payments/webhook}: MercadoPago lo llama automáticamente
 *       cuando un pago cambia de estado. Marca los tickets como PAID si fue
 *       aprobado.</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/payments")
@Tag(name = "Payments", description = "Integración con MercadoPago Checkout Pro")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;

    /** Constructor por defecto. */
    public PaymentController() {
    }

    /**
     * Crea una preference de MercadoPago para iniciar el flujo de pago.
     * Devuelve el preferenceId que el frontend usa con el Wallet Brick.
     *
     * @param userId  ID del usuario comprador (por query param, igual que
     *                {@code /tickets/reserve}).
     * @param request Cuerpo JSON con {@code matchId} y {@code quantity}.
     * @return 201 Created con {@link CreatePreferenceResponse}; 400 si los
     *         datos son inválidos; 500 si MercadoPago rechaza la creación.
     */
    @PostMapping("/create-preference")
    @Operation(summary = "Crea una preference de pago en MercadoPago",
               description = "Inicia el flujo de pago para una compra de tickets.")
    public ResponseEntity<?> createPreference(@RequestParam Long userId,
                                               @RequestBody CreatePreferenceRequest request) {
        try {
            CreatePreferenceResponse response = paymentService.createPreference(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("[payments] Bad request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage(), "success", false));
        } catch (IllegalStateException e) {
            log.error("[payments] MercadoPago rechazó: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "No se pudo iniciar el pago: " + e.getMessage(),
                            "success", false));
        }
    }

    /**
     * Webhook llamado por MercadoPago cuando un pago cambia de estado.
     * <p>
     * MercadoPago envía dos parámetros: {@code type} (siempre "payment") e
     * {@code id} (id del pago). El cuerpo del request también trae los
     * detalles, pero los parámetros son suficientes para resolver el pago.
     * </p>
     * <p>
     * Siempre devolvemos 200 OK para que MercadoPago no haga reintentos.
     * Los errores internos se loguean pero no se propagan al webhook.
     * </p>
     *
     * @param type Tipo de notificación.
     * @param id   ID del pago.
     * @param body Cuerpo crudo del request (útil para debugging y para
     *             integraciones que vienen sin query params).
     * @return 200 OK siempre.
     */
    @PostMapping("/webhook")
    @Operation(summary = "Webhook de MercadoPago",
               description = "Endpoint público llamado por MercadoPago cuando cambia un pago.")
    public ResponseEntity<?> webhook(@RequestParam(required = false) String type,
                                      @RequestParam(name = "id", required = false) String id,
                                      @RequestParam(name = "data.id", required = false) String dataId,
                                      @RequestBody(required = false) Map<String, Object> body) {
        log.info("[mercadopago] Webhook recibido type={} id={} dataId={} bodyKeys={}",
                type, id, dataId, body != null ? body.keySet() : "null");

        // MercadoPago envía a veces ?type=payment&id=..., otras veces ?data.id=...,
        // y el body siempre trae lo importante.
        String resolvedType = type;
        String resolvedId = dataId != null ? dataId : id;

        if (resolvedType == null && body != null) {
            Object t = body.get("type");
            if (t != null) resolvedType = t.toString();
        }
        if (resolvedId == null && body != null) {
            Object data = body.get("data");
            if (data instanceof Map<?, ?> dataMap) {
                Object inner = dataMap.get("id");
                if (inner != null) resolvedId = inner.toString();
            }
        }

        try {
            paymentService.handleWebhook(resolvedType, resolvedId);
        } catch (Exception e) {
            log.error("[mercadopago] Error en webhook: {}", e.getMessage(), e);
        }
        // Siempre 200 OK para que MercadoPago no reintente.
        Map<String, Object> ok = new HashMap<>();
        ok.put("received", true);
        return ResponseEntity.ok(ok);
    }
}