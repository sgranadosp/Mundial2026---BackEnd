/**
 * Paquete que contiene las clases de configuración de la aplicación
 * Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import co.edu.unbosque.mundial2026.model.Sticker;
import co.edu.unbosque.mundial2026.model.Sticker.StickerType;
import co.edu.unbosque.mundial2026.repository.StickerRepository;

/**
 * Loader que precarga el catálogo maestro de 294 láminas del álbum del
 * Mundial 2026 Hub la primera vez que arranca la aplicación.
 * <p>
 * Es idempotente: si la tabla {@code stickers} ya tiene filas, no hace nada.
 * Esto permite que reinicios sucesivos no dupliquen registros ni alteren
 * el catálogo.
 * </p>
 * <p>
 * <b>Estructura del catálogo:</b>
 * <ul>
 *   <li>48 selecciones × 6 láminas cada una = 288 láminas.</li>
 *   <li>1 página de 6 láminas especiales (común a todos).</li>
 *   <li>Total = 294 láminas únicas.</li>
 * </ul>
 * Por convención, dentro de cada selección la posición 1 es el escudo y
 * las posiciones 2 a 6 son jugadores. La página "Especial" usa el tipo
 * {@code SPECIAL} para sus 6 láminas.
 * </p>
 * <p>
 * Los códigos de cada lámina siguen el formato {@code <country_code>_NN}
 * (ej. {@code argentina_01}, {@code especial_06}), que coincide con el
 * nombre del archivo PNG servido por el endpoint {@code /api/laminas/}.
 * </p>
 */
@Configuration
public class StickerCatalogLoader {

    /**
     * Logger para registrar el progreso del precargado.
     */
    private static final Logger log = LoggerFactory.getLogger(StickerCatalogLoader.class);

    /**
     * Bean {@link CommandLineRunner} que ejecuta el precargado del catálogo.
     * Tiene {@code Order(2)} para garantizar que corra DESPUÉS del loader
     * de usuarios semilla (que tiene el orden por defecto). 
     *
     * @param stickerRepo Repositorio del catálogo de láminas.
     * @return El {@link CommandLineRunner} con la lógica de carga.
     */
    @Bean
    @Order(2)
    CommandLineRunner loadStickerCatalog(StickerRepository stickerRepo) {
        return args -> {
            if (stickerRepo.count() > 0) {
                log.info("Catálogo de láminas ya cargado ({} láminas) — omitiendo precarga",
                        stickerRepo.count());
                return;
            }
            log.info("=== Precargando catálogo de láminas (294 entradas) ===");

            List<Sticker> all = new ArrayList<>(294);
            for (Map.Entry<String, String> entry : buildCountryMap().entrySet()) {
                String code = entry.getKey();
                String name = entry.getValue();
                boolean esEspecial = "especial".equals(code);
                for (int pos = 1; pos <= 6; pos++) {
                    String stickerCode = code + "_" + String.format("%02d", pos);
                    StickerType type;
                    if (esEspecial) {
                        type = StickerType.SPECIAL;
                    } else if (pos == 1) {
                        type = StickerType.CREST;
                    } else {
                        type = StickerType.PLAYER;
                    }
                    all.add(new Sticker(stickerCode, code, name, pos, type));
                }
            }

            stickerRepo.saveAll(all);
            log.info("=== Catálogo cargado: {} láminas ({} selecciones + especiales) ===",
                    all.size(), buildCountryMap().size() - 1);
        };
    }

    /**
     * Construye el mapeo (código ASCII → nombre legible) de todas las
     * selecciones del Mundial 2026 más la página de Especiales (al final).
     * <p>
     * Usa un {@link LinkedHashMap} para preservar el orden de inserción:
     * alfabético por nombre, con "Especiales" siempre como última entrada.
     * </p>
     *
     * @return Mapa ordenado de código → nombre.
     */
    private Map<String, String> buildCountryMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("alemania",        "Alemania");
        map.put("arabia_saudi",    "Arabia Saudí");
        map.put("argelia",         "Argelia");
        map.put("argentina",       "Argentina");
        map.put("australia",       "Australia");
        map.put("austria",         "Austria");
        map.put("belgica",         "Bélgica");
        map.put("bosnia",          "Bosnia y Herzegovina");
        map.put("brasil",          "Brasil");
        map.put("cabo_verde",      "Cabo Verde");
        map.put("canada",          "Canadá");
        map.put("catar",           "Catar");
        map.put("colombia",        "Colombia");
        map.put("congo",           "Congo");
        map.put("corea",           "Corea del Sur");
        map.put("costa_marfil",    "Costa de Marfil");
        map.put("croacia",         "Croacia");
        map.put("curazao",         "Curazao");
        map.put("ecuador",         "Ecuador");
        map.put("egipto",          "Egipto");
        map.put("escocia",         "Escocia");
        map.put("espana",          "España");
        map.put("estados_unidos",  "Estados Unidos");
        map.put("francia",         "Francia");
        map.put("ghana",           "Ghana");
        map.put("haiti",           "Haití");
        map.put("inglaterra",      "Inglaterra");
        map.put("iran",            "Irán");
        map.put("iraq",            "Iraq");
        map.put("japon",           "Japón");
        map.put("jordania",        "Jordania");
        map.put("marruecos",       "Marruecos");
        map.put("mexico",          "México");
        map.put("noruega",         "Noruega");
        map.put("nueva_zelanda",   "Nueva Zelanda");
        map.put("paises_bajos",    "Países Bajos");
        map.put("panama",          "Panamá");
        map.put("paraguay",        "Paraguay");
        map.put("portugal",        "Portugal");
        map.put("rep_checa",       "República Checa");
        map.put("senegal",         "Senegal");
        map.put("sudafrica",       "Sudáfrica");
        map.put("suecia",          "Suecia");
        map.put("suiza",           "Suiza");
        map.put("tunez",           "Túnez");
        map.put("turquia",         "Turquía");
        map.put("uruguay",         "Uruguay");
        map.put("uzbekistan",      "Uzbekistán");
        // Página final de láminas especiales
        map.put("especial",        "Especiales");
        return map;
    }
}