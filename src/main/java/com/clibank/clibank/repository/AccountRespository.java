package com.clibank.clibank.repository;

import com.clibank.clibank.model.UserAccountDetails;

public interface AccountRespository {

    int createAccount(int userId, String accountNumber);

    UserAccountDetails getUserAccountDetails(int Userid);

    int updateBalance(int userId, Double amount , int version);
    int updateEarMarkAmount(int userId, Double amount, int version);
    int updateLoanAmount(int userId, Double amount , int version);
     int updateBalanceAndEarMarkAmount(int userId, Double Balanceamount ,Double earMarkAmount, int version);
    int updateisLoanPaymentAllowed(int userId, String isAllowed, int version);
    int updateLoanAmountAndLoanRepayment(int userId, Double amount ,String isAllowed,int version);
}
