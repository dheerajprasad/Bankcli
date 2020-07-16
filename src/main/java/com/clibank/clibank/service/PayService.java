package com.clibank.clibank.service;

import com.clibank.clibank.constants.PaymentTransactionTypes;
import com.clibank.clibank.model.User;
import com.clibank.clibank.model.UserAccountDetails;
import com.clibank.clibank.model.UserLoanDetails;

public interface PayService {
    PaymentTransactionTypes pay(UserAccountDetails creditAccountDetails, Double transactionAmount);
    PaymentTransactionTypes payTranAmountMoreThanBalance(User debitUser, User creditUser, UserAccountDetails
            debitAccountDetails, UserAccountDetails creditAccountDetails, Double transactionAmount);
    UserLoanDetails checkLoanAccountExistsElseCreate(User debitUser, User creditUser);
    PaymentTransactionTypes payTranAmountLessOrEqualToBalance(User debitUser, User
            creditUser, UserAccountDetails debitAccountDetails, UserAccountDetails creditAccountDetails, Double
                                                                      transactionAmount);
}
