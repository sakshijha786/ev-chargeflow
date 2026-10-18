package com.evcharging.station.service;

import com.evcharging.station.dto.ChargerRequest;
import com.evcharging.station.entity.Charger;
import com.evcharging.station.enums.ChargerStatus;
import com.evcharging.station.exception.ResourceNotFoundException;
import com.evcharging.station.repository.ChargerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChargerService {

    private final ChargerRepository chargerRepository;

    public Charger createCharger(ChargerRequest request) {
        Charger charger = Charger.builder()
                .code(request.getCode())
                .location(request.getLocation())
                .powerRatingKw(request.getPowerRatingKw())
                .status(ChargerStatus.AVAILABLE)
                .build();
        return chargerRepository.save(charger);
    }

    public Charger getChargerById(Long id) {
        return chargerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Charger not found with id: " + id));
    }

    /** Real-time view of every charger and its current status. */
    public List<Charger> getAllChargers() {
        return chargerRepository.findAll();
    }

    /** Real-time view of just the chargers free to book right now. */
    public List<Charger> getAvailableChargers() {
        return chargerRepository.findByStatus(ChargerStatus.AVAILABLE);
    }

    public Charger updateStatus(Long id, ChargerStatus status) {
        Charger charger = getChargerById(id);
        charger.setStatus(status);
        return chargerRepository.save(charger);
    }
}
