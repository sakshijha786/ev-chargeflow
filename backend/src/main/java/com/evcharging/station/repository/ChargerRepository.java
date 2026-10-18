package com.evcharging.station.repository;

import com.evcharging.station.entity.Charger;
import com.evcharging.station.enums.ChargerStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface ChargerRepository extends JpaRepository<Charger, Long> {

    List<Charger> findByStatus(ChargerStatus status);

    Optional<Charger> findByCode(String code);

    /**
     * Pessimistic write lock: when two booking requests race for the same
     * "first available charger" at the same instant, this prevents both
     * from grabbing it.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Charger> findFirstByStatus(ChargerStatus status);
}
