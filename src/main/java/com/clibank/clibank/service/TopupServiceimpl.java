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
public class TopupServiceimpl implements TopupService {

    @Autowired
    UserRepositoryimpl userRepository;

    @Autowired
    AccountRespository accountRespository;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    LoanRespository loanRespository;

    @Autowired
    UserServiceImpl userService;

    @Autowired
    private TransactionService transactionService;


    public PaymentTransactionTypes topUpTransaction(User topupUser, Double topupAmount) {

        // Toup up the Account -- Source Pool Account --

        UserAccountDetails topupUserAccntDtls = accountRespository.getUserAccountDetails(topupUser.getId());
        UserLoanDetails topupUserLoanDetails = loanRespository.getUserAccountDetails(topupUser.getId());
        UserAccountDetails poolAccntDtls = userService.getPoolAccountDetails();
        User poolAcntuser = userService.getPoolUserDetails();
        Double debitAccountOriginalBalance = poolAccntDtls.getBalance();
        Double updatedDebitBalance = debitAccountOriginalBalance - topupAmount;
        Double creditAccountOriginalBalance = topupUserAccntDtls.getBalance();
        Double creditUpdatedAccountBalance = creditAccountOriginalBalance + topupAmount;
        //Do a Topup -Payment from Pool to TopupUser


        PaymentTransactionTypes transactionTypes = transactionService.createPaymentTransaction(debitAccountOriginalBalance, updatedDebitBalance, creditAccountOriginalBalance, creditUpdatedAccountBalance, topupAmount, poolAcntuser, topupUser, poolAccntDtls, topupUserAccntDtls, TransactionTypes.TOPUP);

        if (transactionTypes.equals(PaymentTransactionTypes.PAYMENT_TRANCTION_SUCESS)) {

            topupUserAccntDtls = accountRespository.getUserAccountDetails(topupUser.getId());

            log.info("Top up for the User is Successful Balance is " + topupUserAccntDtls.getBalance());
            // Check Loan Transaction Present

            log.info("Checking Loan Transaction Present for the User {} and amount is {}", topupUser.getUserName());

            if (topupUserLoanDetails != null && topupUserLoanDetails.getBalance() > 0.0) {
                log.info("Loan Transaction Present for the User {} " + topupUserLoanDetails.getBalance());
                //  Check Loan Transaction Present -YES -- Get Loan Details
                Double loanAmount = topupUserLoanDetails.getBalance();
                log.info("Loan Transaction Present for the User loanAmount -- {}", loanAmount);
                // Debit TopupUser Credit Credituser
                UserAccountDetails creditUserAccountDetails = accountRespository.getUserAccountDetails(topupUserLoanDetails.getPayToUserId());
                creditAccountOriginalBalance = creditUserAccountDetails.getBalance();
                User creditUser = userRepository.getUserByid(topupUserLoanDetails.getPayToUserId());
                Double updatedLoanAmount = 0.0;
                Double creditAccountUpdatedBalance = 0.0;
                Double transactionAmount = 0.0;
                Double originalEarmarkAmount = topupUserLoanDetails.getEarMarkAmount();
                if (topupUserAccntDtls.getBalance() >= loanAmount) {
                    // account Balance Greater than Loan -- create Payment transaction and update loan amount to 0
                    updatedDebitBalance = topupUserAccntDtls.getBalance() - loanAmount;
                    creditAccountUpdatedBalance = creditAccountOriginalBalance + loanAmount;
                    transactionAmount = loanAmount;
                    updatedLoanAmount = 0.0;
                    //debit Account -- Current User
                } else {
                    // account Balance Less than Loan -- create Payment transaction and update Balance to Zero
                    log.info("account Balance Less than Loan -- create Payment transaction and update Balance to Zero");
                    updatedDebitBalance = 0.0;
                    transactionAmount = topupUserAccntDtls.getBalance();
                    updatedLoanAmount = Math.abs(loanAmount - transactionAmount);
                    creditAccountUpdatedBalance = creditAccountOriginalBalance + topupAmount;
                }
                int loanupdateTranStatus = loanRespository.updateBalanceAndEarMarkAmount(topupUser.getId(), updatedLoanAmount, originalEarmarkAmount + transactionAmount, topupUserLoanDetails.getVersion());
                if (loanupdateTranStatus == 1) {
                    log.info("updatedDebitBalance {} , creditAccountUpdatedBalance {} ,transactionAmount {} ", updatedDebitBalance, creditAccountUpdatedBalance, transactionAmount);
                    PaymentTransactionTypes transactionTypesPayment = transactionService.createPaymentTransaction(topupUserAccntDtls.getBalance(), updatedDebitBalance, creditAccountOriginalBalance, creditAccountUpdatedBalance, transactionAmount, topupUser, creditUser, topupUserAccntDtls, creditUserAccountDetails, TransactionTypes.TOP_INITIATED_LOAN_REPAYMENT);
                    //Loan Repayment Transaction
                    if (transactionTypesPayment.equals(PaymentTransactionTypes.PAYMENT_TRANCTION_SUCESS)) {
                        // Topup  Payment Success
                        log.info("Topup  Payment Success -- Loan Repayment Success");
                        userService.setTranferedAmount(transactionAmount);
                        //update loan amount for topup User
                        int loanrevertTranStatus = loanRespository.updateEarMarkAmount(topupUser.getId(), originalEarmarkAmount, topupUserLoanDetails.getVersion());
                        if (loanrevertTranStatus == 1) {
                            log.info("Topup  Payment Success -- Loan Repayment Success -- update Loan Amount Success");
                            return PaymentTransactionTypes.TOP_UP_SUCCESS_LOANPAYMENT_SUCCESS;
                        } else {
                            log.info("Topup  Payment Success -- Loan Repayment Success -- update Loan Ear mark Amount Failure");
                            //Batch or Manual intervention required to uodate the release the ear mark amount
                            return PaymentTransactionTypes.TOP_UP_SUCCESS_LOANPAYMENT_SUCCESS_UPDATE_LOAN_AMOUNT_FAILURE;
                        }
                    } else {
                        // Topup Success -- Loan Repayment Failure
                        log.info("Topup Success -- Loan Repayment Failure");
                        // revert loan transaction
                        int loanRevertTran = loanRespository.updateBalanceAndEarMarkAmount(topupUser.getId(), loanAmount, originalEarmarkAmount, topupUserLoanDetails.getVersion());
                        if(loanRevertTran==1){
                            log.info("Topup Success -- Loan Repayment Failure -- Loan Revert Success");
                            return PaymentTransactionTypes.TOP_UP_SUCCESS_LOAN_REPAYMENT_FAILURE;
                        }else{
                            log.info("Topup Success -- Loan Repayment Failure -- Loan Revert Failure -- Manual or Batch Handling to remove the Ear mark and add back to balance");
                            return PaymentTransactionTypes.TOP_UP_SUCCESS_LOAN_REPAYMENT_FAILURE;
                        }


                    }
                } else{
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
