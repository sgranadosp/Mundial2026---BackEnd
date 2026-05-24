/**
 * Paquete que contiene los controladores REST de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.mundial2026.dto.DashboardSummaryDTO;
import co.edu.unbosque.mundial2026.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST del Dashboard administrativo (HU16/HU20).
 * <p>
 * Todos los endpoints son exclusivos para el rol {@code ADMIN}. El
 * frontend hace polling de {@code /summary} cada 15 segundos para
 * mantener el panel actualizado.
 * </p>
 */
@RestController
@RequestMapping("/admin/dashboard")
@Tag(name = "Admin · Dashboard", description = "Solo ADMIN. Resumen general del sistema en tiempo real")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    /**
     * Servicio que computa las métricas del dashboard.
     */
    @Autowired
    private DashboardService dashboardService;

    /**
     * Constructor por defecto requerido por Spring.
     */
    public DashboardController() {
    }

    /**
     * Devuelve el resumen agregado del dashboard administrativo. Incluye
     * conteo total y delta semanal de usuarios, total de partidos y
     * partidos en vivo, total de grupos de polla y pronósticos, y total
     * de entradas activas con la cantidad que expira en los próximos 15
     * minutos. Todos los valores se computan en tiempo real desde la BD.
     *
     * @return 200 OK con el {@link DashboardSummaryDTO}.
     */
    @GetMapping("/summary")
    @Operation(summary = "Resumen del dashboard",
               description = "Retorna las métricas agregadas del panel administrativo en tiempo real.")
    public ResponseEntity<DashboardSummaryDTO> getSummary() {
        return ResponseEntity.ok(dashboardService.buildSummary());
    }
}
