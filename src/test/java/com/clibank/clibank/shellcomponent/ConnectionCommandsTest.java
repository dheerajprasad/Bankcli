package com.clibank.clibank.shellcomponent;

import com.clibank.clibank.constants.UserCreation;
import com.clibank.clibank.model.User;
import com.clibank.clibank.model.UserAccountDetails;
import com.clibank.clibank.model.UserLoanDetails;
import com.clibank.clibank.repository.AccountRepositoryimpl;
import com.clibank.clibank.repository.LoanRespositoryimpl;
import com.clibank.clibank.repository.TransactionRepositoryImpl;
import com.clibank.clibank.repository.UserRepositoryimpl;
import com.clibank.clibank.service.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ConnectionCommandsTest {
    @Mock
    private UserRepositoryimpl userRepository;
    @Mock
    private LoanRespositoryimpl loanRespository;
    @Mock
    private AccountRepositoryimpl accountRespository;
    @Mock
    private TransactionRepositoryImpl transactionRepository;

    @Mock
    UserServiceImpl userService;

    @Mock
    PayServiceImpl payService;

    @Mock
    private TopupServiceimpl topupServiceimpl;
    @Mock
    private ConsoleServiceImpl consoleService;

    @InjectMocks
    private ConnectionCommands connectionCommands;


    @Test
    public void logintest() {
        when(userService.checkUserExistsElseCreateuser(Mockito.anyString())).thenReturn(UserCreation.USER_ACCOUNT_CREATION_SUCCESS);

        when(userService.getLoggedInuser()).thenReturn(getUser());
        when(userService.getAccountBalance(Mockito.anyInt())).thenReturn(10.);
        doNothing().when(consoleService).write(Mockito.anyString());
        when(userService.getLoanAccountforPaytoDetails(Mockito.anyInt())).thenReturn(null);
        when( userService.getUserDetails(Mockito.anyString())).thenReturn(getUser());
        connectionCommands.login("String userName");
        verify(userService, times(3)).getLoggedInuser();
    }

    private User getUser() {
        User user = new User();
        user.setUserName("asdas");
        user.setId(1);
        return user;
    }

    private UserAccountDetails getAccount() {

        UserAccountDetails userAccountDetails = new UserAccountDetails();
        userAccountDetails.setAccount_number("1212");
        userAccountDetails.setBalance(1.);
        userAccountDetails.setEarMarkAmount(0.);
        userAccountDetails.setUserid(1);
        userAccountDetails.setVersion(1);

        return userAccountDetails;
    }
    private UserLoanDetails getLoanAccountDetails() {

        UserLoanDetails userLoanDetails = new UserLoanDetails();
        userLoanDetails.setId(1);
        userLoanDetails.setUserid(1);
        userLoanDetails.setAccount_number("a");
        userLoanDetails.setBalance(0.0);
        userLoanDetails.setEarMarkAmount(0.0);
        userLoanDetails.setPayToUserId(2);
        userLoanDetails.setVersion(1);


        return userLoanDetails;

    }


}
