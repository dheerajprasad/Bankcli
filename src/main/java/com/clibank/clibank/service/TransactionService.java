package com.clibank.clibank.service;

import com.clibank.clibank.constants.PaymentTransactionTypes;
import com.clibank.clibank.constants.TransactionTypes;
import com.clibank.clibank.model.User;
import com.clibank.clibank.model.UserAccountDetails;
import com.clibank.clibank.model.UserLoanDetails;

public interface TransactionService {
    PaymentTransactionTypes createPaymentTransaction(Double debitAccountOrignialBalance, Double
            updatedDebitBalance, Double creditAccountOrignialBalance, Double creditUpdatedAccountBalance, Double
                                                             transactionAmount, User debitUser, User creditUser, UserAccountDetails debitAccountDetails, UserAccountDetails
                                                             creditAccountDetails, TransactionTypes transactionTypes);
    public PaymentTransactionTypes createLoanTransaction(UserLoanDetails userLoanDetails, Double loanAmount) ;
}
