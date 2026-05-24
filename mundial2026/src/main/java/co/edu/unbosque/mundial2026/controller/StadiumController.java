/**
 * Paquete que contiene los controladores REST de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.mundial2026.dto.StadiumDTO;
import co.edu.unbosque.mundial2026.service.StadiumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST de estadios y ciudades sede del Mundial 2026.
 * <p>
 * Expone endpoints de solo lectura sobre los estadios sincronizados desde la
 * API externa de fútbol. Sirve principalmente al componente de perfil del
 * usuario para llenar el desplegable de "ciudad preferida" (HU05) y para
 * cualquier otra pantalla que necesite listar sedes.
 * </p>
 * <p>
 * Ambos endpoints son accesibles por roles {@code USER} y {@code ADMIN}.
 * </p>
 */
@RestController
@RequestMapping("/stadiums")
@Tag(name = "Estadios", description = "Listado de estadios y ciudades sede del Mundial 2026")
@SecurityRequirement(name = "bearerAuth")
public class StadiumController {

    /**
     * Servicio de lógica de negocio de estadios.
     */
    @Autowired
    private StadiumService stadiumService;

    /**
     * Constructor por defecto requerido por Spring.
     */
    public StadiumController() {
    }

    /**
     * Lista todos los estadios sede del Mundial 2026, ordenados por ciudad.
     *
     * @return 202 Accepted con la lista de estadios; 204 No Content si la
     *         tabla está vacía.
     */
    @GetMapping
    @Operation(summary = "Listar estadios",
               description = "Retorna todos los estadios sede del Mundial 2026, ordenados por ciudad.")
    public ResponseEntity<List<StadiumDTO>> getAll() {
        List<StadiumDTO> stadiums = stadiumService.getAll();
        if (stadiums.isEmpty()) {
            return new ResponseEntity<>(stadiums, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(stadiums, HttpStatus.ACCEPTED);
    }

    /**
     * Lista las ciudades sede del Mundial 2026 (16 únicas a partir de los
     * estadios sincronizados). Ideal para llenar un desplegable de "ciudad
     * preferida" sin necesidad de transportar todos los campos del estadio.
     *
     * @return 202 Accepted con la lista de ciudades en orden alfabético;
     *         204 No Content si no hay estadios registrados.
     */
    @GetMapping("/cities")
    @Operation(summary = "Listar ciudades sede",
               description = "Retorna las ciudades únicas donde se ubican los estadios sede del Mundial 2026.")
    public ResponseEntity<List<String>> getCities() {
        List<String> cities = stadiumService.getDistinctCities();
        if (cities.isEmpty()) {
            return new ResponseEntity<>(cities, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(cities, HttpStatus.ACCEPTED);
    }
}
