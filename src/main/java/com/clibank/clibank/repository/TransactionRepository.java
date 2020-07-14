package com.clibank.clibank.repository;

import com.clibank.clibank.model.TransactionDetails;
import com.clibank.clibank.model.User;
import com.clibank.clibank.model.UserAccountDetails;
import com.clibank.clibank.model.UserLoanDetails;

public interface TransactionRepository {
     int createTopupTransaction(User user, Double topupAmount , UserAccountDetails userAccountDetails);
    int createTransaction(UserAccountDetails debitAccountDetails, Double transactionAmount , UserAccountDetails creditAccountDetails, String transactionType);
    TransactionDetails getLoanTransactionDetailsDebitUserid(int userId);
    TransactionDetails getLoanTransactionDetailsCreditUserid(int userId);
    int createTransactionLoan(UserLoanDetails userLoanDetails, Double transactionAmount , String transactionType);


}
