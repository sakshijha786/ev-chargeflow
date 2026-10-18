package com.evcharging.station.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Rule-Based Billing & Invoice Record:
 * Energy Cost = kWh * Tariff (₹18.00)
 * Session Fee = ₹25.00
 * Subtotal = Energy Cost + Session Fee
 * Taxes = 18% GST (9% CGST + 9% SGST)
 * Grand Total = Subtotal + 18% GST
 */
@Entity
@Table(name = "invoices", indexes = {
    @Index(name = "idx_invoice_user", columnList = "userId"),
    @Index(name = "idx_invoice_date", columnList = "invoiceDate")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long bookingId;

    private Long userId;

    private String customerName;

    private String customerEmail;

    private String chargerCode;

    private Double energyKwh;

    @Builder.Default
    private Double ratePerKwh = 18.00;

    @Builder.Default
    private Double sessionFee = 25.00;

    private Double energyCost;

    private Double subtotal;

    private Double cgst;

    private Double sgst;

    private Double totalGst;

    private Double grandTotal;

    @Builder.Default
    private String status = "PAID";

    @Column(updatable = false)
    private LocalDateTime invoiceDate;

    @PrePersist
    protected void onCreate() {
        if (this.invoiceDate == null) {
            this.invoiceDate = LocalDateTime.now();
        }
        if (this.ratePerKwh == null) this.ratePerKwh = 18.00;
        if (this.sessionFee == null) this.sessionFee = (this.energyKwh != null && this.energyKwh > 0) ? 25.00 : 0.00;
        if (this.energyCost == null) this.energyCost = (this.energyKwh != null ? this.energyKwh : 0.0) * this.ratePerKwh;
        if (this.subtotal == null) this.subtotal = this.energyCost + this.sessionFee;
        if (this.cgst == null) this.cgst = this.subtotal * 0.09;
        if (this.sgst == null) this.sgst = this.subtotal * 0.09;
        if (this.totalGst == null) this.totalGst = this.cgst + this.sgst;
        if (this.grandTotal == null) this.grandTotal = this.subtotal + this.totalGst;
    }
}
