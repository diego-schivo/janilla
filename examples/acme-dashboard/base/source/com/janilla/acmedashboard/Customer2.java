package com.janilla.acmedashboard;

import java.math.BigDecimal;

import com.janilla.java.Flatten;

public record Customer2(@Flatten Customer customer, Long invoiceCount, BigDecimal pendingAmount, BigDecimal paidAmount) {
}