package com.clibank.clibank.repository;

import com.clibank.clibank.model.UserAccountDetails;
import org.junit.After;
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

@RunWith(SpringRunner.class)
@JdbcTest
@Import({AccountRepositoryimpl.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class AccountRepositoryimplTest {

    @Autowired
    private AccountRespository respository;

    @Test
    public void test_createAccount() {
        Assert.assertNotNull(respository.createAccount(10, "Account"));
        Assert.assertEquals(1, respository.createAccount(2, "Account"));
    }
    @Test
    @Sql(statements = {"INSERT INTO USER_ACCOUNTDETAILS(USERID,ACCOUNT_NUMBER,BALANCE,EARMARKAMOUNT,LOANAMOUNT,ISLOANPAYMNETALLOWED,VERSION,CREATED_DATE,UPDATED_DATE) VALUES (13,'1231233',100000,0,0,'TRUE',1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)"})
    public void test_getAccountDetails() {

        UserAccountDetails test= respository.getUserAccountDetails(13);
     Assert.assertNotNull( test);
     Assert.assertEquals(java.util.Optional.ofNullable(13), java.util.Optional.ofNullable(test.getUserid()));
    }

    @Test
    @Sql(statements = {"INSERT INTO USER_ACCOUNTDETAILS(USERID,ACCOUNT_NUMBER,BALANCE,EARMARKAMOUNT,LOANAMOUNT,ISLOANPAYMNETALLOWED,VERSION,CREATED_DATE,UPDATED_DATE) VALUES (13,'1231233',100000,0,0,'TRUE',1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)"})
    public void test_updateisLoanPaymentAllowed() {

        respository.updateisLoanPaymentAllowed(13,"FALSE",1);
        UserAccountDetails test= respository.getUserAccountDetails(13);
        Assert.assertNotNull( test);
        Assert.assertEquals("FALSE", test.getIsLoanRepayMentAllowed());
    }
    @Test
    @Sql(statements = {"INSERT INTO USER_ACCOUNTDETAILS(USERID,ACCOUNT_NUMBER,BALANCE,EARMARKAMOUNT,LOANAMOUNT,ISLOANPAYMNETALLOWED,VERSION,CREATED_DATE,UPDATED_DATE) VALUES (14,'1231233',100000,0,0,'TRUE',1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)"})
    public void test_updateBalanceAndEarmark() {

        int num = respository.updateBalanceAndEarMarkAmount(14,1.0,1.0,1);
        UserAccountDetails test= respository.getUserAccountDetails(14);
        Assert.assertNotNull( test);
        Assert.assertEquals(java.util.Optional.ofNullable(1.0), java.util.Optional.ofNullable(test.getBalance()));
    }

    @Test
    @Sql(statements = {"INSERT INTO USER_ACCOUNTDETAILS(USERID,ACCOUNT_NUMBER,BALANCE,EARMARKAMOUNT,LOANAMOUNT,ISLOANPAYMNETALLOWED,VERSION,CREATED_DATE,UPDATED_DATE) VALUES (15,'1231233',100000,0,0,'TRUE',1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)"})
    public void test_updateBalance() {

        respository.updateBalance(15,2.0,1);
        UserAccountDetails test= respository.getUserAccountDetails(15);
        Assert.assertNotNull( test);
        Assert.assertEquals(java.util.Optional.ofNullable(2.0), java.util.Optional.ofNullable(test.getBalance()));
    }
    @Test
    @Sql(statements = {"INSERT INTO USER_ACCOUNTDETAILS(USERID,ACCOUNT_NUMBER,BALANCE,EARMARKAMOUNT,LOANAMOUNT,ISLOANPAYMNETALLOWED,VERSION,CREATED_DATE,UPDATED_DATE) VALUES (16,'1231233',100000,0,0,'TRUE',1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)"})
    public void test_updateLoanAmountAndLoanRepaymen() {

        respository.updateLoanAmountAndLoanRepayment(16,2.0,"TRUE",1);
        UserAccountDetails test= respository.getUserAccountDetails(16);
        Assert.assertNotNull( test);
        Assert.assertEquals(java.util.Optional.ofNullable(2.0), java.util.Optional.ofNullable(test.getLoanAmount()));
    }




}