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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class TransactionServiceImpl implements TransactionService {
    @Autowired
    UserRepositoryimpl userRepository;

    @Autowired
    AccountRespository accountRespository;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    LoanRespository loanRespository;

    @Transactional
    public PaymentTransactionTypes createPaymentTransaction(Double debitAccountOrignialBalance, Double
            updatedDebitBalance, Double creditAccountOrignialBalance, Double creditUpdatedAccountBalance, Double
                                                                    transactionAmount, User debitUser, User creditUser, UserAccountDetails debitAccountDetails, UserAccountDetails
                                                                    creditAccountDetails, TransactionTypes transactionTypes) {
        boolean isException = false;
        try {
            // Debit the transaction amount from debitor with EarMarking
            log.info("debiting the amount {} from the User {} with updated Balance {} ", transactionAmount, debitUser.getUserName(), updatedDebitBalance);
            //    int debitTranStatus = accountRespository.updateBalance(debitAccountDetails.getUserid(), updatedDebitBalance, debitAccountDetails.getVersion());

            Double originalEarmarkAmount = debitAccountDetails.getEarMarkAmount();
            Double updatedEarMarkAmoount = originalEarmarkAmount + transactionAmount;

            log.info("originalEarmarkAmount {} to updatedEarMarkAmoount {} for the User {} ", originalEarmarkAmount, updatedEarMarkAmoount, debitUser.getUserName());

            accountRespository.updateBalanceAndEarMarkAmount(debitAccountDetails.getUserid(), updatedDebitBalance, updatedEarMarkAmoount, debitAccountDetails.getVersion());

            accountRespository.updateBalance(creditAccountDetails.getUserid(), creditUpdatedAccountBalance, creditAccountDetails.getVersion());
            log.info("crediting the amount {} to the User {} with updated Balance {} ", transactionAmount, creditUser.getUserName(), creditUpdatedAccountBalance);

            transactionRepository.createTransaction(debitAccountDetails, transactionAmount, creditAccountDetails, transactionTypes.name());

            accountRespository.updateEarMarkAmount(debitAccountDetails.getUserid(), originalEarmarkAmount, debitAccountDetails.getVersion());


        } catch (Exception e) {
            log.info("crediting the amcreatePaymentTransaction {} ", e);
            isException = true;
        } finally {
            if (isException) {
                return PaymentTransactionTypes.PAYMENT_TRANCTION_FAILURE;

            } else {
                return PaymentTransactionTypes.PAYMENT_TRANCTION_SUCESS;
            }

        }


    }

    @Transactional
    public PaymentTransactionTypes createLoanTransaction(UserLoanDetails userLoanDetails, Double loanAmount) {
        boolean isException = false;
        try {
            Double originalLoanAmountforDebitUser = userLoanDetails.getAvailableBalance();
            User debituser = userRepository.getUserByid(userLoanDetails.getUserid());
            Double originalEarMarkLoanAmount = userLoanDetails.getEarMarkAmount();
            Double updatedLoanAmount = originalLoanAmountforDebitUser + loanAmount;
            log.info("Orignal Loan amount {} for the user", originalLoanAmountforDebitUser, debituser.getUserName());
            // update the Loan Amount
            //  int updateLoanAmtStatus = accountRespository.updateLoanAmountAndLoanRepayment(debitUser.getId(), loanAmount, "FALSE", debitAccountDetails.getVersion());
            int updateLoanAmtStatus = loanRespository.updateBalanceAndEarMarkAmount(userLoanDetails.getUserid(), updatedLoanAmount, originalEarMarkLoanAmount + loanAmount, userLoanDetails.getVersion());

            log.info(" Transaction Success -- Updating the Loan amount {} for the user", loanAmount, debituser.getUserName());
            // update LoanAmount Success -- create Transaction Record for loan transaction
            int transactionInsertStatus = transactionRepository.createTransactionLoan(userLoanDetails, loanAmount, TransactionTypes.PAYMENT_INITIATED_LOAN.name());
            // Loan upadate Success and Loan Transaction Creation is Success
            //release Loan earmark after Loan upadate Success and Loan Transaction Creation is Success
            int revertEarMarkAmount = loanRespository.updateEarMarkAmount(userLoanDetails.getUserid(), originalEarMarkLoanAmount, userLoanDetails.getVersion());

        } catch (Exception e) {
            log.info("Exception Occured  in createLoanTransaction {} ", e);
            isException = true;

        } finally {

            if (isException) {
                return PaymentTransactionTypes.LOAN_TRAN_FAILURE;
            } else {
                return PaymentTransactionTypes.LOAN_TRANSACTION_SUCCESS;
            }


        }


    }
}
