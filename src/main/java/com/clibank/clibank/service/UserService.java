package com.clibank.clibank.service;

import com.clibank.clibank.constants.PaymentTransactionTypes;
import com.clibank.clibank.constants.TransactionTypes;
import com.clibank.clibank.constants.UserCreation;
import com.clibank.clibank.model.TransactionDetails;
import com.clibank.clibank.model.UserLoanDetails;
import com.clibank.clibank.repository.AccountRespository;
import com.clibank.clibank.repository.LoanRespository;
import com.clibank.clibank.repository.TransactionRepository;
import com.clibank.clibank.repository.UserRepositoryimpl;
import com.clibank.clibank.model.User;
import com.clibank.clibank.model.UserAccountDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {
    @Autowired
    UserRepositoryimpl userRepository;

    @Autowired
    AccountRespository accountRespository;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    LoanRespository loanRespository;

    //POOL ACCOUNT
    private static final int POOL_ACCOUNT_ID = 1;
    private static final String POOL_ACCOUNT_NAME = "POOL ACCOUNT";

    private final Map<Long, User> Users = new HashMap<>();
    private final AtomicBoolean connected = new AtomicBoolean();


    private User loggedInUser;

    public Double getTranferedAmount() {
        return tranferedAmount;
    }

    public void setTranferedAmount(Double tranferedAmount) {
        this.tranferedAmount = tranferedAmount;
    }

    private Double tranferedAmount = 0.0;

    public User getLoggedInuser() {
        return this.loggedInUser;
    }

    public Double getTransferedAmount() {
        return this.tranferedAmount;
    }

    public boolean isConnected() {
        return this.connected.get();

    }

    public void Connect(User user) {
        this.connected.set(true);
        this.loggedInUser = user;
    }

    public void disconnect() {
        this.connected.set(false);
        this.loggedInUser = null;

    }

    public User findById(Long id) {
        return this.Users.get(id);
    }

    public Collection<User> findByName(String name) {
        return this.Users.values().stream().filter(p -> p.getUserName().toLowerCase().contains(name.toLowerCase())).collect(Collectors.toList());
    }

    public UserAccountDetails getPoolAccountDetails() {

        return accountRespository.getUserAccountDetails(POOL_ACCOUNT_ID);
    }

    public User getPoolUserDetails() {

        return userRepository.getUserByid(POOL_ACCOUNT_ID);
    }

    public List<User> getusers() {
        return userRepository.findallusers();
    }

    public List<String> getusernames() {
        System.out.println("getusernames");
        List<User> users = userRepository.findallusers();
        System.out.println("users.size " + users.size());
        users.forEach(System.out::println);
        return users.stream().map(this::usernames).collect(Collectors.toList());
    }

    public UserCreation checkUserExistsElseCreateuser(String userName) {

        try {
            User user = userRepository.getUserByName(userName);
            if (user == null) {
                int createUserResult = userRepository.createuser(userName);
                if (createUserResult == 1) {
                    user = userRepository.getUserByName(userName);
                    this.loggedInUser = user;
                    this.Connect(user);
                    int accountCreationResult = accountRespository.createAccount(user.getId(), user.getUserName());
                    if (accountCreationResult == 1) {
                        return UserCreation.USER_ACCOUNT_CREATION_SUCCESS;
                    } else {
                        return UserCreation.USER_CREATION_SUCCESS_ACCOUNT_CREATION_FAILURE;
                    }

                } else {
                    return UserCreation.USER_CREATION_FAILURE;
                }
            } else {
                user = userRepository.getUserByName(userName);
                this.Connect(user);
                return UserCreation.USER_EXIST_IN_SYSTEM;
            }


        } catch (Exception e) {
            log.error("checkUserExistsElseCreateuser creation Exception {}", e);
            return UserCreation.USER_ACCOUNT_CREATION_EXCEPTION;

        }


    }

    public TransactionDetails getLoanTransactionDetailsCreditUserid(int creditUserid) {

        return transactionRepository.getLoanTransactionDetailsCreditUserid(creditUserid);
    }

    public User getUserDetails(String userName) {

        return userRepository.getUserByName(userName);
    }

    public User getUserDetails(int userid) {

        return userRepository.getUserByid(userid);
    }


    public Double getAccountBalance(int userid) {

        UserAccountDetails userAccountDetails = accountRespository.getUserAccountDetails(userid);
        return userAccountDetails.getAvailableBalance();

    }

    public TransactionDetails getLoggedinUserLoanDetails() {

        return transactionRepository.getLoanTransactionDetailsDebitUserid(getLoggedInuser().getId());
    }

    public UserAccountDetails getAccountDetails(int Userid) {
        return accountRespository.getUserAccountDetails(Userid);
    }

    public UserLoanDetails getLoanAccountDetails(int Userid) {
        return loanRespository.getUserAccountDetails(Userid);
    }

    public UserLoanDetails getLoanAccountforPaytoDetails(int Userid) {
        return loanRespository.getUserAccountDetailsPayToUserId(Userid);
    }


    public String usernames(User user) {
        String name = user.getUserName();
        return name;
    }




    private String generateAccountNumber() {

        return UUID.randomUUID().toString();
    }

}
