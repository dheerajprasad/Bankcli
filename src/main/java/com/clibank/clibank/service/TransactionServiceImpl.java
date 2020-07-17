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
public class TransactionServiceImpl implements  TransactionService{
    @Autowired
    UserRepositoryimpl userRepository;

    @Autowired
    AccountRespository accountRespository;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    LoanRespository loanRespository;

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


}
