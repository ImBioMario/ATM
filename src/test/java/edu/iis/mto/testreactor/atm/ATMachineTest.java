package edu.iis.mto.testreactor.atm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.iis.mto.testreactor.atm.bank.Bank;
import org.hamcrest.Matchers;
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
    void setUp() throws Exception {
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

}
