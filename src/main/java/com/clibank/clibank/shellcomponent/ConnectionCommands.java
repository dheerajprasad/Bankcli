package com.clibank.clibank.shellcomponent;


import com.clibank.clibank.constants.PaymentTransactionTypes;
import com.clibank.clibank.constants.TransactionTypes;
import com.clibank.clibank.model.TransactionDetails;
import com.clibank.clibank.model.User;
import com.clibank.clibank.model.UserAccountDetails;
import com.clibank.clibank.service.ConsoleService;
import com.clibank.clibank.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.util.StringUtils;
@Slf4j
@ShellComponent
public class ConnectionCommands {

    @Autowired
    private UserService userService;

    @Autowired
    private ConsoleService consoleService;


    @ShellMethod("Login to the Banking Cli")
    public void login(String userName) {
        User user = userService.checkUserExistsElseCreateuser(userName);
        consoleService.write("Hello , %s !", user.getUserName());
        consoleService.write("Your Balance is   %s ", userService.getAccountBalance(user.getId()) + "");
        printOwingAmount();
        printLoanAmount();

    }

    @ShellMethod("Top up")
    public void topup(Double topupAmount) {

        try {
            User user = userService.getLoggedInuser();
            PaymentTransactionTypes transactionTypes = userService.topUpTransaction(user, topupAmount);
            if (transactionTypes.equals(PaymentTransactionTypes.TOP_UP_SUCCESS)) {
                printBalanceAmount();
                printLoanAmount();

            } else if (transactionTypes.equals(PaymentTransactionTypes.TOP_UP_SUCCESS_LOANPAYMENT_SUCCESS)) {
                printBalanceAmount();
                printLoanAmount();
            } else if (transactionTypes.equals(PaymentTransactionTypes.TOP_UP_SUCCESS_LOAN_REPAYMENT_FAILURE)) {
                consoleService.write("Your Topup SuccessFull - Loan Payment Failure", user.getUserName());
                printBalanceAmount();
                printLoanAmount();
            } else if (transactionTypes.equals(PaymentTransactionTypes.TOP_UP_SUCCESS_LOANPAYMENT_SUCCESS_UPDATE_LOAN_AMOUNT_FAILURE)) {
                consoleService.write("Your Topup SuccessFull - Loan Payment Failure", user.getUserName());
                printBalanceAmount();
                printLoanAmount();
            } else {
                consoleService.write("Toup  ");

            }
        } catch (Exception e) {
            consoleService.write("Some Thing Went wrong Please retry "  );
            log.info("Exception "+e);
            printBalanceAmount();
            return;
        }


    }

