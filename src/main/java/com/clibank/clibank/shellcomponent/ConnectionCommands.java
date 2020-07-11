package com.clibank.clibank.shellcomponent;


import com.clibank.clibank.constants.PaymentTransactionTypes;
import com.clibank.clibank.constants.TransactionTypes;
import com.clibank.clibank.model.User;
import com.clibank.clibank.model.UserAccountDetails;
import com.clibank.clibank.service.ConsoleService;
import com.clibank.clibank.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.util.StringUtils;

@ShellComponent
public class ConnectionCommands {

    @Autowired
    private UserService userService;

    @Autowired
    private ConsoleService consoleService;


    @ShellMethod("Login to the Banking Cli")
    public void login(String userName) {
        User user = userService.checkUserExistsElseCreateuser(userName);
        consoleService.write("Hello  %s ", user.getUserName());
        consoleService.write("Your Balance is   %s ", userService.getAccountBalance(user.getId()) + "");

    }

    @ShellMethod("Top up")
    public void topup(Double topupAmount) {
        User user = userService.getLoggedInuser();
        PaymentTransactionTypes transactionTypes  = userService.topUpTransaction(user, topupAmount);
        if (transactionTypes.equals(PaymentTransactionTypes.TOP_UP_SUCCESS)) {
            consoleService.write("Your Balance is   %s ", userService.getAccountBalance(user.getId()) + "");

        }

    }

    @ShellMethod("Pay")
    public void Pay(String  creditor , Double transactionAmount) {

        try {

            if (StringUtils.hasText(creditor)) {
                    if (transactionAmount <= 0) {
                        consoleService.write("Enter Valid Amount Greater than 0");
                        return;
                    }
                    User credUser = userService.getUserDetails(creditor);
                    if (credUser == null) {
                        consoleService.write("Creditor Not Available in System ");
                        return;
                    }
                    UserAccountDetails credUserAccountDetails = userService.getAccountDetails(credUser.getId());

                    if (credUserAccountDetails == null) {
                        consoleService.write("Creditor Not Available in System ");
                        return;
                    }

                    PaymentTransactionTypes transactionTypes =userService.pay(credUserAccountDetails,transactionAmount);

                   if(transactionTypes.equals(PaymentTransactionTypes.PAYMENT_TRANCTION_SUCESS)){
                    consoleService.write("Transferred  " + transactionAmount+" to "+credUser.getUserName() );
                    consoleService.write("Your Balance is   %s ", userService.getAccountBalance(userService.getLoggedInuser().getId())+"");
                    return;


                }else{
                       consoleService.write("Transaction Failed -- Technical Error" );
                       return;
                     }


            } else {
                consoleService.write("Please Provide Valid User ");
                return;
            }


        } catch (Exception e) {
            consoleService.write("Some Thing Went wrong Please retry ");
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





    /*
    @ShellMethod("Print the list of Users")
    public void print(){
        String joined = String.join(" -- ",  userService.getusernames());
        this.consoleService.write("List of Users in System today  %s. ", joined);
    }
*/

}
