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

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceImplTest {

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

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Test
    public void test_create() {

        User debitUser = getUser();
        User creditUser = getUser();
        UserAccountDetails debitAccountDetails = getAccount();
        debitAccountDetails.setAvailableBalance(10.);
        UserAccountDetails creditAccountDetails = getAccount();
        creditAccountDetails.setAvailableBalance(1.);
        when(accountRespository.updateBalanceAndEarMarkAmount(Mockito.anyInt(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyInt())).thenReturn(0);
        Assert.assertEquals(PaymentTransactionTypes.PAYMENT_TRANCTION_SUCESS, transactionService.createPaymentTransaction(10.00, 8.00, 10.00, 12.00, 2.00, debitUser, creditUser, debitAccountDetails, creditAccountDetails, TransactionTypes.FUND_TRANSFER));

    }

    @Test
    public void test_createException() {

        User debitUser = getUser();
        User creditUser = getUser();
        UserAccountDetails debitAccountDetails = getAccount();
        debitAccountDetails.setAvailableBalance(10.);
        UserAccountDetails creditAccountDetails = getAccount();
        creditAccountDetails.setAvailableBalance(1.);
        when(accountRespository.updateBalanceAndEarMarkAmount(Mockito.anyInt(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyInt())).thenThrow(NullPointerException.class);
        when(accountRespository.updateBalance(Mockito.anyInt(), Mockito.anyDouble(), Mockito.anyInt())).thenReturn(1);
        when(transactionRepository.createTransaction(Mockito.any(), Mockito.anyDouble(), Mockito.any(), Mockito.any())).thenReturn(1);
        when(accountRespository.updateEarMarkAmount(Mockito.anyInt(), Mockito.anyDouble(), Mockito.anyInt())).thenReturn(1);
        Assert.assertEquals( PaymentTransactionTypes.PAYMENT_TRANCTION_FAILURE, transactionService.createPaymentTransaction(10.00, 8.00, 10.00, 12.00, 2.00, debitUser, creditUser, debitAccountDetails, creditAccountDetails, TransactionTypes.FUND_TRANSFER));

    }

    @Test
    public void test_PaymentFailure() {


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
        userLoanDetails.setAvailableBalance(10.0);
        userLoanDetails.setPayToUserId(2);
        userLoanDetails.setVersion(1);


        return userLoanDetails;

    }


}