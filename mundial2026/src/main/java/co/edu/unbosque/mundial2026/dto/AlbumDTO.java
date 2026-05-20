/**
 * Paquete que contiene las clases de Transferencia de Datos (DTOs) utilizadas
 * en la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Clase de Transferencia de Datos (DTO) para representar el álbum digital
 * de un usuario en la plataforma Mundial 2026 Hub.
 * Expone el resumen del álbum (contadores, porcentaje de completitud) y
 * opcionalmente la lista de láminas. En la vista de perfil o dashboard
 * solo se devuelven los contadores; la lista completa de láminas se carga
 * bajo demanda desde el endpoint {@code GET /album/{userId}/stickers}.
 */
public class AlbumDTO {

    /**
     * Identificador único del álbum.
     */
    private Long id;

    /**
     * ID del usuario propietario del álbum.
     */
    private Long userId;

    /**
     * Nombre de usuario del propietario (para vistas públicas).
     */
    private String username;

    /**
     * Número de láminas únicas obtenidas por el usuario.
     */
    private Integer uniqueStickersCount;

    /**
     * Número de láminas repetidas disponibles para intercambio.
     */
    private Integer duplicateStickersCount;

    /**
     * Porcentaje de completitud del álbum (0.0 a 100.0).
     */
    private Double completionPercentage;

    /**
     * Lista de láminas del álbum. Se popula solo cuando se solicita
     * el detalle completo del álbum, no en vistas de resumen.
     */
    private List<StickerDTO> stickers = new ArrayList<>();

    /**
     * Constructor por defecto de {@code AlbumDTO}.
     */
    public AlbumDTO() {
    }

    /**
     * Constructor con los campos de resumen del álbum.
     *
     * @param userId               ID del usuario propietario.
     * @param uniqueStickersCount  Número de láminas únicas.
     * @param duplicateStickersCount Número de láminas repetidas.
     * @param completionPercentage Porcentaje de completitud.
     */
    public AlbumDTO(Long userId, Integer uniqueStickersCount,
                    Integer duplicateStickersCount, Double completionPercentage) {
        this.userId = userId;
        this.uniqueStickersCount = uniqueStickersCount;
        this.duplicateStickersCount = duplicateStickersCount;
        this.completionPercentage = completionPercentage;
    }

    // =========================================================================
    // Getters y Setters
    // =========================================================================

    /** @return El ID del álbum. */
    public Long getId() { return id; }

    /** @param id El nuevo ID. */
    public void setId(Long id) { this.id = id; }

    /** @return El ID del usuario propietario. */
    public Long getUserId() { return userId; }

    /** @param userId El nuevo ID de usuario. */
    public void setUserId(Long userId) { this.userId = userId; }

    /** @return El nombre de usuario del propietario. */
    public String getUsername() { return username; }

    /** @param username El nuevo nombre de usuario. */
    public void setUsername(String username) { this.username = username; }

    /** @return El número de láminas únicas. */
    public Integer getUniqueStickersCount() { return uniqueStickersCount; }

    /** @param uniqueStickersCount El nuevo conteo de láminas únicas. */
    public void setUniqueStickersCount(Integer uniqueStickersCount) {
        this.uniqueStickersCount = uniqueStickersCount;
    }

    /** @return El número de láminas repetidas. */
    public Integer getDuplicateStickersCount() { return duplicateStickersCount; }

    /** @param duplicateStickersCount El nuevo conteo de repetidas. */
    public void setDuplicateStickersCount(Integer duplicateStickersCount) {
        this.duplicateStickersCount = duplicateStickersCount;
    }

    /** @return El porcentaje de completitud (0.0 a 100.0). */
    public Double getCompletionPercentage() { return completionPercentage; }

    /** @param completionPercentage El nuevo porcentaje. */
    public void setCompletionPercentage(Double completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    /** @return La lista de láminas del álbum. */
    public List<StickerDTO> getStickers() { return stickers; }

    /** @param stickers La nueva lista de láminas. */
    public void setStickers(List<StickerDTO> stickers) { this.stickers = stickers; }

    // =========================================================================
    // equals, hashCode, toString
    // =========================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AlbumDTO other = (AlbumDTO) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "AlbumDTO [id=" + id + ", userId=" + userId
                + ", uniqueStickersCount=" + uniqueStickersCount
                + ", duplicateStickersCount=" + duplicateStickersCount
                + ", completionPercentage=" + completionPercentage + "]";
    }
}