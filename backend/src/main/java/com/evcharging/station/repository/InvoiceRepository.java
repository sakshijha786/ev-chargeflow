package com.evcharging.station.repository;

import com.evcharging.station.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByUserIdOrderByInvoiceDateDesc(Long userId);

    List<Invoice> findAllByOrderByInvoiceDateDesc();

    @Query("SELECT SUM(i.grandTotal) FROM Invoice i WHERE i.status = 'PAID'")
    Double getTotalRevenue();

    @Query("SELECT SUM(i.energyKwh) FROM Invoice i WHERE i.status = 'PAID'")
    Double getTotalEnergyDelivered();

    @Query("SELECT SUM(i.totalGst) FROM Invoice i WHERE i.status = 'PAID'")
    Double getTotalGstCollected();
}
