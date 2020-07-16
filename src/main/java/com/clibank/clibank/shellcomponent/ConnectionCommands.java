package com.clibank.clibank.shellcomponent;


import com.clibank.clibank.constants.PaymentTransactionTypes;
import com.clibank.clibank.constants.UserCreation;
import com.clibank.clibank.model.User;
import com.clibank.clibank.model.UserAccountDetails;
import com.clibank.clibank.model.UserLoanDetails;
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
       UserCreation userCreation = userService.checkUserExistsElseCreateuser(userName);
        if(userCreation.equals(UserCreation.USER_ACCOUNT_CREATION_SUCCESS)){
           User user =userService.getLoggedInuser();
            consoleService.write("Hello , %s !", user.getUserName());
            consoleService.write("Your Balance is   %s ", userService.getAccountBalance(user.getId()) + "");
            printOwingAmount();
            printLoanAmount();
        }else if (userCreation.equals(UserCreation.USER_CREATION_SUCCESS_ACCOUNT_CREATION_FAILURE)){
            User user =userService.getLoggedInuser();
            consoleService.write("Hello , %s !", user.getUserName());
            consoleService.write("Failed to Create Account Retry Login After Some Time or reach to Customer Care");
        }else if (userCreation.equals(UserCreation.USER_EXIST_IN_SYSTEM)){
            User user =userService.getLoggedInuser();
            consoleService.write("Hello , %s !", user.getUserName());
            consoleService.write("Your Balance is   %s ", userService.getAccountBalance(user.getId()) + "");
            printOwingAmount();
            printLoanAmount();
        }else {
            consoleService.write("Service Not available now Retest Later" );

        }


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
                consoleService.write("Transferred  " + userService.getTransferedAmount() + " to " + userService.getUserDetails(userService.getLoanAccountDetails(user.getId()).getPayToUserId()).getUserName());
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
            consoleService.write("Some Thing Went wrong Please retry ");
            log.info("Exception " + e);
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


                } else if (transactionTypes.equals(PaymentTransactionTypes.PAYMENT_TO_LOAN_SUCCESS)) {
                    printBalanceAmount();
                    printLoanAmount();
                    printOwingAmount();
                    return;

                } else if (transactionTypes.equals(PaymentTransactionTypes.PAYMNET_AND_LOAN_SUCCESS)) {

                    consoleService.write("Transferred  " + userService.getTransferedAmount() + " to " + credUser.getUserName());
                    printBalanceAmount();
                    printLoanAmount();
                    printOwingAmount();
                    return;

                } else if (transactionTypes.equals(PaymentTransactionTypes.PAYMENT_INITIATED_WITH_LOAN_AND_PAYMENT_SUCCESS_LOAN_FAILURE)) {
                    consoleService.write("Transferred  " + transactionAmount + " to " + credUser.getUserName());
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

                    consoleService.write("Already Have a Existing Loan -- Cannot do Payment -- Please clear your Loan Amount " + userService.getLoanAccountDetails(userService.getLoggedInuser().getId()).getAvailableBalance());
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

    Availability PayAvailability() {
        return this.userService.isConnected() ? Availability.available() : Availability.unavailable("You are not Logged in ");
    }
    Availability topupAvailability() {
        return this.userService.isConnected() ? Availability.available() : Availability.unavailable("You are not Logged in ");
    }

    private void printLoanAmount() {

        UserLoanDetails userLoanDetails = userService.getLoanAccountDetails(userService.getLoggedInuser().getId());
        if (userLoanDetails == null) {
            return;
        }
        Double loanAmount = userLoanDetails.getAvailableBalance();

        if (loanAmount > 0) {
            consoleService.write("Owing  " + loanAmount + " to " + userService.getUserDetails(userLoanDetails.getPayToUserId()).getUserName());
        }

    }


    private void printOwingAmount() {

        UserLoanDetails userLoanDetails = userService.getLoanAccountforPaytoDetails(userService.getLoggedInuser().getId());
        if (userLoanDetails == null) {
            return;
        }
        Double loanAmount = userLoanDetails.getAvailableBalance();
        if (loanAmount > 0) {
            consoleService.write("Owing  " + loanAmount + " from " + userService.getUserDetails(userLoanDetails.getUserid()).getUserName());
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
