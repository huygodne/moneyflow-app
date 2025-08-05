package com.moneyflow.moneyflow.util;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Component
public class NumberFormatter {
    private final DecimalFormat decimalFormat;

    public NumberFormatter() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        decimalFormat = new DecimalFormat("#,##0", symbols); // Không hiển thị phần thập phân
    }

    public String formatBigDecimal(BigDecimal value) {
        return value != null ? decimalFormat.format(value) : "0";
    }
}