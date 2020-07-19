package com.clibank.clibank.service;

import com.clibank.clibank.constants.PaymentTransactionTypes;
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

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PayServiceImplTest {

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

    @InjectMocks
    PayServiceImpl payService;

    @Mock
    private TransactionService transactionService;


    @Test
    public void testPayBalanceZero() {

        when(userService.getLoggedInuser()).thenReturn(getUser());
        UserAccountDetails userAccountDetails = getAccount();
        userAccountDetails.setBalance(0.0);
        when(accountRespository.getUserAccountDetails(Mockito.anyInt())).thenReturn(userAccountDetails);
        when(userRepository.getUserByid(Mockito.anyInt())).thenReturn(getUser());
        Assert.assertEquals(PaymentTransactionTypes.INVALID_PAYMENT_TRANSACTION_NO_DEBIT_BALANCE, payService.pay(getAccount(), 1.0));
    }

    @Test
    public void payTranAmountLessOrEqualToBalance_debitFailure() {

        User debitUser = getUser();
        User creditUser = getUser();
        UserAccountDetails debitAccountDetails = getAccount();
        debitAccountDetails.setBalance(10.);
        UserAccountDetails creditAccountDetails = getAccount();
        creditAccountDetails.setBalance(1.);
        payService.payTranAmountLessOrEqualToBalance(debitUser, creditUser, debitAccountDetails, creditAccountDetails, 1.0);
        verify(transactionService, times(1)).createPaymentTransaction(Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any());

    }

    @Test
    public void testPayExistingLoan() {

        when(userService.getLoggedInuser()).thenReturn(getUser());
        when(accountRespository.getUserAccountDetails(Mockito.anyInt())).thenReturn(getAccount());
        when(userRepository.getUserByid(Mockito.anyInt())).thenReturn(getUser());
        UserLoanDetails userLoanDetails = getLoanAccountDetails();
        userLoanDetails.setBalance(10.);
        when(loanRespository.getUserAccountDetails(Mockito.anyInt())).thenReturn(null).thenReturn(getLoanAccountDetails());
        Assert.assertEquals(PaymentTransactionTypes.LOAN_ACCOUNT_CREATION_FAILURE, payService.pay(getAccount(), 100.));
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
        userAccountDetails.setBalance(10.);

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