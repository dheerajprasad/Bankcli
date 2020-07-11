package com.clibank.clibank.repository;

import com.clibank.clibank.model.TransactionDetails;
import com.clibank.clibank.model.User;
import com.clibank.clibank.model.UserAccountDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Repository
public class TransactionRepositoryImpl implements TransactionRepository {

    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private static final String DEBIT_USERID ="DEBIT_USERID";
    private static final String DEBIT_ACCOUNT_ID ="DEBIT_ACCOUNT_ID";
    private static final String CREDIT_USERID ="CREDIT_USERID";
    private static final String CREDIT_ACCOUNT_ID ="CREDIT_ACCOUNT_ID";
    private static final String TRANSACTION_AMOUNT ="TRANSACTION_AMOUNT";



    private static final String TRANSACTION_TYPE ="TRANSACTION_TYPE";

    private static final String CREATE_TRANSACTION = "INSERT INTO USER_TRANSACTIONS(DEBIT_USERID,DEBIT_ACCOUNT_ID,CREDIT_USERID,CREDIT_ACCOUNT_ID,TRANSACTION_AMOUNT,TRANSACTION_TYPE,CREATED_DATE,UPDATED_DATE) VALUES(:DEBIT_USERID,:DEBIT_ACCOUNT_ID,:CREDIT_USERID,:CREDIT_ACCOUNT_ID,:TRANSACTION_AMOUNT,:TRANSACTION_TYPE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)";
    private static final String GET_LOAN_DETAILS_DEBIT_USERID = "SELECT ID , DEBIT_USERID , DEBIT_ACCOUNT_ID , CREDIT_USERID , CREDIT_ACCOUNT_ID , TRANSACTION_AMOUNT , TRANSACTION_TYPE , CREATED_DATE , UPDATED_DATE FROM  USER_TRANSACTIONS where DEBIT_USERID=:DEBIT_USERID AND TRANSACTION_TYPE ='LOAN'";
    private static final String GET_LOAN_DETAILS_CREDIT_USERID = "SELECT ID , DEBIT_USERID , DEBIT_ACCOUNT_ID , CREDIT_USERID , CREDIT_ACCOUNT_ID , TRANSACTION_AMOUNT , TRANSACTION_TYPE , CREATED_DATE , UPDATED_DATE FROM  USER_TRANSACTIONS where CREDIT_USERID=:CREDIT_USERID AND TRANSACTION_TYPE ='LOAN'";

    @Override
    public TransactionDetails getLoanTransactionDetailsDebitUserid(int userId) {
        Map<String,Object> params = new HashMap<>();
        params.put(DEBIT_USERID,userId);
        return namedParameterJdbcTemplate.query(GET_LOAN_DETAILS_DEBIT_USERID,params, (ResultSetExtractor<TransactionDetails>) this::getTransactionDetails);
    }

    @Override
    public TransactionDetails getLoanTransactionDetailsCreditUserid(int userId) {
        Map<String,Object> params = new HashMap<>();
        params.put(CREDIT_USERID,userId);
        return namedParameterJdbcTemplate.query(GET_LOAN_DETAILS_CREDIT_USERID,params, (ResultSetExtractor<TransactionDetails>) this::getTransactionDetails);
    }

    @Override
    public int createTopupTransaction(User user, Double topupAmount , UserAccountDetails userAccountDetails) {

        Map<String,Object> params = new HashMap<>();
        params.put(DEBIT_USERID,user.getId());
        params.put(DEBIT_ACCOUNT_ID,user.getId()+"Linked Account");
        params.put(CREDIT_USERID,user.getId()+user.getId());
        params.put(CREDIT_ACCOUNT_ID,userAccountDetails.getAccount_number());
        params.put(TRANSACTION_AMOUNT,user.getId()+topupAmount);
        params.put(TRANSACTION_TYPE,"Top up");
        return    namedParameterJdbcTemplate.update(CREATE_TRANSACTION,params);
    }


    @Override
    public int createTransaction(UserAccountDetails debitAccountDetails, Double transactionAmount , UserAccountDetails creditAccountDetails, String transactionType) {

        Map<String,Object> params = new HashMap<>();
        params.put(DEBIT_USERID,debitAccountDetails.getUserid());
        params.put(DEBIT_ACCOUNT_ID,debitAccountDetails.getAccount_number());
        params.put(CREDIT_USERID,creditAccountDetails.getUserid());
        params.put(CREDIT_ACCOUNT_ID,creditAccountDetails.getAccount_number());
        params.put(TRANSACTION_AMOUNT,transactionAmount);
        params.put(TRANSACTION_TYPE,transactionType);
        return    namedParameterJdbcTemplate.update(CREATE_TRANSACTION,params);
    }


    protected TransactionDetails getTransactionDetails(ResultSet rs) throws SQLException {

        if(!rs.next()){
            return null;
        }

        TransactionDetails transactionDetails = new TransactionDetails();
        transactionDetails.setId(rs.getInt("ID"));
        transactionDetails.setDebit_userid(rs.getInt("DEBIT_USERID"));
        transactionDetails.setDebit_account_id(rs.getString("DEBIT_ACCOUNT_ID"));
        transactionDetails.setCredit_userid(rs.getInt("CREDIT_USERID"));
        transactionDetails.setCredit_account_id(rs.getString("CREDIT_ACCOUNT_ID"));
        transactionDetails.setTransaction_amount(rs.getDouble("TRANSACTION_AMOUNT"));
        transactionDetails.setTransaction_type(rs.getString("TRANSACTION_TYPE"));
        transactionDetails.setCreated_date(rs.getObject("CREATED_DATE", Date.class));
        transactionDetails.setUpdated_date(rs.getObject("UPDATED_DATE", Date.class));
        return transactionDetails;

    }

}
