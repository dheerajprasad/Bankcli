package com.clibank.clibank.service;

import com.clibank.clibank.constants.UserCreation;
import com.clibank.clibank.model.TransactionDetails;
import com.clibank.clibank.model.User;
import com.clibank.clibank.model.UserAccountDetails;
import com.clibank.clibank.model.UserLoanDetails;

import java.util.Collection;
import java.util.List;

public interface Userservice {

    String usernames(User user);
    UserLoanDetails getLoanAccountforPaytoDetails(int Userid);
    UserLoanDetails getLoanAccountDetails(int Userid);
    UserAccountDetails getAccountDetails(int Userid);
    TransactionDetails getLoggedinUserLoanDetails();
    Double getAccountBalance(int userid);
    User getUserDetails(int userid);
    User getUserDetails(String userName);
    TransactionDetails getLoanTransactionDetailsCreditUserid(int creditUserid);
    UserCreation checkUserExistsElseCreateuser(String userName);
    List<String> getusernames();
    List<User> getusers() ;
    User getPoolUserDetails();
    UserAccountDetails getPoolAccountDetails() ;
    User findById(Long id);
    Collection<User> findByName(String name);
    void disconnect() ;
    void Connect(User user);
    boolean isConnected();
    Double getTransferedAmount();
    User getLoggedInuser();
    void setTranferedAmount(Double tranferedAmount);

}
