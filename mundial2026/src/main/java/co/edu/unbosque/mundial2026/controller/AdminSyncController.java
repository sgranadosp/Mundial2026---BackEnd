/**
 * Paquete que contiene los controladores REST de la aplicación Mundial 2026 Hub.
 */
package co.edu.unbosque.mundial2026.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.mundial2026.service.FootballApiSyncService;
import co.edu.unbosque.mundial2026.service.FootballApiSyncService.SyncReport;
import co.edu.unbosque.mundial2026.service.OpenFootballSyncService;
import co.edu.unbosque.mundial2026.service.OpenFootballSyncService.VenueSyncReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST para acciones administrativas de sincronización con
 * APIs externas.
 * <p>
 * Todos los endpoints aquí están restringidos a usuarios con rol ADMIN
 * mediante {@code @PreAuthorize("hasRole('ADMIN')")}.
 * </p>
 */
@RestController
@RequestMapping("/admin/sync")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin · Sync", description = "Endpoints administrativos de sincronización con APIs externas")
public class AdminSyncController {

    private static final Logger log = LoggerFactory.getLogger(AdminSyncController.class);

    @Autowired
    private FootballApiSyncService syncService;

    @Autowired
    private OpenFootballSyncService venuesSyncService;

    /** Constructor por defecto requerido por Spring. */
    public AdminSyncController() {
    }

    /**
     * Sincroniza los partidos de fase de grupos del Mundial 2026 desde
     * football-data.org.
     * <p>
     * Esta operación consume cuota del plan gratuito (10 requests/minuto en
     * football-data.org). Se recomienda ejecutarla puntualmente, no en bucle.
     * </p>
     *
     * @return Reporte JSON con métricas de la sincronización.
     */
    @PostMapping("/fixtures")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sincroniza fixtures de fase de grupos del Mundial 2026",
               description = "Trae los partidos desde football-data.org y los upsertea en la BD local. Solo admin.")
    public ResponseEntity<Map<String, Object>> syncFixtures() {
        log.info("Admin solicitó sincronización de fixtures");
        SyncReport report = syncService.syncGroupStageFixtures();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Sincronización completada");
        response.put("report", reportToMap(report));

        return ResponseEntity.ok(response);
    }

    /**
     * Convierte el SyncReport a un Map para serialización JSON limpia.
     */
    private Map<String, Object> reportToMap(SyncReport r) {
        Map<String, Object> map = new HashMap<>();
        map.put("fetchedFromApi", r.fetchedFromApi);
        map.put("synced", r.synced);
        map.put("skippedNoTeams", r.skippedNoTeams);
        map.put("errors", r.errors);
        map.put("totalGroupStageMatchesInDb", r.totalInDb);
        return map;
    }

    /**
     * Sincroniza los estadios (venues) y los asigna a los partidos ya
     * existentes en BD, usando como fuente
     * <a href="https://github.com/openfootball/worldcup.json">openfootball/worldcup.json</a>.
     * <p>
     * Diseñado para correr DESPUÉS de {@link #syncFixtures()}: complementa los
     * partidos importados desde football-data.org (que vienen sin venue)
     * asignándoles el estadio correcto según la sede oficial publicada por
     * openfootball.
     * </p>
     * <p>
     * No requiere API key (el dataset es open public domain). La operación
     * es idempotente y se puede ejecutar las veces que sea necesario.
     * </p>
     *
     * @return Reporte JSON con métricas del sync de venues.
     */
    @PostMapping("/venues")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sincroniza estadios y asigna venues a partidos",
               description = "Trae los 16 estadios del Mundial 2026 y asigna cada partido en BD"
                       + " a su sede oficial, usando el dataset open public domain"
                       + " openfootball/worldcup.json. Solo admin.")
    public ResponseEntity<Map<String, Object>> syncVenues() {
        log.info("Admin solicitó sincronización de venues");
        VenueSyncReport report = venuesSyncService.syncVenues();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Sincronización de venues completada");
        response.put("report", venuesReportToMap(report));

        return ResponseEntity.ok(response);
    }

    /**
     * Convierte el VenueSyncReport a un Map para serialización JSON limpia.
     */
    private Map<String, Object> venuesReportToMap(VenueSyncReport r) {
        Map<String, Object> map = new HashMap<>();
        map.put("stadiumsCreated", r.stadiumsCreated);
        map.put("stadiumsUpdated", r.stadiumsUpdated);
        map.put("fetchedFromOpenFootball", r.fetchedFromOpenFootball);
        map.put("matchesAssigned", r.matchesAssigned);
        map.put("matchesAlreadyAssigned", r.matchesAlreadyAssigned);
        map.put("matchesNotInDb", r.matchesNotInDb);
        map.put("unmappedGrounds", r.unmappedGrounds);
        return map;
    }
}