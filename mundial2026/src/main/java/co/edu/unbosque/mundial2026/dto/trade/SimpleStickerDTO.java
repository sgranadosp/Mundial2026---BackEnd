package co.edu.unbosque.mundial2026.dto.trade;

/**
 * DTO mínimo de una lámina para alimentar autocompletes del módulo de
 * intercambios. Contiene solo lo necesario para que el usuario reconozca
 * la lámina (código legible, nombre del país, posición y URL de la imagen).
 */
public class SimpleStickerDTO {

    private Long id;
    private String code;
    private String countryCode;
    private String countryName;
    private int position;
    private String imageUrl;

    /** Cantidad que posee el usuario consultante (solo para repetidas, donde es ≥ 2). */
    private int quantity;

    public SimpleStickerDTO() {}

    public SimpleStickerDTO(Long id, String code, String countryCode, String countryName,
                            int position, String imageUrl, int quantity) {
        this.id = id;
        this.code = code;
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.position = position;
        this.imageUrl = imageUrl;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getCountryName() { return countryName; }
    public void setCountryName(String countryName) { this.countryName = countryName; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}