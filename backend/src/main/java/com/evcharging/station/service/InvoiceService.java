package com.evcharging.station.service;

import com.evcharging.station.entity.Booking;
import com.evcharging.station.entity.Invoice;
import com.evcharging.station.exception.ResourceNotFoundException;
import com.evcharging.station.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    private static final double BASE_TARIFF_INR = 18.00;
    private static final double SESSION_FEE_INR = 25.00;
    private static final double GST_RATE = 0.18;

    /**
     * Rule-Based Billing:
     * Subtotal = (kWh * 18) + (kWh > 0 ? 25 : 0)
     * CGST = 9% on Subtotal
     * SGST = 9% on Subtotal
     * Total = Subtotal + 18% GST
     */
    @Transactional
    public Invoice generateInvoice(Booking booking) {
        double kwh = booking.getEnergyDeliveredKwh() != null ? booking.getEnergyDeliveredKwh() :
                     (booking.getEnergyTargetKwh() != null ? booking.getEnergyTargetKwh() : 0.0);

        double energyCost = kwh * BASE_TARIFF_INR;
        double sessionFee = (kwh > 0) ? SESSION_FEE_INR : 0.0;
        double subtotal = energyCost + sessionFee;
        double cgst = subtotal * (GST_RATE / 2.0);
        double sgst = subtotal * (GST_RATE / 2.0);
        double totalGst = cgst + sgst;
        double grandTotal = subtotal + totalGst;

        String name = booking.getCustomerName();
        String email = null;
        Long uid = null;
        if (booking.getUser() != null) {
            uid = booking.getUser().getId();
            email = booking.getUser().getEmail();
            if (name == null || name.isBlank()) {
                name = booking.getUser().getName();
            }
        }

        Invoice invoice = Invoice.builder()
                .bookingId(booking.getId())
                .userId(uid)
                .customerName(name != null ? name : "Walk-in Guest")
                .customerEmail(email != null ? email : "walkin@chargeflow.in")
                .chargerCode(booking.getCharger().getCode())
                .energyKwh(Math.round(kwh * 100.0) / 100.0)
                .ratePerKwh(BASE_TARIFF_INR)
                .sessionFee(sessionFee)
                .energyCost(Math.round(energyCost * 100.0) / 100.0)
                .subtotal(Math.round(subtotal * 100.0) / 100.0)
                .cgst(Math.round(cgst * 100.0) / 100.0)
                .sgst(Math.round(sgst * 100.0) / 100.0)
                .totalGst(Math.round(totalGst * 100.0) / 100.0)
                .grandTotal(Math.round(grandTotal * 100.0) / 100.0)
                .status("PAID")
                .build();

        invoice = invoiceRepository.save(invoice);
        log.info("Generated Invoice #{} for Booking #{}: ₹{} (Energy: {} kWh)",
                invoice.getId(), booking.getId(), invoice.getGrandTotal(), invoice.getEnergyKwh());
        return invoice;
    }

    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
    }

    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAllByOrderByInvoiceDateDesc();
    }

    public List<Invoice> getInvoicesByUser(Long userId) {
        return invoiceRepository.findByUserIdOrderByInvoiceDateDesc(userId);
    }

    public Double getTotalRevenue() {
        Double rev = invoiceRepository.getTotalRevenue();
        return rev != null ? rev : 0.0;
    }

    public Double getTotalEnergyDelivered() {
        Double en = invoiceRepository.getTotalEnergyDelivered();
        return en != null ? en : 0.0;
    }

    public Double getTotalGst() {
        Double gst = invoiceRepository.getTotalGstCollected();
        return gst != null ? gst : 0.0;
    }
}
