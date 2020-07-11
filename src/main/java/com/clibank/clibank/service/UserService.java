package com.clibank.clibank.service;

import com.clibank.clibank.constants.PaymentTransactionTypes;
import com.clibank.clibank.constants.TransactionTypes;
import com.clibank.clibank.model.TransactionDetails;
import com.clibank.clibank.repository.AccountRespository;
import com.clibank.clibank.repository.TransactionRepository;
import com.clibank.clibank.repository.UserRepository;
import com.clibank.clibank.repository.UserRepositoryimpl;
import com.clibank.clibank.model.User;
import com.clibank.clibank.model.UserAccountDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    //POOL ACCOUNT
    private static final int POOL_ACCOUNT_ID = 1;
    private static final String POOL_ACCOUNT_NAME = "POOL ACCOUNT";

    private final Map<Long, User> Users = new HashMap<>();
    private final AtomicBoolean connected = new AtomicBoolean();


    private User loggedInUser;

    public User getLoggedInuser() {
        return this.loggedInUser;
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

    public User checkUserExistsElseCreateuser(String userName) {

        User user = userRepository.getUserByName(userName);
        if (user == null) {
            userRepository.createuser(userName);
            user = userRepository.getUserByName(userName);
            accountRespository.createAccount(user.getId(), generateAccountNumber());
        }
        this.Connect(user);
        return user;

    }

    public User getUserDetails(String userName) {

        return userRepository.getUserByName(userName);
    }

    public Double getAccountBalance(int userid) {

        UserAccountDetails userAccountDetails = accountRespository.getUserAccountDetails(userid);
        return userAccountDetails.getBalance();

    }

    public UserAccountDetails getAccountDetails(int Userid) {
        return accountRespository.getUserAccountDetails(Userid);
    }


    public String usernames(User user) {
        String name = user.getUserName();
        return name;
    }

    public PaymentTransactionTypes topUpTransaction(User topupUser, Double topupAmount) {

        // Toup up the Account -- Source Pool Account --

        UserAccountDetails topupUserAccntDtls = accountRespository.getUserAccountDetails(topupUser.getId());
        UserAccountDetails poolAccntDtls = getPoolAccountDetails();
        User poolAcntuser = getPoolUserDetails();
        Double debitAccountOriginalBalance = poolAccntDtls.getBalance();
        Double updatedDebitBalance = debitAccountOriginalBalance - topupAmount;
        Double creditAccountOriginalBalance = topupUserAccntDtls.getBalance();
        Double creditUpdatedAccountBalance = creditAccountOriginalBalance + topupAmount;
        //Do a Topup -Payment from Pool to TopupUser
        PaymentTransactionTypes transactionTypes = createPaymentTransaction(debitAccountOriginalBalance, updatedDebitBalance, creditAccountOriginalBalance, creditUpdatedAccountBalance, topupAmount, poolAcntuser, topupUser, poolAccntDtls, topupUserAccntDtls, TransactionTypes.TOPUP);

        if (transactionTypes.equals(PaymentTransactionTypes.PAYMENT_TRANCTION_SUCESS)) {

            log.info("Top up for the User is Successful");

            // Check Loan Transaction Present

            log.info("Checking Loan Transaction Present for the User {} and amount is {}", topupUser.getUserName(), topupUserAccntDtls.getLoanAmount());

            topupUserAccntDtls = accountRespository.getUserAccountDetails(topupUser.getId());

            if (topupUserAccntDtls.getLoanAmount() > 0.0) {


                log.info("Loan Transaction Present for the User");

                //  Check Loan Transaction Present -YES -- Get Loan Details

                TransactionDetails loanTransactionDetails = transactionRepository.getLoanTransactionDetails(topupUser.getId());

                Double loanAmount = loanTransactionDetails.getTransaction_amount();

                // Debit TopupUser Credit Credituser

                UserAccountDetails creditUserAccountDetails = accountRespository.getUserAccountDetails(loanTransactionDetails.getCredit_userid());

                creditAccountOriginalBalance = creditUserAccountDetails.getBalance();
                User creditUser = userRepository.getUserByid(loanTransactionDetails.getCredit_userid());

                Double updatedLoanAmount = 0.0;
                Double creditAccountUpdatedBalance = 0.0;
                Double transactionAmount =0.0;

                if (topupUserAccntDtls.getBalance() >= loanAmount) {
                    // account Balance Greater than Loan -- create Payment transaction and update loan amount to 0

                    updatedDebitBalance = topupUserAccntDtls.getBalance() - loanAmount;

                    creditAccountUpdatedBalance = creditAccountOriginalBalance + loanAmount;
                    transactionAmount =loanAmount;

                    //debit Account -- Current User
                } else {
                    // account Balance Less than Loan -- create Payment transaction and update Balance to Zero
                    log.info("account Balance Less than Loan -- create Payment transaction and update Balance to Zero");

                    updatedDebitBalance = 0.0;
                    creditAccountUpdatedBalance = creditAccountOriginalBalance + topupUserAccntDtls.getBalance();
                    transactionAmount =  topupUserAccntDtls.getBalance();


                }

                PaymentTransactionTypes transactionTypesPayment = createPaymentTransaction(topupUserAccntDtls.getBalance(), updatedDebitBalance, creditAccountOriginalBalance, creditAccountUpdatedBalance, transactionAmount, topupUser, creditUser, topupUserAccntDtls, creditUserAccountDetails, TransactionTypes.LOAN_REPAYMENT);
                //Loan Repayment Transaction
                if (transactionTypesPayment.equals(PaymentTransactionTypes.PAYMENT_TRANCTION_SUCESS)) {
                    // Topup  Payment Success
                    log.info("Topup  Payment Success -- Loan Repayment Success");

                    //update loan amount for topup User
                    int loanPaymentTranStatus = accountRespository.updateLoanAmount(topupUser.getId(), 0.0, topupUserAccntDtls.getVersion());
                    if (loanPaymentTranStatus == 1) {
                        log.info("Topup  Payment Success -- Loan Repayment Success -- update Loan Amount Success");

                        return PaymentTransactionTypes.TOP_UP_SUCCESS_LOANPAYMENT_SUCCESS;

                    } else {
                        log.info("Topup  Payment Success -- Loan Repayment Success -- update Loan Amount Failure");

                        //revert Loan Repayment

                        return PaymentTransactionTypes.TOP_UP_SUCCESS_LOANPAYMENT_SUCCESS_UPDATE_LOAN_AMOUNT_FAILURE;

                        // delete loan repayment transaction only


                    }

                } else {
                    // Topup Success -- Loan Repayment Failure
                    log.info("Topup Success -- Loan Repayment Failure");

                    return PaymentTransactionTypes.TOP_UP_SUCCESS_LOAN_REPAYMENT_FAILURE;


                }
                // Credit Account -- Loan Transaction Credit User


            } else {
                log.info("No Loan Transaction Available for this User -- Top up Successfull ");

                return PaymentTransactionTypes.TOP_UP_SUCCESS;

            }


        } else {

            log.info("Top up for the User Failed");

            return PaymentTransactionTypes.TOP_UP_FAILURE;

        }


    }

    public PaymentTransactionTypes pay(UserAccountDetails creditAccountDetails, Double transactionAmount) {

        User debitUser = this.getLoggedInuser();
        UserAccountDetails debitAccountDetails = accountRespository.getUserAccountDetails(debitUser.getId());
        User creditUser = userRepository.getUserByid(creditAccountDetails.getUserid());

        // Do not Allow Payment if Balance is Zero //Assumption
        if (debitAccountDetails.getBalance() <= 0.0) {

            log.info("debitAccount Balance Less than 0 ");
            return PaymentTransactionTypes.INVALID_PAYMENT_TRANSACTION_NO_DEBIT_BALANCE;
        }

        //Check Balance >= Transaction Amount
        else if (debitAccountDetails.getBalance() >= transactionAmount) {
            log.info("debitAccountDetails.getBalance() >= transactionAmount ");
            return payTranAmountLessOrEqualToBalance(debitUser, creditUser, debitAccountDetails, creditAccountDetails, transactionAmount);

        } else {
            // Do not Allow Loan Transaction if there is already a existing Loan //Assumption
            if (debitAccountDetails.getLoanAmount() > 0.0) {
                log.info("Payment Transaction with Loan not allowed as there is Existing Loan for this user");
                return PaymentTransactionTypes.INVALID_PAYMENT_TRANSACTION_NO_LOAN_ALLOWED_EXISTING_LOAN_PRESENT;

            } else {

                return payTranAmountMoreThanBalance(debitUser, creditUser, debitAccountDetails, creditAccountDetails, transactionAmount);
            }
        }


    }


    public PaymentTransactionTypes payTranAmountLessOrEqualToBalance(User debitUser, User creditUser, UserAccountDetails debitAccountDetails, UserAccountDetails creditAccountDetails, Double transactionAmount) {

        //debitAccountOrignialBalance
        Double debitAccountOrignialBalance = debitAccountDetails.getBalance();
        // debit Amount from debitAccount
        Double updatedDebitBalance = debitAccountOrignialBalance - transactionAmount;
        // creditAccountOrignialBalance
        Double creditAccountOrignialBalance = creditAccountDetails.getBalance();
        // updated latest Balance = creditaccountpresentbalance + transactionAmount
        Double creditUpdatedAccountBalance = creditAccountOrignialBalance + transactionAmount;

        PaymentTransactionTypes transactionTypes = createPaymentTransaction(debitAccountOrignialBalance, updatedDebitBalance, creditAccountOrignialBalance, creditUpdatedAccountBalance, transactionAmount, debitUser, creditUser, debitAccountDetails, creditAccountDetails, TransactionTypes.PAYMENT);

        return transactionTypes;
    }


    public PaymentTransactionTypes createPaymentTransaction(Double debitAccountOrignialBalance, Double updatedDebitBalance, Double creditAccountOrignialBalance, Double creditUpdatedAccountBalance, Double transactionAmount, User debitUser, User creditUser, UserAccountDetails debitAccountDetails, UserAccountDetails creditAccountDetails, TransactionTypes transactionTypes) {

        log.info("debiting the amount {} from the User {} with updated Balance {} ", transactionAmount, debitUser.getUserName(), updatedDebitBalance);
        int debitTranStatus = accountRespository.updateBalance(debitAccountDetails.getUserid(), updatedDebitBalance, debitAccountDetails.getVersion());

        // debit Transaction Successfull
        if (debitTranStatus == 1) {
            log.info("debiting the amount {} from the User {} with updated Balance {}  -- Transaction Success", transactionAmount, debitUser.getUserName(), updatedDebitBalance);
            log.info("Credit Account present balance is amount {}", creditAccountOrignialBalance);

            int credittranstatus = accountRespository.updateBalance(creditAccountDetails.getUserid(), creditUpdatedAccountBalance, creditAccountDetails.getVersion());
            log.info("crediting the amount {} to the User {} with updated Balance {} ", transactionAmount, creditUser.getUserName(), creditUpdatedAccountBalance);

            if (credittranstatus == 1) {
                // credit Transaction Successfull
                log.info("Transaction Success --crediting the amount {} to the User {} with updated Balance {}", transactionAmount, creditUser.getUserName(), creditUpdatedAccountBalance);

                // debit and credit Success -- Create a payment Transaction record

                int transactionInsertStatus = transactionRepository.createTransaction(debitAccountDetails, transactionAmount, creditAccountDetails, transactionTypes.name());

                if (transactionInsertStatus == 1) {
                    // Transaction record Creation Successfull
                    log.info("Transaction Success --Inserting Record in to Transaction Table");

                    return PaymentTransactionTypes.PAYMENT_TRANCTION_SUCESS;

                } else {
                    //  Debit/Credit Success --Transaction record Creation Failure -- Revert both Debit and Credit Transaction

                    //revert debit transaction
                    log.info("Debit Revertion -- crediting the amount {} to the Debited User {} with updated Balance {} -- Transaction Success", transactionAmount, debitUser.getUserName(), debitAccountOrignialBalance);

                    int revertDebitTranStatus = accountRespository.updateBalance(debitAccountDetails.getUserid(), debitAccountOrignialBalance, debitAccountDetails.getVersion());

                    if (revertDebitTranStatus == 1) {

                        //  Debit/Credit Success --Transaction record Creation Failure -- Revert Debit Transaction Success

                        log.info("Debit Revertion  Success");

                        // revert credit Transaction

                        //revert credit transaction
                        log.info("credit Revertion -- reverting the credited amount {} to the  User {} with updated Balance {} -- Transaction Success", transactionAmount, creditUser.getUserName(), creditAccountOrignialBalance);

                        int revertCreditTranStatus = accountRespository.updateBalance(creditAccountDetails.getUserid(), creditAccountOrignialBalance, creditAccountDetails.getVersion());

                        if (revertCreditTranStatus == 1) {
                            //  Debit/Credit Success --Transaction record Creation Failure -- Revert Debit/Credit Transaction Success

                            log.info("Credit Revertion  Success");
                            return PaymentTransactionTypes.DEBIT_SUCCESS_CREDIT_SUCCESS_TRANSACTION_CREATE_FAUIRE;

                        } else {
                            //  Debit/Credit Success --Transaction record Creation Failure -- Revert Debit Transaction Success / Revert Credit Failure
                            log.info("Credit Revertion  Failure");
                            return PaymentTransactionTypes.DEBIT_SUCCESS_CREDIT_SUCCESS_TRANSACTION_CREATE_FAUIRE_DEBIT_REVERT_SUCCESS_CREDIT_REVERT_FAILURE;

                        }


                    } else {

                        // //  Debit/Credit Success --Transaction record Creation Failure -- Revert Debit Transaction Failure

                        log.info("Debit Revertion  Faiure --  crediting the amount {} to the Debited User {} with updated Balance {} ", transactionAmount, debitUser.getUserName(), creditAccountOrignialBalance);

                        return PaymentTransactionTypes.DEBIT_SUCCESS_CREDIT_SUCCESS_TRANSACTION_CREATE_FAUIRE_DEBIT_REVERT_FAILURE;
                    }


                }


            } else {
                //debit Success -- Credit Failure

                //revert debit transaction
                log.info("Debit Revertion -- crediting the amount {} to the Debited User {} with updated Balance {} -- Transaction Success", transactionAmount, debitUser.getUserName(), debitAccountOrignialBalance);

                int revertDebitTranStatus = accountRespository.updateBalance(debitAccountDetails.getUserid(), debitAccountOrignialBalance, debitAccountDetails.getVersion());

                if (revertDebitTranStatus == 1) {

                    log.info("Debit Revertion  Success-- crediting the amount {} to the Debited User {} with updated Balance {}", transactionAmount, debitUser.getUserName(), debitAccountOrignialBalance);
                    return PaymentTransactionTypes.DEBIT_SUCCESS_CREDIT_FAILURE;
                } else {

                    // debit revertion Failure

                    log.info("Debit Revertion  Faiure --  crediting the amount {} to the Debited User {} with updated Balance {} ", transactionAmount, debitUser.getUserName(), creditAccountOrignialBalance);

                    return PaymentTransactionTypes.DEBIT_SUCCESS_CREDIT_FAILURE_DEBIT_REVERT_FAILURE;
                }


            }


        } else { // debit Transaction Failure

            log.info("debiting the amount {} from the User {} with updated Balance {}  -- Transaction Failure", transactionAmount, debitUser.getUserName(), debitAccountOrignialBalance);

            return PaymentTransactionTypes.DEBIT_TRANFAILURE;

        }


    }


    public PaymentTransactionTypes payTranAmountMoreThanBalance(User debitUser, User creditUser, UserAccountDetails debitAccountDetails, UserAccountDetails creditAccountDetails, Double transactionAmount) {

        //debitAccountOrignialBalance
        Double debitAccountOrignialBalance = debitAccountDetails.getBalance();
        // debit Amount from debitAccount
        Double creditAccountOrignialBalance = creditAccountDetails.getBalance();
        // updated latest Balance = creditaccountpresentbalance + transactionAmount

        Double updatedDebitBalance = 0.0;
        // updated latest Balance = creditaccountpresentbalance + transactionAmount

        Double updatedTransactionAmount = debitAccountDetails.getBalance();

        Double DebitLoanBalanceToCreditor = Math.abs(debitAccountOrignialBalance - transactionAmount);
        // creditAccountOrignialBalance

        Double creditUpdatedAccountBalance = creditAccountOrignialBalance + updatedTransactionAmount;


        // create a Loan Transaction

        PaymentTransactionTypes loanTransactionTypes = createLoanTransaction(debitUser, creditUser, debitAccountDetails, creditAccountDetails, DebitLoanBalanceToCreditor);

        if (loanTransactionTypes.equals(PaymentTransactionTypes.LOAN_TRANSACTION_SUCCESS)) {
            log.info("Loan Transaction Success -- Creating the Payment Transaction");

            PaymentTransactionTypes payTransactionType = createPaymentTransaction(debitAccountOrignialBalance, updatedDebitBalance, creditAccountOrignialBalance, creditUpdatedAccountBalance, updatedTransactionAmount, debitUser, creditUser, debitAccountDetails, creditAccountDetails, TransactionTypes.PAYMENT);

            if (payTransactionType.equals(PaymentTransactionTypes.PAYMENT_TRANCTION_SUCESS)) {
                log.info("Loan Transaction and Payment Transaction Success");

                return PaymentTransactionTypes.LOAN_AND_PAYMNET_SUCCESS;
            } else {
                log.info("Loan Transaction Success and Payment Transaction Failure -- Reverting Loan Transaction");

                int loanAmtReverStat = accountRespository.updateLoanAmount(debitUser.getId(), debitAccountOrignialBalance, debitAccountDetails.getVersion());

                if (loanAmtReverStat == 1) {
                    // Loan Transaction Success and Payment Transaction Failure  // Reverting Loan Transaction --- Success
                    log.info("Reverting Loan Transaction --- Success");

                    return PaymentTransactionTypes.LOAN_SUCCESS_PAYMNET_FAILURE;

                } else {
                    // Loan Transaction Success and Payment Transaction Failure  // Reverting Loan Amount Transaction --- Failure
                    log.info("Loan Transaction Success and Payment Transaction Failure  // Reverting Loan Amount Transaction --- Failure");
                    return PaymentTransactionTypes.LOAN_SUCCESS_PAYMNET_FAILURE_REVERT_LOAN_AMT_FAILURE;

                }


            }

        } else {
            log.info("Loan Transaction Failure -- Returning Failure");
            return loanTransactionTypes;


        }


    }


    public PaymentTransactionTypes createLoanTransaction(User debitUser, User creditUser, UserAccountDetails debitAccountDetails, UserAccountDetails creditAccountDetails, Double loanAmount) {

        Double originalLoanAmountforDebitUser = debitAccountDetails.getLoanAmount();


        Double updatedLoanAmount = originalLoanAmountforDebitUser + loanAmount;
        log.info("Orignal Loan amount {} for the user", debitAccountDetails.getLoanAmount(), debitUser.getUserName());
        // update the Loan Amount
        log.info("Updating the Loan amount {} for the user", loanAmount, debitUser.getUserName());
        int updateLoanAmtStatus = accountRespository.updateLoanAmount(debitUser.getId(), loanAmount, debitAccountDetails.getVersion());
        if (updateLoanAmtStatus == 1) {
            log.info(" Transaction Success -- Updating the Loan amount {} for the user", loanAmount, debitUser.getUserName());

            // update LoanAmount Success -- create Transaction Record for loan transaction

            int transactionInsertStatus = transactionRepository.createTransaction(debitAccountDetails, updatedLoanAmount, creditAccountDetails, TransactionTypes.LOAN.name());

            if (transactionInsertStatus == 1) {

                // Loan upadate Success and Loan Transaction Creation is Success

                log.info(" Loan update Success and Loan Transaction Creation is Success");

                return PaymentTransactionTypes.LOAN_TRANSACTION_SUCCESS;

            } else {

                log.info(" Loan update Success and Loan Transaction Creation is Failure");
                // Loan upadate Success and Loan Transaction Creation is Failure
                //revert Loan Amount

                int loanAmtReverStat = accountRespository.updateLoanAmount(debitUser.getId(), originalLoanAmountforDebitUser, debitAccountDetails.getVersion());

                if (loanAmtReverStat == 1) {

                    return PaymentTransactionTypes.LOAN_UPDATE_SUCCESS_TRAN_INS_FAILURE;

                    //

                } else {

                    //Loan update Success tran insertion failure -- loanrevertion Failure

                    return PaymentTransactionTypes.LOAN_UPDATE_SUCCESS_TRAN_INS_FAILURE_LOAN_REVERT_FAILURE;


                }


            }


        } else {
            log.info(" Transaction Failure -- Updating the Loan amount {} for the user", loanAmount, debitUser.getUserName());

            return PaymentTransactionTypes.LOAN_UPDATE_AMOUNT_FAIURE;

        }

    }


    private String generateAccountNumber() {

        return UUID.randomUUID().toString();
    }

}
