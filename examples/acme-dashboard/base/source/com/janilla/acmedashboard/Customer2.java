package com.janilla.acmedashboard;

import java.math.BigDecimal;

import com.janilla.java.Flat;

public record Customer2(@Flat Customer customer, Long invoiceCount, BigDecimal pendingAmount, BigDecimal paidAmount) {
}