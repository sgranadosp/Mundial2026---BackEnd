/**
 * Paquete con los DTOs de pagos consumidos por la pasarela MercadoPago.
 */
package co.edu.unbosque.mundial2026.dto;

/**
 * DTO devuelto por {@code POST /payments/create-preference}.
 * <p>
 * Contiene los datos que el frontend necesita para abrir la pasarela:
 * <ul>
 *   <li>{@code preferenceId}: ID de la preference creada en MercadoPago.
 *       Es lo que recibe el Wallet Brick del SDK del frontend.</li>
 *   <li>{@code initPoint}: URL del checkout web (fallback si el Brick no
 *       se puede mostrar, ej. en móviles muy antiguos).</li>
 *   <li>{@code publicKey}: Public Key de MercadoPago. Se envía desde el
 *       backend para que el frontend no tenga que hardcodearla.</li>
 *   <li>{@code totalAmount}: total cobrado, para mostrarlo en la UI.</li>
 * </ul>
 * </p>
 */
public class CreatePreferenceResponse {

    private String preferenceId;
    private String initPoint;
    private String publicKey;
    private Double totalAmount;
    private String currency;

    /** Constructor por defecto. */
    public CreatePreferenceResponse() { }

    public String getPreferenceId() { return preferenceId; }
    public void setPreferenceId(String preferenceId) { this.preferenceId = preferenceId; }

    public String getInitPoint() { return initPoint; }
    public void setInitPoint(String initPoint) { this.initPoint = initPoint; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}