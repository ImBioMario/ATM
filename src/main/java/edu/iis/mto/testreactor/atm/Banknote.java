package edu.iis.mto.testreactor.atm;

import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum Banknote {

    PL_10(10, "PLN"),
    PL_20(20, "PLN"),
    PL_50(50, "PLN"),
    PL_100(100, "PLN"),
    PL_200(200, "PLN"),
    PL_500(500, "PLN");

    private static final Map<String, List<Banknote>> banknotesForCurrency;
    static {
        banknotesForCurrency = new HashMap<>();
        banknotesForCurrency.put("PLN", List.of(PL_500, PL_200, PL_100, PL_50, PL_20, PL_10));
    }
    private final int denomination;
    private final Currency currency;

    private Banknote(int denomination, String currencyCode) {
        this.denomination = denomination;
        this.currency = Currency.getInstance(currencyCode);
    }

    public int getDenomination() {
        return denomination;
    }

    public Currency getCurrency() {
        return currency;
    }

    public static List<Banknote> getDescFor(Currency currency) {
        return banknotesForCurrency.getOrDefault(currency.getCurrencyCode(), List.of());
    }

}
