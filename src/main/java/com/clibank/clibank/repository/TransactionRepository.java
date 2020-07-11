package com.clibank.clibank.repository;

import com.clibank.clibank.model.TransactionDetails;
import com.clibank.clibank.model.User;
import com.clibank.clibank.model.UserAccountDetails;

public interface TransactionRepository {
     int createTopupTransaction(User user, Double topupAmount , UserAccountDetails userAccountDetails);
    int createTransaction(UserAccountDetails debitAccountDetails, Double transactionAmount , UserAccountDetails creditAccountDetails, String transactionType);
    TransactionDetails getLoanTransactionDetails(int userId);


}