    @ShellMethod("Pay")
    public void Pay(String creditor, Double transactionAmount) {

        try {

            if (StringUtils.hasText(creditor)) {
                if (transactionAmount <= 0) {
                    consoleService.write("Enter Valid Amount Greater than 0");
                    return;
                }
                User credUser = userService.getUserDetails(creditor);
                if (credUser == null) {
                    consoleService.write("Creditor Not Available in System -- Please Refer to Onboard in the Bank");
                    return;
                }
                if (credUser.getId() == userService.getLoggedInuser().getId()) {
                    consoleService.write("Cannot Pay to the Same User");
                    return;

                }

                UserAccountDetails credUserAccountDetails = userService.getAccountDetails(credUser.getId());

                if (credUserAccountDetails == null) {
                    consoleService.write("Creditor Not Available in System ");
                    return;
                }

                PaymentTransactionTypes transactionTypes = userService.pay(credUserAccountDetails, transactionAmount);

                if (transactionTypes.equals(PaymentTransactionTypes.PAYMENT_TRANCTION_SUCESS) || transactionTypes.equals(PaymentTransactionTypes.DEBIT_SUCCESS_CREDIT_SUCCESS_TRNRECORD_CREATE_SUCCESS_REMOVE_DEBIT_EAR_MARK_FAIURE_TRAN_SUCCESS)) {
                    consoleService.write("Transferred  " + transactionAmount + " to " + credUser.getUserName());
                    printBalanceAmount();
                    printLoanAmount();
                    printOwingAmount();
                    return;


                } else if (transactionTypes.equals(PaymentTransactionTypes.LOAN_AND_PAYMNET_SUCCESS)) {

                    consoleService.write("Transferred  " + userService.getTransferedAmount() + " to " + credUser.getUserName());
                    printBalanceAmount();
                    printLoanAmount();
                    printOwingAmount();
                    return;

                } else if (transactionTypes.equals(PaymentTransactionTypes.PAYMENT_TO_LOAN_SUCCESS)) {

                    printBalanceAmount();
                    printLoanAmount();
                    printOwingAmount();
                    return;

                } else if (transactionTypes.equals(PaymentTransactionTypes.INVALID_PAYMENT_TRANSACTION_NO_DEBIT_BALANCE)) {

                    consoleService.write("Balance is Zero Cannot Pay ");
                    printBalanceAmount();
                    printOwingAmount();
                    return;

                } else if (transactionTypes.equals(PaymentTransactionTypes.INVALID_PAYMENT_TRANSACTION_NO_LOAN_ALLOWED_EXISTING_LOAN_PRESENT)) {

                    consoleService.write("Already Have a Existing Loan -- Cannot do Payment -- Please clear your Loan Amount " + userService.getAccountDetails(userService.getLoggedInuser().getId()).getLoanAmount());
                    printBalanceAmount();
                    printOwingAmount();
                    return;

                } else if (transactionTypes.equals(PaymentTransactionTypes.LOAN_REPAY_DISPUTE_PRENDING)) {

                    consoleService.write("Cannot Pay as there is a Existing Loan Reversal");
                    printBalanceAmount();
                    printOwingAmount();
                    return;

                } else {
                    consoleService.write("Transaction Failure -- Cannot Topup");
                    printBalanceAmount();
                    printOwingAmount();
                    return;
                }


            } else {
                consoleService.write("Please Provide Valid User ");
                return;
            }


        } catch (Exception e) {
            consoleService.write("Some Thing Went wrong Please retry ");
            printBalanceAmount();
            return;
        }

    }


    Availability loginAvailability() {
        return !this.userService.isConnected() ? Availability.available() : Availability.unavailable("You are Already Logged in ");
    }

    @ShellMethod("Logout of the Banking Cli")
    public void logout() {
        this.userService.disconnect();
        this.consoleService.write("Logged out ");
    }

    Availability logoutAvailability() {
        return this.userService.isConnected() ? Availability.available() : Availability.unavailable("You are not Logged in ");
    }


    private void printLoanAmount() {

        UserAccountDetails loggedInUserAccountDetails = userService.getAccountDetails(userService.getLoggedInuser().getId());

        Double loanAmount = loggedInUserAccountDetails.getLoanAmount();

        TransactionDetails loanTransactionDetails = userService.getLoggedinUserLoanDetails();

        if (loanAmount > 0 && loanTransactionDetails != null) {
            consoleService.write("Owing  " + loanAmount + " to " + userService.getUserDetails(loanTransactionDetails.getCredit_userid()).getUserName());
        }

    }


    private void printOwingAmount() {

        UserAccountDetails loggedInUserAccountDetails = userService.getAccountDetails(userService.getLoggedInuser().getId());

        TransactionDetails loanTransactionDetails = userService.getLoanTransactionDetailsCreditUserid(userService.getLoggedInuser().getId());
        if (loanTransactionDetails == null) {
            return;
        }

        Double loanAmount = userService.getAccountDetails(loanTransactionDetails.getDebit_userid()).getLoanAmount();

        if (loanAmount > 0) {
            consoleService.write("Owing  " + loanAmount + " from " + userService.getUserDetails(loanTransactionDetails.getDebit_userid()).getUserName());
        }

    }

    private void printBalanceAmount() {

        consoleService.write("Your Balance is   %s ", userService.getAccountBalance(userService.getLoggedInuser().getId()) + "");
    }


    /*
    @ShellMethod("Print the list of Users")
    public void print(){
        String joined = String.join(" -- ",  userService.getusernames());
        this.consoleService.write("List of Users in System today  %s. ", joined);
    }
*/

}
