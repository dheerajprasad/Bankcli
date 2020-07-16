package com.clibank.clibank.service;

import com.clibank.clibank.constants.PaymentTransactionTypes;
import com.clibank.clibank.model.User;

public interface TopupService {
    PaymentTransactionTypes topUpTransaction(User topupUser, Double topupAmount);
}
