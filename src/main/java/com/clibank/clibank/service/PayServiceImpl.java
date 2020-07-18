package com.clibank.clibank.service;

import com.clibank.clibank.constants.PaymentTransactionTypes;
import com.clibank.clibank.constants.TransactionTypes;
import com.clibank.clibank.model.ClearExistingLoanRes;
import com.clibank.clibank.model.User;
import com.clibank.clibank.model.UserAccountDetails;
import com.clibank.clibank.model.UserLoanDetails;
import com.clibank.clibank.repository.AccountRespository;
import com.clibank.clibank.repository.LoanRespository;
import com.clibank.clibank.repository.TransactionRepository;
import com.clibank.clibank.repository.UserRepositoryimpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class PayServiceImpl implements PayService {


    @Autowired
    UserRepositoryimpl userRepository;

    @Autowired
    AccountRespository accountRespository;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    LoanRespository loanRespository;

    @Autowired
    UserService userService;

    @Autowired
    private TransactionService transactionService;


    public PaymentTransactionTypes pay(UserAccountDetails creditAccountDetails, Double transactionAmount) {

        User debitUser = userService.getLoggedInuser();
        UserAccountDetails debitAccountDetails = accountRespository.getUserAccountDetails(debitUser.getId());
        User creditUser = userRepository.getUserByid(creditAccountDetails.getUserid());

        // Do not Allow Payment if Balance is Zero //Assumption
        if (debitAccountDetails.getAvailableBalance() <= 0.0) {
            log.info("debitAccount Balance Less than 0 ");
            return PaymentTransactionTypes.INVALID_PAYMENT_TRANSACTION_NO_DEBIT_BALANCE;
        }

        ClearExistingLoanRes clearExistingLoanRes = clearExistingLoan(debitUser, debitAccountDetails, creditUser, creditAccountDetails, transactionAmount);
        PaymentTransactionTypes paymentTransactionTypes = clearExistingLoanRes.getPaymentTransactionTypes();

        // only Loan Payment Exists for user
        if ((paymentTransactionTypes.equals(PaymentTransactionTypes.PAYMENT_TO_LOAN_FAILURE))) {
            return PaymentTransactionTypes.PAYMENT_TO_LOAN_FAILURE;
        }
        if((paymentTransactionTypes.equals(PaymentTransactionTypes.PAYMENT_TO_LOAN_ONLY_SUCCESS))){
            return PaymentTransactionTypes.PAYMENT_TO_LOAN_SUCCESS;
        }
        // Loan and Extra Payment Required
        if (paymentTransactionTypes.equals(PaymentTransactionTypes.PAYMENT_TO_LOAN_SUCCESS)) {
            transactionAmount = clearExistingLoanRes.getRemainingTranamt();
        }

        //Check Balance >= Transaction Amount
        if (debitAccountDetails.getAvailableBalance() >= transactionAmount) {
            log.info("debitAccountDetails.getAvailableBalance() >= transactionAmount ");
            return payTranAmountLessOrEqualToBalance(debitUser, creditUser, debitAccountDetails, creditAccountDetails, transactionAmount);

        } else {
            // Do not Allow Loan Transaction if there is already a existing Loan //Assumption
            UserLoanDetails debitUserLoan = loanRespository.getUserAccountDetails(userService.getLoggedInuser().getId());
            if (debitUserLoan != null && debitUserLoan.getAvailableBalance() > 0.0) {
                log.info("Payment Transaction with Loan not allowed as there is Existing Loan for this user");
                return PaymentTransactionTypes.INVALID_PAYMENT_TRANSACTION_NO_LOAN_ALLOWED_EXISTING_LOAN_PRESENT;

            } else {
                log.info("payTranAmountMoreThanBalance ");
                return payTranAmountMoreThanBalance(debitUser, creditUser, debitAccountDetails, creditAccountDetails, transactionAmount);
            }
        }

    }


    @Transactional
    public ClearExistingLoanRes  clearExistingLoan(User debitUser, UserAccountDetails debitAccountDetails, User creditUser, UserAccountDetails creditAccountDetails, Double transactionAmount) {

        ClearExistingLoanRes clearExistingLoanRes = new ClearExistingLoanRes();
        PaymentTransactionTypes paymentTransactionTypes = null;
        boolean isException = false;
        try {
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
                    loanRespository.updateBalanceAndEarMarkAmount(creditUserLoanDetails.getUserid(), updatedLoanAmount, orinialEarMarkAmount + originalLoanAmount - updatedLoanAmount, creditUserLoanDetails.getVersion());

                    log.info("updating updatedLoanAmount -- Transaction Success");
                    // Create a Payment Record
                    debitAccountDetails = accountRespository.getUserAccountDetails(debitUser.getId());
                    Double debitAccountOriginalbalance = debitAccountDetails.getAvailableBalance();

                    // Doube transaction Amount
                    Double loanTransactionAmount = originalLoanAmount - updatedLoanAmount;
                    log.info("updating updatedLoanAmount -- Transaction Success -- Creating a Payment Transaction ");
                    transactionRepository.createTransactionLoan(creditUserLoanDetails, loanTransactionAmount, TransactionTypes.PAYMENT_INITIATED_PAY_TO_LOAN.name());

                    log.info("updating updatedLoanAmount -- Transaction Success -- Creating Loan Transaction Record -- Success");
                    //revert earmark
                    loanRespository.updateEarMarkAmount(creditAccountDetails.getUserid(), orinialEarMarkAmount, creditAccountDetails.getVersion());
                    // Loan  revertion Success
                    if (OriginalTranAmount <= originalLoanAmount) {
                        // revert the Loan Lock again
                        log.info("updating updatedLoanAmount -- Transaction Success -- Creating a  Transaction record -- Success --loanEarMarkrevertStatus--success");
                        paymentTransactionTypes = PaymentTransactionTypes.PAYMENT_TO_LOAN_ONLY_SUCCESS;
                    } else {
                        log.info("OriginalTranAmount >  originalLoanAmountupdating :::: updatedLoanAmount -- Transaction Success -- Creating a  Transaction record -- Success --loanEarMarkrevertStatus--success");
                        // Updating the Transaction Amount to Proceed after Loan Transaction
                        log.info("Continue Flow to the Payments");
                        transactionAmount = transactionAmount - originalLoanAmount;
                        paymentTransactionTypes = PaymentTransactionTypes.PAYMENT_TO_LOAN_SUCCESS;
                    }

                } else {
                    log.info("Loan is not Associated  With Payer");
                    paymentTransactionTypes = PaymentTransactionTypes.NO_LOAN_ASSOCIATED;
                }
            } else {
                log.info("Credit User has no loan  ");
                paymentTransactionTypes = PaymentTransactionTypes.NO_LOAN_ASSOCIATED;

            }
        } catch (Exception e) {
            log.info("clearExistingLoan Exception  ", e);
            isException = true;
        } finally {
            if (isException) {
                clearExistingLoanRes.setPaymentTransactionTypes(PaymentTransactionTypes.PAYMENT_TO_LOAN_FAILURE);
                return clearExistingLoanRes;

            } else {
                clearExistingLoanRes.setPaymentTransactionTypes(paymentTransactionTypes);
                clearExistingLoanRes.setRemainingTranamt(transactionAmount);
                return clearExistingLoanRes;
            }

        }
    }

    @Transactional
    public PaymentTransactionTypes payTranAmountMoreThanBalance(User debitUser, User
            creditUser, UserAccountDetails
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
        PaymentTransactionTypes payTransactionType = transactionService.createPaymentTransaction(debitAccountOrignialBalance, updatedDebitBalance, creditAccountOrignialBalance, creditUpdatedAccountBalance, updatedTransactionAmount, debitUser, creditUser, debitAccountDetails, creditAccountDetails, TransactionTypes.FUND_TRANSFER_WITH_LOAN);

        if (payTransactionType.equals(PaymentTransactionTypes.PAYMENT_TRANCTION_SUCESS)) {
            log.info("Payment Transaction Success");
            userService.setTranferedAmount(updatedTransactionAmount);
            //  Payment Transaction Success -- Initiate the Loan Transaction

            PaymentTransactionTypes loanTransactionTypes = transactionService.createLoanTransaction(userLoanDetails, DebitLoanBalanceToCreditor);
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

        PaymentTransactionTypes transactionTypes = transactionService.createPaymentTransaction(debitAccountOrignialBalance, updatedDebitBalance, creditAccountOrignialBalance, creditUpdatedAccountBalance, transactionAmount, debitUser, creditUser, debitAccountDetails, creditAccountDetails, TransactionTypes.FUND_TRANSFER);

        return transactionTypes;
    }

}
