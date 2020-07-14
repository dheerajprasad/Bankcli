package com.clibank.clibank.repository;

import com.clibank.clibank.model.UserAccountDetails;
import com.clibank.clibank.model.UserLoanDetails;

public interface LoanRespository {

    int createAccount(int userId, String accountNumber, int payToUserId);

    UserLoanDetails getUserAccountDetails(int Userid);

    UserLoanDetails getUserAccountDetailsPayToUserId(int payToUserId);

    int updateBalance(int userId, Double amount, int version);

    int updateEarMarkAmount(int userId, Double amount, int version);

    int updateBalanceAndEarMarkAmount(int userId, Double Balanceamount, Double earMarkAmount, int version);
}
