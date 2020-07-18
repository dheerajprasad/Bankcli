package com.clibank.clibank.shellcomponent;


import com.clibank.clibank.constants.PaymentTransactionTypes;
import com.clibank.clibank.constants.UserCreation;
import com.clibank.clibank.model.User;
import com.clibank.clibank.model.UserAccountDetails;
import com.clibank.clibank.model.UserLoanDetails;
import com.clibank.clibank.service.ConsoleServiceImpl;
import com.clibank.clibank.service.PayService;
import com.clibank.clibank.service.TopupServiceimpl;
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
    private ConsoleServiceImpl consoleServiceImpl;

    @Autowired
    private TopupServiceimpl topupServiceimpl;


    @Autowired
    private PayService payService;
    @ShellMethod("Login to the Banking Cli")
    public void login(String userName) {
       UserCreation userCreation = userService.checkUserExistsElseCreateuser(userName);
        if(userCreation.equals(UserCreation.USER_ACCOUNT_CREATION_SUCCESS)){
           User user =userService.getLoggedInuser();
            consoleServiceImpl.write("Hello , %s !", user.getUserName());
            consoleServiceImpl.write("Your Balance is   %s ", userService.getAccountBalance(user.getId()) + "");
            printOwingAmount();
            printLoanAmount();
        }else if (userCreation.equals(UserCreation.USER_CREATION_SUCCESS_ACCOUNT_CREATION_FAILURE)){
            User user =userService.getLoggedInuser();
            consoleServiceImpl.write("Hello , %s !", user.getUserName());
            consoleServiceImpl.write("Failed to Create Account Retry Login After Some Time or reach to Customer Care");
        }else if (userCreation.equals(UserCreation.USER_EXIST_IN_SYSTEM)){
            User user =userService.getLoggedInuser();
            consoleServiceImpl.write("Hello , %s !", user.getUserName());
            consoleServiceImpl.write("Your Balance is   %s ", userService.getAccountBalance(user.getId()) + "");
            printOwingAmount();
            printLoanAmount();
        }else {
            consoleServiceImpl.write("Service Not available now Retest Later" );

        }


    }

    @ShellMethod("Top up")
    public void topup(Double topupAmount) {

        try {
            User user = userService.getLoggedInuser();
            PaymentTransactionTypes transactionTypes = topupServiceimpl.topUpTransaction(user, topupAmount);
            if (transactionTypes.equals(PaymentTransactionTypes.TOP_UP_SUCCESS)) {
                printBalanceAmount();
                printLoanAmount();

            } else if (transactionTypes.equals(PaymentTransactionTypes.TOP_UP_SUCCESS_LOANPAYMENT_SUCCESS)) {
                consoleServiceImpl.write("Transferred  " + userService.getTransferedAmount() + " to " + userService.getUserDetails(userService.getLoanAccountDetails(user.getId()).getPayToUserId()).getUserName());
                printBalanceAmount();
                printLoanAmount();
            } else if (transactionTypes.equals(PaymentTransactionTypes.TOP_UP_SUCCESS_LOAN_REPAYMENT_FAILURE)) {
                consoleServiceImpl.write("Your Topup SuccessFull - Loan Payment Failure", user.getUserName());
                printBalanceAmount();
                printLoanAmount();
            } else if (transactionTypes.equals(PaymentTransactionTypes.TOP_UP_SUCCESS_LOANPAYMENT_SUCCESS_UPDATE_LOAN_AMOUNT_FAILURE)) {
                consoleServiceImpl.write("Your Topup SuccessFull - Loan Payment Failure", user.getUserName());
                printBalanceAmount();
                printLoanAmount();
            } else {
                consoleServiceImpl.write("Transaction Failed  ");

            }
        } catch (Exception e) {
            consoleServiceImpl.write("Some Thing Went wrong Please retry ");
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
                    consoleServiceImpl.write("Enter Valid Amount Greater than 0");
                    return;
                }
                User credUser = userService.getUserDetails(creditor);
                if (credUser == null) {
                    consoleServiceImpl.write("Creditor Not Available in System -- Please Refer to Onboard in the Bank");
                    return;
                }
                if (credUser.getId() == userService.getLoggedInuser().getId()) {
                    consoleServiceImpl.write("Cannot Pay to the Same User");
                    return;

                }

                UserAccountDetails credUserAccountDetails = userService.getAccountDetails(credUser.getId());

                if (credUserAccountDetails == null) {
                    consoleServiceImpl.write("Creditor Not Available in System ");
                    return;
                }

                PaymentTransactionTypes transactionTypes = payService.pay(credUserAccountDetails, transactionAmount);

                if (transactionTypes.equals(PaymentTransactionTypes.PAYMENT_TRANCTION_SUCESS) ) {
                    consoleServiceImpl.write("Transferred  " + transactionAmount + " to " + credUser.getUserName());
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

                    consoleServiceImpl.write("Transferred  " + userService.getTransferedAmount() + " to " + credUser.getUserName());
                    printBalanceAmount();
                    printLoanAmount();
                    printOwingAmount();
                    return;

                } else if (transactionTypes.equals(PaymentTransactionTypes.PAYMENT_INITIATED_WITH_LOAN_AND_PAYMENT_SUCCESS_LOAN_FAILURE)) {
                    consoleServiceImpl.write("Transferred  " + transactionAmount + " to " + credUser.getUserName());
                    printBalanceAmount();
                    printLoanAmount();
                    printOwingAmount();
                    return;

                } else if (transactionTypes.equals(PaymentTransactionTypes.INVALID_PAYMENT_TRANSACTION_NO_DEBIT_BALANCE)) {
                    consoleServiceImpl.write("Balance is Zero Cannot Pay ");
                    printBalanceAmount();
                    printOwingAmount();
                    return;

                } else if (transactionTypes.equals(PaymentTransactionTypes.INVALID_PAYMENT_TRANSACTION_NO_LOAN_ALLOWED_EXISTING_LOAN_PRESENT)) {

                    consoleServiceImpl.write("Already Have a Existing Loan -- Cannot do Payment -- Please clear your Loan Amount " + userService.getLoanAccountDetails(userService.getLoggedInuser().getId()).getAvailableBalance());
                    printBalanceAmount();
                    printOwingAmount();
                    return;

                } else if (transactionTypes.equals(PaymentTransactionTypes.LOAN_REPAY_DISPUTE_PRENDING)) {

                    consoleServiceImpl.write("Cannot Pay as there is a Existing Loan Reversal");
                    printBalanceAmount();
                    printOwingAmount();
                    return;

                } else {
                    consoleServiceImpl.write("Transaction Failure -- Cannot Topup");
                    printBalanceAmount();
                    printOwingAmount();
                    return;
                }


            } else {
                consoleServiceImpl.write("Please Provide Valid User ");
                return;
            }


        } catch (Exception e) {
            consoleServiceImpl.write("Some Thing Went wrong Please retry ");
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
        this.consoleServiceImpl.write("Logged out ");
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
            consoleServiceImpl.write("Owing  " + loanAmount + " to " + userService.getUserDetails(userLoanDetails.getPayToUserId()).getUserName());
        }

    }


    private void printOwingAmount() {

        UserLoanDetails userLoanDetails = userService.getLoanAccountforPaytoDetails(userService.getLoggedInuser().getId());
        if (userLoanDetails == null) {
            return;
        }
        Double loanAmount = userLoanDetails.getAvailableBalance();
        if (loanAmount > 0) {
            consoleServiceImpl.write("Owing  " + loanAmount + " from " + userService.getUserDetails(userLoanDetails.getUserid()).getUserName());
        }

    }

    private void printBalanceAmount() {

        consoleServiceImpl.write("Your Balance is   %s ", userService.getAccountBalance(userService.getLoggedInuser().getId()) + "");
    }



}
