package edu.iis.mto.testreactor.atm;

import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import edu.iis.mto.testreactor.atm.bank.AccountException;
import edu.iis.mto.testreactor.atm.bank.AuthorizationException;
import edu.iis.mto.testreactor.atm.bank.AuthorizationToken;
import edu.iis.mto.testreactor.atm.bank.Bank;

public class ATMachine {

    private final Bank bank;

    private MoneyDeposit deposit;

    public ATMachine(Bank bank, Currency currency) {
        this.bank = requireNonNull(bank);
        this.deposit = MoneyDeposit.create(requireNonNull(currency), List.of());
    }

    public void setDeposit(MoneyDeposit deposit) {
        this.deposit = requireNonNull(deposit);
    }

    public MoneyDeposit getCurrentDeposit() {
        return MoneyDeposit.create(deposit.getCurrency(), deposit.getBanknotes());
    }

    public Withdrawal withdraw(PinCode pin, Card card, Money amount) throws ATMOperationException {
        validateAmount(amount);
        AuthorizationToken token = authorize(pin, card);
        List<BanknotesPack> banknotes = calculateWithdrawal(amount);
        performBankTransaction(token, amount);

        return Withdrawal.create(release(banknotes));
    }

    private List<BanknotesPack> release(List<BanknotesPack> banknotes) {
        for (BanknotesPack banknotesPack : banknotes) {
            deposit.release(banknotesPack);
        }
        return banknotes;
    }

    private void validateAmount(Money amount) throws ATMOperationException {
        if (unavailableCurrency(amount)) {
            throw new ATMOperationException(ErrorCode.WRONG_CURRENCY);
        }
    }

    private AuthorizationToken authorize(PinCode pin, Card card) throws ATMOperationException {
        try {
            return bank.autorize(pin.getPIN(), card.getNumber());
        } catch (AuthorizationException e) {
            throw new ATMOperationException(ErrorCode.AUTHORIZATION);
        }
    }

    private void performBankTransaction(AuthorizationToken token, Money amount) throws ATMOperationException {
        try {
            bank.charge(token, amount);
        } catch (AccountException e) {
            throw new ATMOperationException(ErrorCode.NO_FUNDS_ON_ACCOUNT);
        }

    }

    private List<BanknotesPack> calculateWithdrawal(Money amount) throws ATMOperationException {

        int valueToWithdraw = getValueToWithdraw(amount.getDenomination());
        List<BanknotesPack> result = new ArrayList<>();
        List<Banknote> allBanknotesForCurrency = Banknote.getDescFor(amount.getCurrency());

        for (Banknote banknote : allBanknotesForCurrency) {
            int denomination = banknote.getDenomination();
            int requiredBanknotesCount = valueToWithdraw / denomination;
            valueToWithdraw = valueToWithdraw % banknote.getDenomination();

            if (requiredBanknotesCount > 0) {
                int availableCount = deposit.getAvailableCountOf(banknote);
                if (notEnoughBanknotes(requiredBanknotesCount, availableCount)) {
                    valueToWithdraw += (requiredBanknotesCount - availableCount) * denomination;
                    requiredBanknotesCount = availableCount;
                }
                result.add(BanknotesPack.create(requiredBanknotesCount, banknote));
            }
        }

        if (valueToWithdraw > 0) {
            throw new ATMOperationException(ErrorCode.WRONG_AMOUNT);
        }
        return result;
    }

    private boolean notEnoughBanknotes(int requiredBanknotesCount, int availableCount) {
        return requiredBanknotesCount > availableCount;
    }

    private int getValueToWithdraw(BigDecimal denomination) throws ATMOperationException {
        try {
            return denomination.intValueExact();
        } catch (ArithmeticException e) {
            throw new ATMOperationException(ErrorCode.WRONG_AMOUNT);
        }
    }

    private boolean unavailableCurrency(Money amount) {
        return !deposit.getCurrency()
                       .equals(amount.getCurrency());
    }

}
