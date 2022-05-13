package edu.iis.mto.testreactor.atm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import edu.iis.mto.testreactor.atm.bank.AuthorizationException;
import edu.iis.mto.testreactor.atm.bank.Bank;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
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
        pin = PinCode.createPIN(1,2,3,4);
        card = Card.create("123456789");
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
        BanknotesPack hundreds = BanknotesPack.create(100, Banknote.PL_100);
        banknotes.add(hundreds);
        MoneyDeposit deposit = MoneyDeposit.create(atm.getCurrentDeposit().getCurrency(), banknotes);
        atm.setDeposit(deposit);

        List<BanknotesPack> correctWithdraw = new ArrayList<>();
        correctWithdraw.add(BanknotesPack.create(10, Banknote.PL_100));

        assertEquals(atm.withdraw(pin, card, new Money(1000)), Withdrawal.create(correctWithdraw));
        int banknotesLeft = atm.getCurrentDeposit().getBanknotes().get(0).getCount();
        assertEquals(banknotesLeft, 100-10);
    }

    @Test
    void failedWithdrawalWithFailedAuthorization() throws AuthorizationException {

        doThrow(AuthorizationException.class).when(bank).autorize(any(), any());
        ATMOperationException errCode = assertThrows(ATMOperationException.class, () -> atm.withdraw(pin, card, new Money(100)));
        assertEquals(errCode.getErrorCode(), ErrorCode.AUTHORIZATION);
    }

}
