package com.clibank.clibank.service;

import com.clibank.clibank.constants.PaymentTransactionTypes;
import com.clibank.clibank.constants.TransactionTypes;
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

    public PaymentTransactionTypes topUpTransaction(User topupUser, Double topupAmount) {

        // Toup up the Account -- Source Pool Account --

        UserAccountDetails topupUserAccntDtls = accountRespository.getUserAccountDetails(topupUser.getId());
        UserLoanDetails topupUserLoanDetails = loanRespository.getUserAccountDetails(topupUser.getId());
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

            log.info("Top up for the User is Successful Balance is " + topupUserAccntDtls.getAvailableBalance() );
            // Check Loan Transaction Present

            log.info("Checking Loan Transaction Present for the User {} and amount is {}", topupUser.getUserName());

            if (topupUserLoanDetails != null && topupUserLoanDetails.getAvailableBalance() > 0.0) {
                log.info("Loan Transaction Present for the User {} " + topupUserLoanDetails.getAvailableBalance());
                //  Check Loan Transaction Present -YES -- Get Loan Details
                Double loanAmount = topupUserLoanDetails.getAvailableBalance();
                log.info("Loan Transaction Present for the User loanAmount -- {}", loanAmount);
                // Debit TopupUser Credit Credituser
                UserAccountDetails creditUserAccountDetails = accountRespository.getUserAccountDetails(topupUserLoanDetails.getPayToUserId());
                creditAccountOriginalBalance = creditUserAccountDetails.getAvailableBalance();
                User creditUser = userRepository.getUserByid(topupUserLoanDetails.getPayToUserId());
                Double updatedLoanAmount = 0.0;
                Double creditAccountUpdatedBalance = 0.0;
                Double transactionAmount = 0.0;
                Double originalEarmarkAmount = topupUserLoanDetails.getEarMarkAmount();
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
                PaymentTransactionTypes transactionTypesPayment = createPaymentTransaction(topupUserAccntDtls.getAvailableBalance(), updatedDebitBalance, creditAccountOriginalBalance, creditAccountUpdatedBalance, transactionAmount, topupUser, creditUser, topupUserAccntDtls, creditUserAccountDetails, TransactionTypes.TOP_INITIATED_LOAN_REPAYMENT);
                //Loan Repayment Transaction
                if (transactionTypesPayment.equals(PaymentTransactionTypes.PAYMENT_TRANCTION_SUCESS) || transactionTypes.equals(PaymentTransactionTypes.DEBIT_SUCCESS_CREDIT_SUCCESS_TRNRECORD_CREATE_SUCCESS_REMOVE_DEBIT_EAR_MARK_FAIURE_TRAN_SUCCESS)) {
                    // Topup  Payment Success
                    log.info("Topup  Payment Success -- Loan Repayment Success");
                    this.tranferedAmount=transactionAmount;
                    //update loan amount for topup User
                    //   int loanUpdateTranStatus = accountRespository.updateLoanAmountAndLoanRepayment(topupUser.getId(), updatedLoanAmount, "TRUE", topupUserAccntDtls.getVersion());
                    int loanrevertTranStatus = loanRespository.updateBalanceAndEarMarkAmount(topupUser.getId(), updatedLoanAmount, originalEarmarkAmount, topupUserLoanDetails.getVersion());
                    if (loanrevertTranStatus == 1) {
                        log.info("Topup  Payment Success -- Loan Repayment Success -- update Loan Amount Success");
                        return PaymentTransactionTypes.TOP_UP_SUCCESS_LOANPAYMENT_SUCCESS;
                    } else {
                        log.info("Topup  Payment Success -- Loan Repayment Success -- update Loan Amount Failure");
                        //Consider the above payment  transaction as normal payment from topup user to loaner transaction as as loan Payment failed
                        // Requires Batch or Manual Handling to Change the Above transaction Payment type from TOP_INITIATED_LOAN_REPAYMENT to PAYMENT
                        return PaymentTransactionTypes.TOP_UP_SUCCESS_LOANPAYMENT_SUCCESS_UPDATE_LOAN_AMOUNT_FAILURE;
                   }
                } else {
                    // Topup Success -- Loan Repayment Failure
                    log.info("Topup Success -- Loan Repayment Failure");
                    // revert loan transaction
                    return PaymentTransactionTypes.TOP_UP_SUCCESS_LOAN_REPAYMENT_FAILURE;
           }

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


        // Check whether the Creditor has Loan Associated
        UserLoanDetails creditUserLoanDetails = loanRespository.getUserAccountDetails(creditAccountDetails.getUserid());
        if (creditUserLoanDetails != null && creditUserLoanDetails.getAvailableBalance() > 0) {
            log.info("Creditor has  Previous Loan");
            log.info("Checking if the Loan is Associated  With Present payer");

            if (creditUserLoanDetails.getPayToUserId() == debitUser.getId()) {

                log.info("Payer has existing Loan with the user " + creditUserLoanDetails);

                Double originalLoanAmount = creditUserLoanDetails.getAvailableBalance();
                Double updatedLoanAmount = 0.0;
                Double OriginalTranAmount = transactionAmount;
                Double orinialEarMarkAmount = creditUserLoanDetails.getEarMarkAmount();
                if (transactionAmount <= creditUserLoanDetails.getAvailableBalance()) {
                    log.info("transactionAmount  {} <  debitUserLoanDetails.getAvailableBalance() {}", transactionAmount, creditUserLoanDetails.getAvailableBalance());
                    // update Loan amount for the debitor
                    updatedLoanAmount = originalLoanAmount - transactionAmount;
                } else {
                    log.info("transactionAmount  {} >  debitUserLoanDetails.getAvailableBalance() {}", transactionAmount, creditUserLoanDetails.getAvailableBalance());
                    updatedLoanAmount = 0.0;
                    log.info("Updating the Transaction Amount transactionAmount {} to transactionAmount- originalLoanAmount {}", transactionAmount, transactionAmount - originalLoanAmount);
                }
                log.info("updating updatedLoanAmount {}", updatedLoanAmount);
                Double updaedEarmarkAmount = orinialEarMarkAmount + (originalLoanAmount - updatedLoanAmount);
                log.info("updating updaedEarmarkAmount {}", updaedEarmarkAmount);
                //    int accountUpdateStatus = accountRespository.updateLoanAmountAndLoanRepayment(creditAccountDetails.getUserid(), updatedLoanAmount, "FALSE", creditAccountDetails.getVersion());
                int accountUpdateStatus = loanRespository.updateBalanceAndEarMarkAmount(creditUserLoanDetails.getUserid(), updatedLoanAmount, orinialEarMarkAmount + originalLoanAmount - updatedLoanAmount, creditUserLoanDetails.getVersion());
                if (accountUpdateStatus == 1) {
                    log.info("updating updatedLoanAmount -- Transaction Success");
                    // Create a Payment Record
                    debitAccountDetails = accountRespository.getUserAccountDetails(debitUser.getId());
                    Double debitAccountOriginalbalance = debitAccountDetails.getAvailableBalance();

                    // Doube transaction Amount
                    Double loanTransactionAmount = originalLoanAmount - updatedLoanAmount;
                    log.info("updating updatedLoanAmount -- Transaction Success -- Creating a Payment Transaction ");
                    int loanTransactionUpdateStatus = transactionRepository.createTransactionLoan(creditUserLoanDetails, loanTransactionAmount, TransactionTypes.PAYMENT_INITIATED_PAY_TO_LOAN.name());
                    if (loanTransactionUpdateStatus == 1) {
                        log.info("updating updatedLoanAmount -- Transaction Success -- Creating Loan Transaction Record -- Success");
                        //revert earmark
                        int loanEarMarkrevertStatus = loanRespository.updateEarMarkAmount(creditAccountDetails.getUserid(), orinialEarMarkAmount, creditAccountDetails.getVersion());
                        if (loanEarMarkrevertStatus == 1) {
                            // Loan  revertion Success
                            if (OriginalTranAmount <= originalLoanAmount) {
                                // revert the Loan Lock again
                                log.info("updating updatedLoanAmount -- Transaction Success -- Creating a  Transaction record -- Success --loanEarMarkrevertStatus--success");
                                return PaymentTransactionTypes.PAYMENT_TO_LOAN_SUCCESS;
                            } else {
                                log.info("OriginalTranAmount >  originalLoanAmountupdating :::: updatedLoanAmount -- Transaction Success -- Creating a  Transaction record -- Success --loanEarMarkrevertStatus--success");
                                // Updating the Transaction Amount to Proceed after Loan Transaction
                                log.info("Continue Flow to the Payments");
                                transactionAmount = transactionAmount - originalLoanAmount;

                            }

                        } else {

                            log.info("updating updatedLoanAmount -- Transaction Success -- Creating a  Transaction record -- Success --loanEarMarkrevertStatus--Failure");
                            // Loan Earmark revertion Failure

                            return PaymentTransactionTypes.LOAN_TRANSFER_TO_CREDITOR_SUCESS_TRAN_CREATE_SUCCESS_LOAN_EARMARK_REVERT_FAILURE;
                            // Batch or Manual Intervention required to remove the earmark amount and add it to loan amount
                        }

                    } else {
                        log.info("updating updatedLoanAmount -- Transaction Success -- Creating a  Transaction record-- Failure");

                        // revert the update LoanAmount
                        int accountRevertStatus = loanRespository.updateBalanceAndEarMarkAmount(creditUserLoanDetails.getUserid(), originalLoanAmount, orinialEarMarkAmount, creditAccountDetails.getVersion());
                        if (accountRevertStatus == 1) {
                            log.info("updating updatedLoanAmount -- Transaction Success -- Creating a  Transaction -- Failure -- Reverting Loan Amount Success");
                            return PaymentTransactionTypes.PAYMENT_TO_LOAN_SUCCESS_TRANSACTION_CREATION_FAILURE;
                        } else {
                            log.info("updating updatedLoanAmount -- Transaction Success -- Creating a  Transaction -- Failure -- Reverting Loan Amount Failure");
                            return PaymentTransactionTypes.PAYMENT_TO_LOAN_SUCCESS_TRANSACTION_CREATION_FAILURE_REVERT_LOANAMOUNT_FAILURE;
                            // Batch or Manual Intervention required to release the ear mark amount and
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
            log.info("Credit User has no loan  ");

        }

        //Check Balance >= Transaction Amount
        if (debitAccountDetails.getAvailableBalance() >= transactionAmount) {
            log.info("debitAccountDetails.getAvailableBalance() >= transactionAmount ");
            return payTranAmountLessOrEqualToBalance(debitUser, creditUser, debitAccountDetails, creditAccountDetails, transactionAmount);

        } else {
            // Do not Allow Loan Transaction if there is already a existing Loan //Assumption
            UserLoanDetails debitUserLoan =loanRespository.getUserAccountDetails(getLoggedInuser().getId());
            if ( debitUserLoan != null && debitUserLoan.getAvailableBalance() > 0.0) {
                log.info("Payment Transaction with Loan not allowed as there is Existing Loan for this user");
                return PaymentTransactionTypes.INVALID_PAYMENT_TRANSACTION_NO_LOAN_ALLOWED_EXISTING_LOAN_PRESENT;

            } else {
                log.info("payTranAmountMoreThanBalance ");
                return payTranAmountMoreThanBalance(debitUser, creditUser, debitAccountDetails, creditAccountDetails, transactionAmount);
            }
        }

    }


    public PaymentTransactionTypes payTranAmountLessOrEqualToBalance(User debitUser, User
            creditUser, UserAccountDetails debitAccountDetails, UserAccountDetails creditAccountDetails, Double
                                                                             transactionAmount) {

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


    public PaymentTransactionTypes createPaymentTransaction(Double debitAccountOrignialBalance, Double
            updatedDebitBalance, Double creditAccountOrignialBalance, Double creditUpdatedAccountBalance, Double
                                                                    transactionAmount, User debitUser, User creditUser, UserAccountDetails debitAccountDetails, UserAccountDetails
                                                                    creditAccountDetails, TransactionTypes transactionTypes) {


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


    public PaymentTransactionTypes payTranAmountMoreThanBalance(User debitUser, User creditUser, UserAccountDetails
            debitAccountDetails, UserAccountDetails creditAccountDetails, Double transactionAmount) {


        UserLoanDetails userLoanDetails = checkLoanAccountExistsElseCreate(debitUser, creditUser);
        if (userLoanDetails == null) {
            return PaymentTransactionTypes.LOAN_ACCOUNT_CREATION_FAILURE;
        }
        //debitAccountOrignialBalance
        Double debitAccountOrignialBalance = debitAccountDetails.getAvailableBalance();
        // debit Account Original LoanAmount
        Double debitAccountOriginalLoanAmount = userLoanDetails.getAvailableBalance();
        // debit Amount from debitAccount
        Double creditAccountOrignialBalance = creditAccountDetails.getAvailableBalance();
        // updated latest Balance = creditaccountpresentbalance + transactionAmount

        Double updatedDebitBalance = 0.0;
        // updated latest Balance = creditaccountpresentbalance + transactionAmount

        Double updatedTransactionAmount = debitAccountDetails.getAvailableBalance();

        Double DebitLoanBalanceToCreditor = debitAccountOriginalLoanAmount + Math.abs(debitAccountOrignialBalance - transactionAmount);
        // creditAccountOrignialBalance

        Double creditUpdatedAccountBalance = creditAccountOrignialBalance + debitAccountOrignialBalance;

        //  Initiate the Payment Transaction --
        PaymentTransactionTypes payTransactionType = createPaymentTransaction(debitAccountOrignialBalance, updatedDebitBalance, creditAccountOrignialBalance, creditUpdatedAccountBalance, updatedTransactionAmount, debitUser, creditUser, debitAccountDetails, creditAccountDetails, TransactionTypes.FUND_TRANSFER_WITH_LOAN);

        if (payTransactionType.equals(PaymentTransactionTypes.PAYMENT_TRANCTION_SUCESS)) {
            log.info("Payment Transaction Success");
            this.tranferedAmount = updatedTransactionAmount;
            //  Payment Transaction Success -- Initiate the Loan Transaction

            PaymentTransactionTypes loanTransactionTypes = createLoanTransaction(userLoanDetails, DebitLoanBalanceToCreditor);
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

    public UserLoanDetails checkLoanAccountExistsElseCreate(User debitUser, User creditUser) {

        log.info("Check if the loan Record exist for the User ");
        UserLoanDetails loanDetails = loanRespository.getUserAccountDetails(debitUser.getId());
        if (loanDetails != null && loanDetails.getPayToUserId() == creditUser.getId()) {
            // Loan Record Already Exist sending Existing Data Back
            log.info("Loan Record Already Exist sending Existing Data Back {} ", loanDetails);
            return loanDetails;
        } else {
            int loanAcountCreationStatus = loanRespository.createAccount(debitUser.getId(), debitUser.getUserName() + "Loan", creditUser.getId());
            if (loanAcountCreationStatus == 1) {
                //
                log.info("Loan Record Creation Success");
                loanDetails = loanRespository.getUserAccountDetails(debitUser.getId());
                return loanDetails;
            } else {
                return null;
            }
        }

    }

    @Transactional
    public PaymentTransactionTypes createLoanTransaction(UserLoanDetails userLoanDetails, Double loanAmount) {

        Double originalLoanAmountforDebitUser = userLoanDetails.getAvailableBalance();
        User debituser = userRepository.getUserByid(userLoanDetails.getUserid());
        Double originalEarMarkLoanAmount = userLoanDetails.getEarMarkAmount();
        Double updatedLoanAmount = originalLoanAmountforDebitUser + loanAmount;
        log.info("Orignal Loan amount {} for the user", originalLoanAmountforDebitUser, debituser.getUserName());
        // update the Loan Amount
        //  int updateLoanAmtStatus = accountRespository.updateLoanAmountAndLoanRepayment(debitUser.getId(), loanAmount, "FALSE", debitAccountDetails.getVersion());
        int updateLoanAmtStatus = loanRespository.updateBalanceAndEarMarkAmount(userLoanDetails.getUserid(), updatedLoanAmount, originalEarMarkLoanAmount + loanAmount, userLoanDetails.getVersion());
        if (updateLoanAmtStatus == 1) {
            log.info(" Transaction Success -- Updating the Loan amount {} for the user", loanAmount, debituser.getUserName());
            // update LoanAmount Success -- create Transaction Record for loan transaction
            int transactionInsertStatus = transactionRepository.createTransactionLoan(userLoanDetails, loanAmount, TransactionTypes.PAYMENT_INITIATED_LOAN.name());
            if (transactionInsertStatus == 1) {
                // Loan upadate Success and Loan Transaction Creation is Success
                //release Loan earmark after Loan upadate Success and Loan Transaction Creation is Success
                int revertEarMarkAmount = loanRespository.updateEarMarkAmount(userLoanDetails.getUserid(), originalEarMarkLoanAmount, userLoanDetails.getVersion());
                if (revertEarMarkAmount == 1) {
                    log.info("release Loan Lock after Loan upadate Success and Loan Transaction Creation is Success");
                    return PaymentTransactionTypes.LOAN_TRANSACTION_SUCCESS;
                } else {
                    log.info("release Loan Lock after Loan upadate Success and Loan Transaction Creation is Failure");
                    return PaymentTransactionTypes.LOAN_UPDATE_SUCCESS_TRAN_CREATE_SUCCESS_LOAN_EARMARK_REVERT_FAILURE;
                    // Batch or Manual Intervention required to release EarMark Amount
                }
            } else {
                log.info(" Loan update Success and Loan Transaction Creation is Failure");
                // Loan upadate Success and Loan Transaction Creation is Failure
                //revert Loan Amount
                int loanAmtReverStat = loanRespository.updateBalanceAndEarMarkAmount(userLoanDetails.getUserid(), originalLoanAmountforDebitUser, originalEarMarkLoanAmount, userLoanDetails.getVersion());
                if (loanAmtReverStat == 1) {
                    log.info(" LOAN_UPDATE_SUCCESS_TRAN_INS_FAILURE_LOAN_REVERT_SUCCESS");
                    return PaymentTransactionTypes.LOAN_UPDATE_SUCCESS_TRAN_INS_FAILURE_LOAN_REVERT_SUCCESS;
                } else {
                    //Loan update Success tran insertion failure -- loanrevertion Failure
                    log.info(" LOAN_UPDATE_SUCCESS_TRAN_INS_FAILURE_LOAN_REVERT_FAILURE");
                    return PaymentTransactionTypes.LOAN_UPDATE_SUCCESS_TRAN_INS_FAILURE_LOAN_REVERT_FAILURE;
                    // Batch or Manual Intervention required to release the Loan earmark for the loan product and add to loan amount
                }


            }


        } else {
            log.info(" Transaction Failure -- Updating the Loan amount {} for the user", loanAmount, debituser.getUserName());

            return PaymentTransactionTypes.LOAN_UPDATE_AMOUNT_FAIURE;

        }

    }


    private String generateAccountNumber() {

        return UUID.randomUUID().toString();
    }

}
