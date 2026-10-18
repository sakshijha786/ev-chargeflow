package com.evcharging.station.controller;

import com.evcharging.station.dto.ApiResponse;
import com.evcharging.station.dto.ChargerRequest;
import com.evcharging.station.entity.Charger;
import com.evcharging.station.enums.ChargerStatus;
import com.evcharging.station.service.ChargerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chargers")
@RequiredArgsConstructor
public class ChargerController {

    private final ChargerService chargerService;

    @PostMapping
    public ResponseEntity<ApiResponse<Charger>> createCharger(@Valid @RequestBody ChargerRequest request) {
        Charger charger = chargerService.createCharger(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Charger created", charger));
    }

    /** Real-time status of every charger at the station. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Charger>>> getAllChargers() {
        return ResponseEntity.ok(ApiResponse.ok("Chargers fetched", chargerService.getAllChargers()));
    }

    /** Real-time list of chargers free to book right now. */
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<Charger>>> getAvailableChargers() {
        return ResponseEntity.ok(ApiResponse.ok("Available chargers fetched", chargerService.getAvailableChargers()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Charger>> getCharger(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Charger fetched", chargerService.getChargerById(id)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Charger>> updateStatus(@PathVariable Long id,
                                                               @RequestParam ChargerStatus status) {
        return ResponseEntity.ok(ApiResponse.ok("Charger status updated", chargerService.updateStatus(id, status)));
    }
}
