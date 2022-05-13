package edu.iis.mto.testreactor.atm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import edu.iis.mto.testreactor.atm.bank.AccountException;
import edu.iis.mto.testreactor.atm.bank.AuthorizationException;
import edu.iis.mto.testreactor.atm.bank.Bank;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ATMachineTest {
    @Mock
    Bank bank;

    Card card;
    PinCode pin;
    ATMachine atm;
    List<BanknotesPack> banknotes;

    @BeforeEach
    void setUp() {
        atm = new ATMachine(bank, Currency.getInstance("PLN"));
        banknotes = new ArrayList<>();
        pin = PinCode.createPIN(1, 2, 3, 4);
        card = Card.create("123456789");
    }

    @AfterEach
    void cleanUp() {
        atm = null;
        banknotes = null;
    }

    @Test
    void successfulWithdrawal() throws ATMOperationException {
        BanknotesPack tens = BanknotesPack.create(100, Banknote.PL_10);
        banknotes.add(tens);
        MoneyDeposit deposit = MoneyDeposit.create(atm.getCurrentDeposit().getCurrency(), banknotes);
        atm.setDeposit(deposit);

        List<BanknotesPack> correctWithdraw = new ArrayList<>();
        correctWithdraw.add(BanknotesPack.create(10, Banknote.PL_10));

        assertEquals(atm.withdraw(pin, card, new Money(100)), Withdrawal.create(correctWithdraw));
    }

    @Test
    void succesfulWithdrawalAndCorrectAmountOfDepositLeft() throws ATMOperationException {
        banknotes = List.of(
                BanknotesPack.create(0, Banknote.PL_10),
                BanknotesPack.create(0, Banknote.PL_20),
                BanknotesPack.create(0, Banknote.PL_50),
                BanknotesPack.create(100, Banknote.PL_100),
                BanknotesPack.create(0, Banknote.PL_200),
                BanknotesPack.create(0, Banknote.PL_500)
        );
        atm.setDeposit(MoneyDeposit.create(atm.getCurrentDeposit().getCurrency(), banknotes));

        List<BanknotesPack> correctWithdraw = new ArrayList<>();
        correctWithdraw.add(BanknotesPack.create(10, Banknote.PL_100));

        MoneyDeposit afterDeposit = MoneyDeposit.create(atm.getCurrentDeposit().getCurrency(), List.of(
                BanknotesPack.create(0, Banknote.PL_10),
                BanknotesPack.create(0, Banknote.PL_20),
                BanknotesPack.create(0, Banknote.PL_50),
                BanknotesPack.create(90, Banknote.PL_100),
                BanknotesPack.create(0, Banknote.PL_200),
                BanknotesPack.create(0, Banknote.PL_500)));

        assertEquals(atm.withdraw(pin, card, new Money(1000)), Withdrawal.create(correctWithdraw));

        assertEquals(afterDeposit, atm.getCurrentDeposit());
    }

    @Test
    void failedWithdrawalWithFailedAuthorization() throws AuthorizationException {

        doThrow(AuthorizationException.class).when(bank).autorize(any(), any());
        ATMOperationException errCode = assertThrows(ATMOperationException.class, () -> atm.withdraw(pin, card, new Money(100)));
        assertEquals(errCode.getErrorCode(), ErrorCode.AUTHORIZATION);
    }

    @Test
    void failedWithdrawalWrongCurrency() {
        BanknotesPack hundreds = BanknotesPack.create(100, Banknote.PL_200);
        banknotes.add(hundreds);
        MoneyDeposit deposit = MoneyDeposit.create(atm.getCurrentDeposit().getCurrency(), banknotes);
        atm.setDeposit(deposit);

        ATMOperationException errCode = assertThrows(ATMOperationException.class, () -> atm.withdraw(pin, card, new Money(200, "USD")));
        assertEquals(errCode.getErrorCode(), ErrorCode.WRONG_CURRENCY);
    }

    @Test
    void failedWithdrawalWrongAmount() {
        BanknotesPack hundreds = BanknotesPack.create(100, Banknote.PL_500);
        banknotes.add(hundreds);
        MoneyDeposit deposit = MoneyDeposit.create(atm.getCurrentDeposit().getCurrency(), banknotes);
        atm.setDeposit(deposit);

        ATMOperationException errCode = assertThrows(ATMOperationException.class, () -> atm.withdraw(pin, card, new Money(200)));
        assertEquals(errCode.getErrorCode(), ErrorCode.WRONG_AMOUNT);
    }

    @Test
    void failedWithdrawalAccountExceptionThrownByBank() throws AccountException {
        BanknotesPack hundreds = BanknotesPack.create(100, Banknote.PL_500);
        banknotes.add(hundreds);
        MoneyDeposit deposit = MoneyDeposit.create(atm.getCurrentDeposit().getCurrency(), banknotes);
        atm.setDeposit(deposit);

        doThrow(AccountException.class).when(bank).charge(any(), any());
        ATMOperationException errCode = assertThrows(ATMOperationException.class, () -> atm.withdraw(pin, card, new Money(500)));
        assertEquals(errCode.getErrorCode(), ErrorCode.NO_FUNDS_ON_ACCOUNT);
    }

    @Test
    void failedWithdrawalTryingToGetPennies() {
        BanknotesPack hundreds = BanknotesPack.create(100, Banknote.PL_500);
        banknotes.add(hundreds);
        MoneyDeposit deposit = MoneyDeposit.create(atm.getCurrentDeposit().getCurrency(), banknotes);
        atm.setDeposit(deposit);

        ATMOperationException errCode = assertThrows(ATMOperationException.class, () -> atm.withdraw(pin, card, new Money(0.5)));
        assertEquals(errCode.getErrorCode(), ErrorCode.WRONG_AMOUNT);
    }

    @Test
    void succesfulWithdrawalThoroughDepositCheck() throws ATMOperationException {
        banknotes = List.of(BanknotesPack.create(3, Banknote.PL_10),
                BanknotesPack.create(1, Banknote.PL_20),
                BanknotesPack.create(1, Banknote.PL_50),
                BanknotesPack.create(1, Banknote.PL_100),
                BanknotesPack.create(2, Banknote.PL_200),
                BanknotesPack.create(2, Banknote.PL_500));

        atm.setDeposit(MoneyDeposit.create(atm.getCurrentDeposit().getCurrency(), banknotes));
        List<BanknotesPack> correctWithdraw = new ArrayList<>(List.of(
                BanknotesPack.create(2, Banknote.PL_500),
                BanknotesPack.create(2, Banknote.PL_200),
                BanknotesPack.create(1, Banknote.PL_100),
                BanknotesPack.create(1, Banknote.PL_50),
                BanknotesPack.create(1, Banknote.PL_20),
                BanknotesPack.create(1, Banknote.PL_10)));

        MoneyDeposit afterDeposit = MoneyDeposit.create(atm.getCurrentDeposit().getCurrency(), List.of(BanknotesPack.create(2, Banknote.PL_10),
                BanknotesPack.create(0, Banknote.PL_20),
                BanknotesPack.create(0, Banknote.PL_50),
                BanknotesPack.create(0, Banknote.PL_100),
                BanknotesPack.create(0, Banknote.PL_200),
                BanknotesPack.create(0, Banknote.PL_500)));
        assertEquals(atm.withdraw(pin, card, new Money(1580)), Withdrawal.create(correctWithdraw));
        assertEquals(atm.getCurrentDeposit(), afterDeposit);

    }
}
