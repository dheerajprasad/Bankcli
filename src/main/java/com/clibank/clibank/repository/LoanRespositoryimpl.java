package com.clibank.clibank.repository;

import com.clibank.clibank.model.UserAccountDetails;
import com.clibank.clibank.model.UserLoanDetails;
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
public class LoanRespositoryimpl implements LoanRespository{

    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private static final String USERID ="USERID";
    private static final String PAY_TO_USERID ="PAY_TO_USERID";
    private static final String Account_NUMBER ="ACCOUNT_NUMBER";
    private static final String BALANCE ="BALANCE";
    private static final String EARMARKAMOUNT ="EARMARKAMOUNT";
    private static final String VERSION ="VERSION";
    private static final String CREATE_USER = "INSERT INTO USER_LOANDETAILS (USERID,ACCOUNT_NUMBER,BALANCE,EARMARKAMOUNT,PAY_TO_USERID,VERSION,CREATED_DATE,UPDATED_DATE) VALUES (:USERID,:ACCOUNT_NUMBER,:BALANCE,:EARMARKAMOUNT,:PAY_TO_USERID,:VERSION,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)";
    private static final String GET_ACCOUNT = "SELECT ID,USERID,ACCOUNT_NUMBER,BALANCE,EARMARKAMOUNT,PAY_TO_USERID,VERSION,CREATED_DATE,UPDATED_DATE from USER_LOANDETAILS where userid=:USERID";
    private static final String GET_ACCOUNT_PAY_TO_USER_ID = "SELECT ID,USERID,ACCOUNT_NUMBER,BALANCE,EARMARKAMOUNT,PAY_TO_USERID,VERSION,CREATED_DATE,UPDATED_DATE from USER_LOANDETAILS where PAY_TO_USERID=:PAY_TO_USERID";
    private static final String UPDATE_ACCOUNT =  "UPDATE  USER_LOANDETAILS SET BALANCE =:BALANCE WHERE USERID=:USERID AND VERSION=:VERSION";
    private static final String UPDATE_EARMARK =  "UPDATE  USER_LOANDETAILS SET EARMARKAMOUNT =:EARMARKAMOUNT WHERE USERID=:USERID AND VERSION=:VERSION";
    private static final String UPDATE_BALANCEAND_EARMARKAMOUNT=  "UPDATE  USER_LOANDETAILS SET BALANCE =:BALANCE ,EARMARKAMOUNT =:EARMARKAMOUNT  WHERE USERID=:USERID AND VERSION=:VERSION";

    @Override
    public int createAccount(int userId, String accountNumber, int payToUserId) {
        Map<String,Object> params = new HashMap<>();
        params.put(USERID,userId);
        params.put(Account_NUMBER,accountNumber);
        params.put(BALANCE,0);
        params.put(EARMARKAMOUNT,0);
        params.put(VERSION,1);
        params.put(PAY_TO_USERID,payToUserId);
        return  namedParameterJdbcTemplate.update(CREATE_USER,params);
    }

    @Override
    public UserLoanDetails getUserAccountDetails(int Userid) {
        Map<String,Object> params = new HashMap<>();
        params.put(USERID,Userid);
        return namedParameterJdbcTemplate.query(GET_ACCOUNT,params, (ResultSetExtractor<UserLoanDetails>) this::getLoanDetails);

    }

    @Override
    public UserLoanDetails getUserAccountDetailsPayToUserId(int payToUserId) {
        Map<String,Object> params = new HashMap<>();
        params.put(PAY_TO_USERID,payToUserId);
        return namedParameterJdbcTemplate.query(GET_ACCOUNT_PAY_TO_USER_ID,params, (ResultSetExtractor<UserLoanDetails>) this::getLoanDetails);

    }

    @Override
    public int updateBalance(int userId, Double amount, int version) {
        Map<String,Object> params = new HashMap<>();
        params.put(USERID,userId);
        params.put(BALANCE,amount);
        params.put(VERSION,version);
        return namedParameterJdbcTemplate.update(UPDATE_ACCOUNT,params);
    }

    @Override
    public int updateEarMarkAmount(int userId, Double amount, int version) {
        Map<String,Object> params = new HashMap<>();
        params.put(USERID,userId);
        params.put(EARMARKAMOUNT,amount);
        params.put(VERSION,version);
        return namedParameterJdbcTemplate.update(UPDATE_EARMARK,params);
    }

    @Override
    public int updateBalanceAndEarMarkAmount(int userId, Double Balanceamount, Double earMarkAmount, int version) {
        Map<String,Object> params = new HashMap<>();
        params.put(USERID,userId);
        params.put(BALANCE,Balanceamount);
        params.put(EARMARKAMOUNT,earMarkAmount);
        params.put(VERSION,version);
        return namedParameterJdbcTemplate.update(UPDATE_BALANCEAND_EARMARKAMOUNT,params);
    }

    protected UserLoanDetails getLoanDetails(ResultSet rs) throws SQLException {
        if(!rs.next()){
            return null;
        }
        UserLoanDetails userLoanDetails = new UserLoanDetails();
        userLoanDetails.setId(rs.getInt("ID"));
        userLoanDetails.setUserid(rs.getInt("USERID"));
        userLoanDetails.setAccount_number(rs.getString("account_number"));
        userLoanDetails.setBalance(rs.getDouble("BALANCE" ));
        userLoanDetails.setEarMarkAmount(rs.getDouble("EARMARKAMOUNT"));
        userLoanDetails.setPayToUserId(rs.getInt("PAY_TO_USERID"));
        userLoanDetails.setVersion(rs.getInt("VERSION"));
        userLoanDetails.setCreated_date(rs.getObject("CREATED_DATE", Date.class));
        userLoanDetails.setUpdated_date(rs.getObject("UPDATED_DATE", Date.class));

        return userLoanDetails;

    }
}
