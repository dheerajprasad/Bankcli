package com.clibank.clibank.service;

import com.clibank.clibank.constants.PaymentTransactionTypes;
import com.clibank.clibank.constants.TransactionTypes;
import com.clibank.clibank.model.TransactionDetails;
import com.clibank.clibank.repository.AccountRespository;
import com.clibank.clibank.repository.TransactionRepository;
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

    public User checkUserExistsElseCreateuser(String userName) {

        User user = userRepository.getUserByName(userName);
        if (user == null) {
            userRepository.createuser(userName);
            user = userRepository.getUserByName(userName);
            accountRespository.createAccount(user.getId(), user.getUserName());
        }
        this.Connect(user);
        return user;

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


    public String usernames(User user) {
        String name = user.getUserName();
        return name;
    }

    public PaymentTransactionTypes topUpTransaction(User topupUser, Double topupAmount) {

        // Toup up the Account -- Source Pool Account --

        UserAccountDetails topupUserAccntDtls = accountRespository.getUserAccountDetails(topupUser.getId());
        UserAccountDetails poolAccntDtls = getPoolAccountDetails();
        User poolAcntuser = getPoolUserDetails();
        Double debitAccountOriginalBalance = poolAccntDtls.getAvailableBalance();
        Double updatedDebitBalance = debitAccountOriginalBalance - topupAmount;
        Double creditAccountOriginalBalance = topupUserAccntDtls.getAvailableBalance();
        Double creditUpdatedAccountBalance = creditAccountOriginalBalance + topupAmount;
        //Do a Topup -Payment from Pool to TopupUser


        PaymentTransactionTypes transactionTypes = createPaymentTransaction(debitAccountOriginalBalance, updatedDebitBalance, creditAccountOriginalBalance, creditUpdatedAccountBalance, topupAmount, poolAcntuser, topupUser, poolAccntDtls, topupUserAccntDtls, TransactionTypes.TOPUP);

        if (transactionTypes.equals(PaymentTransactionTypes.PAYMENT_TRANCTION_SUCESS) || transactionTypes.equals(PaymentTransactionTypes.DEBIT_SUCCESS_CREDIT_SUCCESS_TRNRECORD_CREATE_SUCCESS_REMOVE_DEBIT_EAR_MARK_FAIURE_TRAN_SUCCESS)) {

            topupUserAccntDtls = accountRespository.getUserAccountDetails(topupUser.getId());
            log.info("Top up for the User is Successful Balance is " + topupUserAccntDtls.getAvailableBalance() + "Loan Amount is " + topupUserAccntDtls.getLoanAmount());


            // Check Loan Transaction Present

            log.info("Checking Loan Transaction Present for the User {} and amount is {}", topupUser.getUserName(), topupUserAccntDtls.getLoanAmount());


            if (topupUserAccntDtls.getLoanAmount() > 0.0 && topupUserAccntDtls.getIsLoanRepayMentAllowed().equalsIgnoreCase("TRUE")) {


                log.info("Loan Transaction Present for the User {} && getIsLoanRepayMentAllowed " + topupUserAccntDtls.getLoanAmount(), topupUserAccntDtls.getIsLoanRepayMentAllowed());

                //  Check Loan Transaction Present -YES -- Get Loan Details


                TransactionDetails loanTransactionDetails = transactionRepository.getLoanTransactionDetailsDebitUserid(topupUser.getId());

                Double loanAmount = topupUserAccntDtls.getLoanAmount();
                log.info("Loan Transaction Present for the User loanAmount -- {}", loanAmount);
                // Debit TopupUser Credit Credituser

                UserAccountDetails creditUserAccountDetails = accountRespository.getUserAccountDetails(loanTransactionDetails.getCredit_userid());

                creditAccountOriginalBalance = creditUserAccountDetails.getAvailableBalance();
                User creditUser = userRepository.getUserByid(loanTransactionDetails.getCredit_userid());

                Double updatedLoanAmount = 0.0;
                Double creditAccountUpdatedBalance = 0.0;
                Double transactionAmount = 0.0;


                if (topupUserAccntDtls.getAvailableBalance() >= loanAmount) {
                    // account Balance Greater than Loan -- create Payment transaction and update loan amount to 0

                    updatedDebitBalance = topupUserAccntDtls.getAvailableBalance() - loanAmount;

                    creditAccountUpdatedBalance = creditAccountOriginalBalance + loanAmount;
                    transactionAmount = loanAmount;
                    updatedLoanAmount = 0.0;

                    //debit Account -- Current User
                } else {
                    // account Balance Less than Loan -- create Payment transaction and update Balance to Zero
                    log.info("account Balance Less than Loan -- create Payment transaction and update Balance to Zero");

                    updatedDebitBalance = 0.0;

                    transactionAmount = topupUserAccntDtls.getAvailableBalance();
                    updatedLoanAmount = Math.abs(loanAmount - transactionAmount);
                    creditAccountUpdatedBalance = creditAccountOriginalBalance + topupAmount;
                }

                log.info("updatedDebitBalance {} , creditAccountUpdatedBalance {} ,transactionAmount {} ", updatedDebitBalance, creditAccountUpdatedBalance, transactionAmount);


                //update isLoanRepayment Allowed  to false for the user -- Reset Back once Loan Repayment Transaction is Successfull
                int updateisLoanRepayAllowedstatus = accountRespository.updateisLoanPaymentAllowed(topupUser.getId(), "FALSE", topupUserAccntDtls.getVersion());

                if (updateisLoanRepayAllowedstatus == 1) {
                    log.info("Updating isLoanRepayment Allowed for the TopUP User is SuccessFull");
                } else {
                    log.info("Updating isLoanRepayment Allowed for the TopUP User is Failure -- Reverting as Topup Success , Loan transaction Failure");
                    return PaymentTransactionTypes.TOP_UP_SUCCESS_LOAN_REPAYMENT_FAILURE;
                }

                // Payment Transaction to debit Topup user and Credit the Loanholding user Account

                Double debitLoanPaymentOrgBalance = topupUserAccntDtls.getAvailableBalance();
                Double creditLoanPaymentOrgBalance = creditAccountOriginalBalance;


                PaymentTransactionTypes transactionTypesPayment = createPaymentTransaction(topupUserAccntDtls.getAvailableBalance(), updatedDebitBalance, creditAccountOriginalBalance, creditAccountUpdatedBalance, transactionAmount, topupUser, creditUser, topupUserAccntDtls, creditUserAccountDetails, TransactionTypes.TOP_INITIATED_LOAN_REPAYMENT);


                topupUserAccntDtls = accountRespository.getUserAccountDetails(topupUser.getId());
                log.info(" topupUser User  Balance is " + topupUserAccntDtls.getAvailableBalance() + "Loan Amount is " + topupUserAccntDtls.getLoanAmount());
                creditUserAccountDetails = accountRespository.getUserAccountDetails(creditUserAccountDetails.getUserid());
                log.info(" creditUserAccountDetails User  Balance is " + creditUserAccountDetails.getAvailableBalance() + "Loan Amount is " + creditUserAccountDetails.getLoanAmount());


                //Loan Repayment Transaction
                if (transactionTypesPayment.equals(PaymentTransactionTypes.PAYMENT_TRANCTION_SUCESS) || transactionTypes.equals(PaymentTransactionTypes.DEBIT_SUCCESS_CREDIT_SUCCESS_TRNRECORD_CREATE_SUCCESS_REMOVE_DEBIT_EAR_MARK_FAIURE_TRAN_SUCCESS)) {
                    // Topup  Payment Success
                    log.info("Topup  Payment Success -- Loan Repayment Success");

                    //update loan amount for topup User

                    int loanUpdateTranStatus = accountRespository.updateLoanAmountAndLoanRepayment(topupUser.getId(), updatedLoanAmount, "TRUE", topupUserAccntDtls.getVersion());
                    if (loanUpdateTranStatus == 1) {
                        log.info("Topup  Payment Success -- Loan Repayment Success -- update Loan Amount Success");

                        return PaymentTransactionTypes.TOP_UP_SUCCESS_LOANPAYMENT_SUCCESS;

                    } else {
                        log.info("Topup  Payment Success -- Loan Repayment Success -- update Loan Amount Failure");

                        //Assumption - This type of Issue is very Rare --
                        // Blocking the Payment Transaction between this Users to Sort out the Update Loan clearing Process
                        // Manual Intervention to Sort out this Issue required - update Loan amount to updated Loan amount and set LoanPayment Allowed to TRUE

                        return PaymentTransactionTypes.TOP_UP_SUCCESS_LOANPAYMENT_SUCCESS_UPDATE_LOAN_AMOUNT_FAILURE;


                    }

                } else {
                    // Topup Success -- Loan Repayment Failure
                    log.info("Topup Success -- Loan Repayment Failure");

                    return PaymentTransactionTypes.TOP_UP_SUCCESS_LOAN_REPAYMENT_FAILURE;


                }
                // Credit Account -- Loan Transaction Credit User


            } else {
                log.info("No Loan Transaction or Loan Repayment Not Allowed Available for this User -- Top up Successfull ");

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
        if (debitAccountDetails.getAvailableBalance() <= 0.0) {

            log.info("debitAccount Balance Less than 0 ");
            return PaymentTransactionTypes.INVALID_PAYMENT_TRANSACTION_NO_DEBIT_BALANCE;
        }


        // Check whether the Creditor has Loan
        if (creditAccountDetails.getLoanAmount() > 0) {
            log.info("Creditor has  Previous Loan ");
            log.info("Checking if the Loan is Associated  With Payer");
            TransactionDetails CreditorLoanTransactionDetails = transactionRepository.getLoanTransactionDetailsDebitUserid(creditAccountDetails.getUserid());
            if (CreditorLoanTransactionDetails.getCredit_userid() == getLoggedInuser().getId()) {

                if (debitAccountDetails.getIsLoanRepayMentAllowed().equalsIgnoreCase("FALSE")) {

                    log.info("Loan Repayment Not Allowed for You --");
                    return PaymentTransactionTypes.LOAN_REPAY_DISPUTE_PRENDING;
                }

                log.info("Loan is Associated  With Payer with Transactionid " + CreditorLoanTransactionDetails.getId());

                Double originalLoanAmount = 0.0;
                Double updatedLoanAmount = 0.0;
                Double OriginalTranAmount = transactionAmount;
                if (transactionAmount <= creditAccountDetails.getLoanAmount()) {
                    log.info("transactionAmount  {} <  creditAccountDetails.getLoanAmount() {}", transactionAmount, creditAccountDetails.getLoanAmount());

                    // update Loan amount for the Creditor
                    originalLoanAmount = creditAccountDetails.getLoanAmount();
                    updatedLoanAmount = originalLoanAmount - transactionAmount;
                } else {
                    log.info("transactionAmount  {} >  creditAccountDetails.getLoanAmount() {}", transactionAmount, creditAccountDetails.getLoanAmount());
                    originalLoanAmount = creditAccountDetails.getLoanAmount();
                    updatedLoanAmount = 0.0;
                    log.info("Updating the Transaction Amount transactionAmount {} to transactionAmount- originalLoanAmount {}", transactionAmount, transactionAmount - originalLoanAmount);

                    // Updating the Transaction Amount to Proceed after Loan Transaction
                    transactionAmount = transactionAmount - originalLoanAmount;
                }
                log.info("updating updatedLoanAmount {}", updatedLoanAmount);
                int accountUpdateStatus = accountRespository.updateLoanAmountAndLoanRepayment(creditAccountDetails.getUserid(), updatedLoanAmount, "TRUE", creditAccountDetails.getVersion());

                if (accountUpdateStatus == 1) {
                    log.info("updating updatedLoanAmount -- Transaction Success");

                    // Create a Payment Record
                    debitAccountDetails = accountRespository.getUserAccountDetails(debitUser.getId());

                    Double debitAccountOriginalbalance = debitAccountDetails.getAvailableBalance();

                    log.info("updating updatedLoanAmount -- Transaction Success -- Creating a Payment Transaction");
                    PaymentTransactionTypes transactionTypes = createPaymentTransaction(debitAccountOriginalbalance, debitAccountOriginalbalance, creditAccountDetails.getAvailableBalance(), creditAccountDetails.getAvailableBalance(), updatedLoanAmount, debitUser, creditUser, debitAccountDetails, creditAccountDetails, TransactionTypes.PAYMENT_INITIATED_LOAN_REPAYMENT);

                    if (transactionTypes.equals(PaymentTransactionTypes.PAYMENT_TRANCTION_SUCESS) || transactionTypes.equals(PaymentTransactionTypes.DEBIT_SUCCESS_CREDIT_SUCCESS_TRNRECORD_CREATE_SUCCESS_REMOVE_DEBIT_EAR_MARK_FAIURE_TRAN_SUCCESS)) {
                        log.info("updating updatedLoanAmount -- Transaction Success -- Creating a Payment Transaction -- Success");

                        if (OriginalTranAmount <= creditAccountDetails.getLoanAmount()) {

                            return PaymentTransactionTypes.PAYMENT_TO_LOAN_SUCCESS;
                        } else {

                            log.info("Since  OriginalTranAmount > creditAccountDetails.getLoanAmount() -- Will Continue the transaction Below");
                        }

                    } else {
                        log.info("updating updatedLoanAmount -- Transaction Success -- Creating a Payment Transaction -- Failure");

                        // revert the update LoanAmount
                        int accountRevertStatus = accountRespository.updateLoanAmountAndLoanRepayment(CreditorLoanTransactionDetails.getCredit_userid(), originalLoanAmount, "TRUE", creditAccountDetails.getVersion());
                        if (accountRevertStatus == 1) {
                            log.info("updating updatedLoanAmount -- Transaction Success -- Creating a Payment Transaction -- Failure -- Reverting Loan Amount Success");
                            return PaymentTransactionTypes.PAYMENT_TO_LOAN_SUCCESS_TRANSACTION_CREATION_FAILURE;
                        } else {
                            log.info("updating updatedLoanAmount -- Transaction Success -- Creating a Payment Transaction -- Failure -- Reverting Loan Amount Failure");
                            // Assumption -- Very Rare Case -- Requires Manual Intervention
                            return PaymentTransactionTypes.PAYMENT_TO_LOAN_SUCCESS_TRANSACTION_CREATION_FAILURE_REVERT_LOANAMOUNT_FAILURE;
                        }

                    }
                } else {
                    log.info("updating updatedLoanAmount -- Failure ");
                    return PaymentTransactionTypes.LOAN_REPAY_UPDATE_LOAN_AMOUNT_FAILURE;

                }
                //create a transaction record


            } else {

                log.info("Loan is not Associated  With Payer");

            }


        } else {
            log.info("Creditor has no Previous Loan ");
        }


        //Check Balance >= Transaction Amount
        if (debitAccountDetails.getAvailableBalance() >= transactionAmount) {
            log.info("debitAccountDetails.getAvailableBalance() >= transactionAmount ");
            return payTranAmountLessOrEqualToBalance(debitUser, creditUser, debitAccountDetails, creditAccountDetails, transactionAmount);

        } else {
            // Do not Allow Loan Transaction if there is already a existing Loan //Assumption
            if (debitAccountDetails.getLoanAmount() > 0.0) {
                log.info("Payment Transaction with Loan not allowed as there is Existing Loan for this user");
                return PaymentTransactionTypes.INVALID_PAYMENT_TRANSACTION_NO_LOAN_ALLOWED_EXISTING_LOAN_PRESENT;

            } else {
                log.info("payTranAmountMoreThanBalance ");
                return payTranAmountMoreThanBalance(debitUser, creditUser, debitAccountDetails, creditAccountDetails, transactionAmount);
            }
        }


    }


    public PaymentTransactionTypes payTranAmountLessOrEqualToBalance(User debitUser, User creditUser, UserAccountDetails debitAccountDetails, UserAccountDetails creditAccountDetails, Double transactionAmount) {

        //debitAccountOrignialBalance
        Double debitAccountOrignialBalance = debitAccountDetails.getAvailableBalance();
        // debit Amount from debitAccount
        Double updatedDebitBalance = debitAccountOrignialBalance - transactionAmount;
        // creditAccountOrignialBalance
        Double creditAccountOrignialBalance = creditAccountDetails.getAvailableBalance();
        // updated latest Balance = creditaccountpresentbalance + transactionAmount
        Double creditUpdatedAccountBalance = creditAccountOrignialBalance + transactionAmount;

        PaymentTransactionTypes transactionTypes = createPaymentTransaction(debitAccountOrignialBalance, updatedDebitBalance, creditAccountOrignialBalance, creditUpdatedAccountBalance, transactionAmount, debitUser, creditUser, debitAccountDetails, creditAccountDetails, TransactionTypes.FUND_TRANSFER);

        return transactionTypes;
    }


    public PaymentTransactionTypes createPaymentTransaction(Double debitAccountOrignialBalance, Double updatedDebitBalance, Double creditAccountOrignialBalance, Double creditUpdatedAccountBalance, Double transactionAmount, User debitUser, User creditUser, UserAccountDetails debitAccountDetails, UserAccountDetails creditAccountDetails, TransactionTypes transactionTypes) {


        // Debit the transaction amount from debitor with EarMarking

        log.info("debiting the amount {} from the User {} with updated Balance {} ", transactionAmount, debitUser.getUserName(), updatedDebitBalance);
        //    int debitTranStatus = accountRespository.updateBalance(debitAccountDetails.getUserid(), updatedDebitBalance, debitAccountDetails.getVersion());

        Double originalEarmarkAmount = debitAccountDetails.getEarMarkAmount();
        Double updatedEarMarkAmoount = originalEarmarkAmount + transactionAmount;

        log.info("originalEarmarkAmount {} to updatedEarMarkAmoount {} for the User {} ", originalEarmarkAmount, updatedEarMarkAmoount, debitUser.getUserName());

        int debitTranStatus = accountRespository.updateBalanceAndEarMarkAmount(debitAccountDetails.getUserid(), updatedDebitBalance, updatedEarMarkAmoount, debitAccountDetails.getVersion());

        // debit Transaction Successfull
        if (debitTranStatus == 1) {
            log.info("debiting the amount {} from the User {} with updated Balance {}  -- Transaction Success", transactionAmount, debitUser.getUserName(), updatedDebitBalance);
            log.info("Credit Account present balance is amount {}", creditAccountOrignialBalance);

            int credittranstatus = accountRespository.updateBalance(creditAccountDetails.getUserid(), creditUpdatedAccountBalance, creditAccountDetails.getVersion());
            log.info("crediting the amount {} to the User {} with updated Balance {} ", transactionAmount, creditUser.getUserName(), creditUpdatedAccountBalance);

            if (credittranstatus == 1) {
                // credit Transaction Successfull
                log.info("Transaction Success --Ear Marking and crediting the amount {} to the User {} with updated Balance {}", transactionAmount, creditUser.getUserName(), creditUpdatedAccountBalance);

                // debit and credit Success remove EarMark Success -- Create a payment Transaction record

                int transactionInsertStatus = transactionRepository.createTransaction(debitAccountDetails, transactionAmount, creditAccountDetails, transactionTypes.name());

                if (transactionInsertStatus == 1) {
                    // Transaction record Creation Successfull
                    log.info("Transaction Success --Inserting Record in to Transaction Table");


                    // Remove Debit Account  Earmark Amount
                    int removeEarMarkedAmountStatus = accountRespository.updateEarMarkAmount(debitAccountDetails.getUserid(), originalEarmarkAmount, debitAccountDetails.getVersion());
                    if (removeEarMarkedAmountStatus == 1) {
                        log.info("Removed the earmarked amount added and set back from {} to  {}", updatedEarMarkAmoount, originalEarmarkAmount);
                        log.info("Debit Success -- Credit Success and Debit Earmark  removal Success -- Proceed to Create Transaction Record");
                        return PaymentTransactionTypes.PAYMENT_TRANCTION_SUCESS;

                    } else {
                        log.info("Debit Acount Transaction Ear Mark Amount Revertion Failed after Debit Success / Credit Success / Tran Record Creation Success -- ");
                        log.info("Manual Intervention Required to remove the EarmarkedTranAmount {} for the User", updatedEarMarkAmoount - originalEarmarkAmount, debitUser.getUserName());
                        // Assumption -- this type of scenario is very very Rare - Marking this transaction as Success --Can also be enhanced to revert this transaction
                        return PaymentTransactionTypes.DEBIT_SUCCESS_CREDIT_SUCCESS_TRNRECORD_CREATE_SUCCESS_REMOVE_DEBIT_EAR_MARK_FAIURE_TRAN_SUCCESS;
                    }


                } else {
                    //  Debit/Credit Success --Transaction record Creation Failure -- Revert both Credit and Debit Transaction

                    //revert Credit transaction
                    int revertCreditTranStatus = accountRespository.updateBalance(creditAccountDetails.getUserid(), creditAccountOrignialBalance, creditAccountDetails.getVersion());
                    if (revertCreditTranStatus == 1) {
                        //  Debit/Credit Success --Transaction record Creation Failure -- Revert Debit/Credit Transaction Success
                        log.info("Credit Revertion  Success");

                        // Revert Debit Transaction
                        int revertDebitTranStatus = accountRespository.updateBalanceAndEarMarkAmount(debitAccountDetails.getUserid(), debitAccountOrignialBalance, originalEarmarkAmount, debitAccountDetails.getVersion());
                        if (revertDebitTranStatus == 1) {

                            //  Debit/Credit Success --Transaction record Creation Failure -- Revert Credit Success and Revert Debit Transaction Success
                            log.info("DDebit/Credit Success --Transaction record Creation Failure -- Revert Credit Success and Revert Debit Transaction Success");
                            return PaymentTransactionTypes.DEBIT_SUCCES_CREDIT_SUCCESS_TRAN_CREATE_FAILURE_CREDIT_REVERT_SUCCESS_DEBIT_REVERT_SUCCESS;
                        } else {

                            // //  Debit/Credit Success --Transaction record Creation Failure -- Credit Revertion Success / Debit Revertion failure

                            log.info("Debit Revertion  Faiure --  crediting the amount {} to the Debited User {} with updated Balance {} ", transactionAmount, debitUser.getUserName(), creditAccountOrignialBalance);

                            return PaymentTransactionTypes.DEBIT_SUCCESS_CREDIT_SUCCESS_TRANSACTION_CREATE_FAUIRE_CREDIT_REVERT_SUCCESS_DEBIT_REVERT_FAILURE;
                            // Manual Intervention Required to Remove the transaction EarMark Amount and Add transaction amount to the Balance
                        }


                    } else {
                        //  Debit/Credit Success --Transaction record Creation Failure -- Revert Debit Transaction Success / Revert Credit Failure
                        log.info("Credit Revertion  Failure");
                        return PaymentTransactionTypes.DEBIT_SUCCESS_CREDIT_SUCCESS_TRANSACTION_CREATE_FAUIRE_CREDIT_REVERT_FAILURE;
                        // Manual Intervention Required to remove the Debit EarMarked Amount and create a transaction record for Debit Account to Credit Account

                    }


                }


            } else {
                //debit Success -- Credit Failure

                //revert debit transaction
                log.info("Debit Revertion -- crediting the amount {} to the Debited User {} with updated Balance {}  and Earmark -- Transaction Success", transactionAmount, debitUser.getUserName(), debitAccountOrignialBalance);

                int revertDebitTranStatus = accountRespository.updateBalanceAndEarMarkAmount(debitAccountDetails.getUserid(), debitAccountOrignialBalance, originalEarmarkAmount, debitAccountDetails.getVersion());

                if (revertDebitTranStatus == 1) {

                    log.info("Debit Success /Credit Failure / Debit Revertion  Success-- crediting the amount {} to the Debited User {} with updated Balance {}", transactionAmount, debitUser.getUserName(), debitAccountOrignialBalance);
                    return PaymentTransactionTypes.DEBIT_SUCCESS_CREDIT_FAILURE;
                } else {

                    // debit revertion Failure

                    log.info("Debit Success /Credit Failure / Debit Revertion Faiure --  crediting the amount {} to the Debited User {} with updated Balance {} ", transactionAmount, debitUser.getUserName(), creditAccountOrignialBalance);

                    return PaymentTransactionTypes.DEBIT_SUCCESS_CREDIT_FAILURE_DEBIT_REVERT_FAILURE;
                    // Manual Handing Required to remove this transaction earmark Amount for the debit User and add to the balance
                }


            }


        } else { // debit Transaction Failure

            log.info("debiting the amount {} from the User {} with updated Balance {}  -- Transaction Failure", transactionAmount, debitUser.getUserName(), debitAccountOrignialBalance);

            return PaymentTransactionTypes.DEBIT_TRANFAILURE;

        }


    }

/*

    public PaymentTransactionTypes payTranAmountMoreThanBalance(User debitUser, User creditUser, UserAccountDetails debitAccountDetails, UserAccountDetails creditAccountDetails, Double transactionAmount) {

        //debitAccountOrignialBalance
        Double debitAccountOrignialBalance = debitAccountDetails.getAvailableBalance();
        // debit Account Original LoanAmount
        Double debitAccountOriginalLoanAmount = debitAccountDetails.getLoanAmount();
        // debit Amount from debitAccount
        Double creditAccountOrignialBalance = creditAccountDetails.getAvailableBalance();
        // updated latest Balance = creditaccountpresentbalance + transactionAmount

        Double updatedDebitBalance = 0.0;
        // updated latest Balance = creditaccountpresentbalance + transactionAmount

        Double updatedTransactionAmount = debitAccountDetails.getAvailableBalance();

        Double DebitLoanBalanceToCreditor = Math.abs(debitAccountOrignialBalance - transactionAmount);
        // creditAccountOrignialBalance

        Double creditUpdatedAccountBalance = creditAccountOrignialBalance + debitAccountOrignialBalance;


        // create a Loan Transaction

        PaymentTransactionTypes loanTransactionTypes = createLoanTransaction(debitUser, creditUser, debitAccountDetails, creditAccountDetails, DebitLoanBalanceToCreditor);

        if (loanTransactionTypes.equals(PaymentTransactionTypes.LOAN_TRANSACTION_SUCCESS)) {
            log.info("Loan Transaction Success -- Creating the Payment Transaction");

            PaymentTransactionTypes payTransactionType = createPaymentTransaction(debitAccountOrignialBalance, updatedDebitBalance, creditAccountOrignialBalance, creditUpdatedAccountBalance, updatedTransactionAmount, debitUser, creditUser, debitAccountDetails, creditAccountDetails, TransactionTypes.FUND_TRANSFER);

            if (payTransactionType.equals(PaymentTransactionTypes.PAYMENT_TRANCTION_SUCESS)) {
                log.info("Loan Transaction and Payment Transaction Success");
                this.tranferedAmount = updatedTransactionAmount;
                return PaymentTransactionTypes.LOAN_AND_PAYMNET_SUCCESS;
            } else {
                log.info("Loan Transaction Success and Payment Transaction Failure -- Reverting Loan Transaction");

                int loanAmtReverStat = accountRespository.updateLoanAmountAndLoanRepayment(debitUser.getId(), debitAccountOriginalLoanAmount,"FALSE", debitAccountDetails.getVersion());

                if (loanAmtReverStat == 1) {
                    // Loan Transaction Success and Payment Transaction Failure  // Reverting Loan Transaction --- Success
                    log.info("Reverting Loan Transaction --- Success");

                    return PaymentTransactionTypes.LOAN_SUCCESS_PAYMNET_FAILURE;

                } else {
                    // Loan Transaction Success and Payment Transaction Failure  // Reverting Loan Amount Transaction --- Failure
                    // Needs Admin Handling
                    log.info("Loan Transaction Success and Payment Transaction Failure  // Reverting Loan Amount Transaction --- Failure");
                    return PaymentTransactionTypes.LOAN_SUCCESS_PAYMNET_FAILURE_REVERT_LOAN_AMT_FAILURE;

                }


            }

        } else {
            log.info("Loan Transaction Failure -- Returning Failure");
            return loanTransactionTypes;


        }


    }


*/


    public PaymentTransactionTypes payTranAmountMoreThanBalance(User debitUser, User creditUser, UserAccountDetails debitAccountDetails, UserAccountDetails creditAccountDetails, Double transactionAmount) {

        //debitAccountOrignialBalance
        Double debitAccountOrignialBalance = debitAccountDetails.getAvailableBalance();
        // debit Account Original LoanAmount
        Double debitAccountOriginalLoanAmount = debitAccountDetails.getLoanAmount();
        // debit Amount from debitAccount
        Double creditAccountOrignialBalance = creditAccountDetails.getAvailableBalance();
        // updated latest Balance = creditaccountpresentbalance + transactionAmount

        Double updatedDebitBalance = 0.0;
        // updated latest Balance = creditaccountpresentbalance + transactionAmount

        Double updatedTransactionAmount = debitAccountDetails.getAvailableBalance();

        Double DebitLoanBalanceToCreditor = Math.abs(debitAccountOrignialBalance - transactionAmount);
        // creditAccountOrignialBalance

        Double creditUpdatedAccountBalance = creditAccountOrignialBalance + debitAccountOrignialBalance;

        //  Initiate the Payment Transaction --
        PaymentTransactionTypes payTransactionType = createPaymentTransaction(debitAccountOrignialBalance, updatedDebitBalance, creditAccountOrignialBalance, creditUpdatedAccountBalance, updatedTransactionAmount, debitUser, creditUser, debitAccountDetails, creditAccountDetails, TransactionTypes.FUND_TRANSFER_WITH_LOAN);

        if (payTransactionType.equals(PaymentTransactionTypes.PAYMENT_TRANCTION_SUCESS)) {
            log.info("Payment Transaction Success");
            this.tranferedAmount = updatedTransactionAmount;
            //  Payment Transaction Success -- Initiate the Loan Transaction

            PaymentTransactionTypes loanTransactionTypes = createLoanTransaction(debitUser, creditUser, debitAccountDetails, creditAccountDetails, DebitLoanBalanceToCreditor);
            if (loanTransactionTypes.equals(PaymentTransactionTypes.LOAN_TRANSACTION_SUCCESS)) {
                log.info("Payment Transaction Success --Loan Transaction Success -");
                return PaymentTransactionTypes.PAYMNET_AND_LOAN_SUCCESS;
            } else {
                log.info("Payment Transaction Success --Loan Transaction Failure -");
                return PaymentTransactionTypes.PAYMENT_INITIATED_WITH_LOAN_AND_PAYMENT_SUCCESS_LOAN_FAILURE;
            }

        } else {
            log.info("payTranAmountMoreThanBalance -- Payment Transaction Failure");
            // Payment Failed -- Reverting Failure
            return payTransactionType;
        }


    }


    public PaymentTransactionTypes createLoanTransaction(User debitUser, User creditUser, UserAccountDetails debitAccountDetails, UserAccountDetails creditAccountDetails, Double loanAmount) {

        if (debitAccountDetails.getIsLoanRepayMentAllowed().equalsIgnoreCase("FALSE")) {
            log.info("Loan Repayment Not Allowed for the User as his getIsLoanRepayMentAllowed is {}", debitAccountDetails.getIsLoanRepayMentAllowed());
            return PaymentTransactionTypes.LOAN_REPAYMENT_NOT_ALLOWED_FOR_USER;
        }
        Double originalLoanAmountforDebitUser = debitAccountDetails.getLoanAmount();
        Double updatedLoanAmount = originalLoanAmountforDebitUser + loanAmount;
        log.info("Orignal Loan amount {} for the user", debitAccountDetails.getLoanAmount(), debitUser.getUserName());
        // update the Loan Amount
        log.info("Updating the Loan amount {} for the user", loanAmount, debitUser.getUserName());
        int updateLoanAmtStatus = accountRespository.updateLoanAmountAndLoanRepayment(debitUser.getId(), loanAmount, "FALSE", debitAccountDetails.getVersion());
        if (updateLoanAmtStatus == 1) {
            log.info(" Transaction Success -- Updating the Loan amount {} for the user", loanAmount, debitUser.getUserName());
            // update LoanAmount Success -- create Transaction Record for loan transaction
            int transactionInsertStatus = transactionRepository.createTransaction(debitAccountDetails, updatedLoanAmount, creditAccountDetails, TransactionTypes.PAYMENT_INITIATED_LOAN.name());
            if (transactionInsertStatus == 1) {
                // Loan upadate Success and Loan Transaction Creation is Success
                //release Loan Lock after Loan upadate Success and Loan Transaction Creation is Success
                int updatelockreleaesstatus = accountRespository.updateisLoanPaymentAllowed(debitUser.getId(), "TRUE", debitAccountDetails.getVersion());
                if (updatelockreleaesstatus == 1) {
                    log.info("release Loan Lock after Loan upadate Success and Loan Transaction Creation is Success");
                    return PaymentTransactionTypes.LOAN_TRANSACTION_SUCCESS;
                } else {
                    log.info("release Loan Lock after Loan upadate Success and Loan Transaction Creation is Failure");
                    return PaymentTransactionTypes.LOAN_UPDATE_SUCCESS_TRAN_CREATE_SUCCESS_LOAN_LOCK_REVERT_FAILURE;
                    // Manual Intervention required to release Loan lock
                }
            } else {
              log.info(" Loan update Success and Loan Transaction Creation is Failure");
                // Loan upadate Success and Loan Transaction Creation is Failure
                //revert Loan Amount
               int loanAmtReverStat = accountRespository.updateLoanAmountAndLoanRepayment(debitUser.getId(), originalLoanAmountforDebitUser, "TRUE", debitAccountDetails.getVersion());
                if (loanAmtReverStat == 1) {
                    log.info(" LOAN_UPDATE_SUCCESS_TRAN_INS_FAILURE_LOAN_REVERT_SUCCESS");
                    return PaymentTransactionTypes.LOAN_UPDATE_SUCCESS_TRAN_INS_FAILURE_LOAN_REVERT_SUCCESS;
                } else {
                    //Loan update Success tran insertion failure -- loanrevertion Failure
                    log.info(" LOAN_UPDATE_SUCCESS_TRAN_INS_FAILURE_LOAN_REVERT_FAILURE");
                    return PaymentTransactionTypes.LOAN_UPDATE_SUCCESS_TRAN_INS_FAILURE_LOAN_REVERT_FAILURE;
                    // Manual Intervention required to release the Loan Lock for the user and update the loan amount back to original loan amount
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
