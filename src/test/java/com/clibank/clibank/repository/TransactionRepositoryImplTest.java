package com.clibank.clibank.repository;

import com.clibank.clibank.model.TransactionDetails;
import com.clibank.clibank.model.User;
import com.clibank.clibank.model.UserAccountDetails;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

import static org.junit.Assert.*;
@RunWith(SpringRunner.class)
@JdbcTest
@Import({TransactionRepositoryImpl.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class TransactionRepositoryImplTest {

    @Autowired
    private TransactionRepository respository;

    @Test
    public void test_createTransaction() {

   int insert =respository.createTransaction(getUserDetails(),1.0,getUserDetails2(),"TEST");
      Assert.assertEquals(1,insert);
    }

    @Test
    public void test_createTopUp() {

        int insert =respository.createTopupTransaction(getuser(),10.0,getUserDetails());
        Assert.assertEquals(1,insert);
    }
    private User getuser(){
        User user = new User();
        user.setId(22);
        user.setUserName("Hess");
        return user;
    }


    @Test
    @Sql(statements = {"INSERT INTO USER_TRANSACTIONS(DEBIT_USERID,DEBIT_ACCOUNT_ID,CREDIT_USERID,CREDIT_ACCOUNT_ID,TRANSACTION_AMOUNT,TRANSACTION_TYPE,CREATED_DATE,UPDATED_DATE) VALUES(12,'123',2,'345',10,'LOAN',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)"})
    public void test_getLoanTransactionDetailsDebitUserid() {
       TransactionDetails transactionDetails = respository.getLoanTransactionDetailsDebitUserid(12);
        Assert.assertNotNull(transactionDetails);
        Assert.assertEquals(12,transactionDetails.getDebit_userid());
    }

    private UserAccountDetails getUserDetails(){
        UserAccountDetails userAccountDetails = new UserAccountDetails();
        userAccountDetails.setUserid(1);
        userAccountDetails.setAccount_number("123");
        userAccountDetails.setBalance(1.0);
        userAccountDetails.setEarMarkAmount(0.0);
        userAccountDetails.setVersion(1);
            return userAccountDetails;
    }

    private UserAccountDetails getUserDetails2(){
        UserAccountDetails userAccountDetails = new UserAccountDetails();
        userAccountDetails.setUserid(3);
        userAccountDetails.setAccount_number("123");
        userAccountDetails.setBalance(1.0);
        userAccountDetails.setEarMarkAmount(0.0);
        userAccountDetails.setVersion(1);
        return userAccountDetails;
    }

}