package com.evcharging.station.controller;

import com.evcharging.station.dto.ApiResponse;
import com.evcharging.station.entity.Invoice;
import com.evcharging.station.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Invoice>>> getAllInvoices() {
        return ResponseEntity.ok(ApiResponse.ok("Invoices fetched", invoiceService.getAllInvoices()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Invoice>> getInvoiceById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Invoice fetched", invoiceService.getInvoiceById(id)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<Invoice>>> getInvoicesByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("User invoices fetched", invoiceService.getInvoicesByUser(userId)));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBillingSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalRevenueInr", invoiceService.getTotalRevenue());
        summary.put("totalEnergyDeliveredKwh", invoiceService.getTotalEnergyDelivered());
        summary.put("totalGstInr", invoiceService.getTotalGst());
        summary.put("tariffFormula", "Total = (Energy kWh * ₹18.00 + ₹25.00 Session Fee) + 18% GST");
        return ResponseEntity.ok(ApiResponse.ok("Financial billing summary fetched", summary));
    }
}
