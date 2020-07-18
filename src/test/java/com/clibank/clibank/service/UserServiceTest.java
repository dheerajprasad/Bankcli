package com.clibank.clibank.service;

import com.clibank.clibank.constants.UserCreation;
import com.clibank.clibank.model.User;
import com.clibank.clibank.model.UserAccountDetails;
import com.clibank.clibank.repository.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceTest {

    @Mock
    private UserRepositoryimpl userRepository;
    @Mock
    private LoanRespositoryimpl loanRespository;
    @Mock
    private AccountRepositoryimpl accountRespository;
    @Mock
    private TransactionRepositoryImpl transactionRepository;

    @InjectMocks
    UserServiceImpl userService;


    @Test
    public void test_login_ExistUsersuccess() {
        when(userRepository.getUserByName(Mockito.any())).thenReturn(getUser());
        when(accountRespository.getUserAccountDetails(Mockito.anyInt())).thenReturn(getAccount());
        Assert.assertEquals(UserCreation.USER_EXIST_IN_SYSTEM, userService.checkUserExistsElseCreateuser("asdas"));

    }

    @Test
    public void test_login_createUserAndAccountsuccess() {

        when(userRepository.getUserByName(Mockito.any())).thenReturn(null).thenReturn(getUser());
        when(userRepository.createuser(Mockito.anyString())).thenReturn(1);
        when(accountRespository.createAccount(Mockito.anyInt(), Mockito.anyString())).thenReturn(1);
        Assert.assertEquals(UserCreation.USER_ACCOUNT_CREATION_SUCCESS, userService.checkUserExistsElseCreateuser("asdas"));


    }

    @Test
    public void test_login_createUserSuccessAccountFailure() {

        when(userRepository.getUserByName(Mockito.any())).thenReturn(null).thenReturn(getUser());
        when(userRepository.createuser(Mockito.anyString())).thenReturn(1);
        when(accountRespository.createAccount(Mockito.anyInt(), Mockito.anyString())).thenReturn(0);
        Assert.assertEquals(UserCreation.USER_CREATION_SUCCESS_ACCOUNT_CREATION_FAILURE, userService.checkUserExistsElseCreateuser("asdas"));


    }

    @Test
    public void test_login_createUserFailure() {

        when(userRepository.getUserByName(Mockito.any())).thenReturn(null);
        when(userRepository.createuser(Mockito.anyString())).thenReturn(0);
        Assert.assertEquals(UserCreation.USER_CREATION_FAILURE, userService.checkUserExistsElseCreateuser("asdas"));


    }

    @Test
    public void test_login_createUserFailures() {

        when(userRepository.getUserByName(Mockito.any())).thenReturn(null).thenThrow(NullPointerException.class);
        when(userRepository.createuser(Mockito.anyString())).thenReturn(1);
        Assert.assertEquals(UserCreation.USER_ACCOUNT_CREATION_EXCEPTION, userService.checkUserExistsElseCreateuser("asdas"));


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


}