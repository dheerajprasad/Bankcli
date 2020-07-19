package com.clibank.clibank.service;

import com.clibank.clibank.constants.PaymentTransactionTypes;
import com.clibank.clibank.constants.TransactionTypes;
import com.clibank.clibank.model.User;
import com.clibank.clibank.model.UserAccountDetails;
import com.clibank.clibank.model.UserLoanDetails;
import com.clibank.clibank.repository.AccountRepositoryimpl;
import com.clibank.clibank.repository.LoanRespositoryimpl;
import com.clibank.clibank.repository.TransactionRepositoryImpl;
import com.clibank.clibank.repository.UserRepositoryimpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TopupServiceimplTest {

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
    private TransactionServiceImpl transactionService;

    @InjectMocks
    private TopupServiceimpl topupservice;



    @Test
    public void testTopup_PaymentFail() {

        when(userService.getLoggedInuser()).thenReturn(getUser());
        when(accountRespository.getUserAccountDetails(Mockito.anyInt())).thenReturn(getAccount()).thenReturn(getAccount()).thenReturn(getAccount());
        when(userRepository.getUserByid(Mockito.anyInt())).thenReturn(getUser());
        when(userService.getPoolAccountDetails()).thenReturn(getAccount());
        when(userService.getPoolUserDetails()).thenReturn(getUser());
        when(loanRespository.getUserAccountDetails(Mockito.anyInt())).thenReturn(null).thenReturn(getLoanAccountDetails());
        when(transactionService.createPaymentTransaction(Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(PaymentTransactionTypes.PAYMENT_TRANCTION_FAILURE);
        Assert.assertEquals(PaymentTransactionTypes.TOP_UP_FAILURE,topupservice.topUpTransaction(getUser(),5.0));
    }

    @Test
    public void testTopupSuccess_NoLoan() {

        when(userService.getLoggedInuser()).thenReturn(getUser());
        when(accountRespository.getUserAccountDetails(Mockito.anyInt())).thenReturn(getAccount()).thenReturn(getAccount()).thenReturn(getAccount());
        when(loanRespository.getUserAccountDetails(Mockito.anyInt())).thenReturn(getLoanAccountDetails());
        when(userRepository.getUserByid(Mockito.anyInt())).thenReturn(getUser());
        when(userService.getPoolAccountDetails()).thenReturn(getAccount());
        when(userService.getPoolUserDetails()).thenReturn(getUser());
        when(loanRespository.getUserAccountDetails(Mockito.anyInt())).thenReturn(null).thenReturn(getLoanAccountDetails());
        when(transactionService.createPaymentTransaction(Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(PaymentTransactionTypes.PAYMENT_TRANCTION_SUCESS);
        Assert.assertEquals(PaymentTransactionTypes.TOP_UP_SUCCESS,topupservice.topUpTransaction(getUser(),5.0));
    }






    private User getUser() {
        User user = new User();
        user.setUserName("asdas");
        user.setId(1);
        return user;
    }

    private UserAccountDetails getAccount() {

        UserAccountDetails userAccountDetails = new UserAccountDetails();
        userAccountDetails.setUserid(1);
        userAccountDetails.setAccount_number("1212");
        userAccountDetails.setBalance(1.);
        userAccountDetails.setEarMarkAmount(0.);
        userAccountDetails.setUserid(1);
        userAccountDetails.setVersion(1);
        userAccountDetails.setAvailableBalance(10.);

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