package com.clibank.clibank.service;

import com.clibank.clibank.constants.PaymentTransactionTypes;
import com.clibank.clibank.constants.TransactionTypes;
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

@Slf4j
@Service
public class TopupServiceimpl implements TopupService{

    @Autowired
    UserRepositoryimpl userRepository;

    @Autowired
    AccountRespository accountRespository;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    LoanRespository loanRespository;

    @Autowired UserService userService;


    public PaymentTransactionTypes topUpTransaction(User topupUser, Double topupAmount) {

        // Toup up the Account -- Source Pool Account --

        UserAccountDetails topupUserAccntDtls = accountRespository.getUserAccountDetails(topupUser.getId());
        UserLoanDetails topupUserLoanDetails = loanRespository.getUserAccountDetails(topupUser.getId());
        UserAccountDetails poolAccntDtls = userService.getPoolAccountDetails();
        User poolAcntuser = userService.getPoolUserDetails();
        Double debitAccountOriginalBalance = poolAccntDtls.getAvailableBalance();
        Double updatedDebitBalance = debitAccountOriginalBalance - topupAmount;
        Double creditAccountOriginalBalance = topupUserAccntDtls.getAvailableBalance();
        Double creditUpdatedAccountBalance = creditAccountOriginalBalance + topupAmount;
        //Do a Topup -Payment from Pool to TopupUser


        PaymentTransactionTypes transactionTypes = userService.createPaymentTransaction(debitAccountOriginalBalance, updatedDebitBalance, creditAccountOriginalBalance, creditUpdatedAccountBalance, topupAmount, poolAcntuser, topupUser, poolAccntDtls, topupUserAccntDtls, TransactionTypes.TOPUP);

        if (transactionTypes.equals(PaymentTransactionTypes.PAYMENT_TRANCTION_SUCESS) || transactionTypes.equals(PaymentTransactionTypes.DEBIT_SUCCESS_CREDIT_SUCCESS_TRNRECORD_CREATE_SUCCESS_REMOVE_DEBIT_EAR_MARK_FAIURE_TRAN_SUCCESS)) {

            topupUserAccntDtls = accountRespository.getUserAccountDetails(topupUser.getId());

            log.info("Top up for the User is Successful Balance is " + topupUserAccntDtls.getAvailableBalance());
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
                PaymentTransactionTypes transactionTypesPayment = userService.createPaymentTransaction(topupUserAccntDtls.getAvailableBalance(), updatedDebitBalance, creditAccountOriginalBalance, creditAccountUpdatedBalance, transactionAmount, topupUser, creditUser, topupUserAccntDtls, creditUserAccountDetails, TransactionTypes.TOP_INITIATED_LOAN_REPAYMENT);
                //Loan Repayment Transaction
                if (transactionTypesPayment.equals(PaymentTransactionTypes.PAYMENT_TRANCTION_SUCESS) || transactionTypes.equals(PaymentTransactionTypes.DEBIT_SUCCESS_CREDIT_SUCCESS_TRNRECORD_CREATE_SUCCESS_REMOVE_DEBIT_EAR_MARK_FAIURE_TRAN_SUCCESS)) {
                    // Topup  Payment Success
                    log.info("Topup  Payment Success -- Loan Repayment Success");
                    userService.setTranferedAmount(transactionAmount);
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



}
