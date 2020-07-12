package com.clibank.clibank.repository;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;
@RunWith(SpringRunner.class)
@JdbcTest
@Import({UserRepositoryimpl.class })
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryimplTest {
    @Autowired
    private UserRepository respository;

    @Test
    @Sql(statements = {"INSERT INTO USER_DETAILS (USERNAME,CREATED_DATE,UPDATED_DATE) VALUES('POOL ACCOUNTS',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)"})
    public void test_getuserid(){
        Assert.assertNotNull(respository.getUserByid(1));
    }

    @Test
    @Sql(statements = {"INSERT INTO USER_DETAILS (USERNAME,CREATED_DATE,UPDATED_DATE) VALUES('POOL ACCOUNTS',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)"})
    public void test_getUserName(){
        Assert.assertNotNull(respository.getUserByName("POOL ACCOUNTS"));
    }


    @Test

    public void test_createUser(){

        Assert.assertEquals("TESTCREATE",respository.createuser("TESTCREATE"));
    }

}