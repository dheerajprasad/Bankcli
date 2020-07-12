package com.clibank.clibank.repository;

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
public class AccountRepositoryimpl implements AccountRespository {
    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private static final String USERID ="USERID";
    private static final String Account_NUMBER ="ACCOUNT_NUMBER";
    private static final String BALANCE ="BALANCE";
    private static final String EARMARKAMOUNT ="EARMARKAMOUNT";
    private static final String VERSION ="VERSION";
    private static final String LOANAMOUNT ="LOANAMOUNT";
    private static final String CREATE_USER = "INSERT INTO USER_ACCOUNTDETAILS (USERID,ACCOUNT_NUMBER,BALANCE,EARMARKAMOUNT,LOANAMOUNT,VERSION,CREATED_DATE,UPDATED_DATE) VALUES (:USERID,:ACCOUNT_NUMBER,:BALANCE,:EARMARKAMOUNT,:LOANAMOUNT,:VERSION,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)";
    private static final String GET_ACCOUNT = "SELECT ID,USERID,ACCOUNT_NUMBER,BALANCE,EARMARKAMOUNT,LOANAMOUNT,VERSION,CREATED_DATE,UPDATED_DATE from USER_ACCOUNTDETAILS where userid=:USERID";
    private static final String UPDATE_ACCOUNT =  "UPDATE  USER_ACCOUNTDETAILS SET BALANCE =:BALANCE WHERE USERID=:USERID AND VERSION=:VERSION";
    private static final String UPDATE_EARMARK =  "UPDATE  USER_ACCOUNTDETAILS SET EARMARKAMOUNT =:EARMARKAMOUNT WHERE USERID=:USERID AND VERSION=:VERSION";
    private static final String UPDATE_LOANAMOUNT=  "UPDATE  USER_ACCOUNTDETAILS SET LOANAMOUNT =:LOANAMOUNT WHERE USERID=:USERID AND VERSION=:VERSION";
    private static final String UPDATE_BALANCEAND_EARMARKAMOUNT=  "UPDATE  USER_ACCOUNTDETAILS SET BALANCE =:BALANCE ,EARMARKAMOUNT =:EARMARKAMOUNT  WHERE USERID=:USERID AND VERSION=:VERSION";

    @Override
    public int createAccount(int userId, String accountNumber) {

        Map<String,Object> params = new HashMap<>();
        params.put(USERID,userId);
        params.put(Account_NUMBER,accountNumber);
        params.put(BALANCE,0);
        params.put(EARMARKAMOUNT,0);
        params.put(LOANAMOUNT,0);
        params.put(VERSION,1);
        return    namedParameterJdbcTemplate.update(CREATE_USER,params);
    }

    @Override
    public UserAccountDetails getUserAccountDetails(int Userid){
        Map<String,Object> params = new HashMap<>();
        params.put(USERID,Userid);
        return namedParameterJdbcTemplate.query(GET_ACCOUNT,params, (ResultSetExtractor<UserAccountDetails>) this::getAccountDetails);

    }

    @Override
    public int updateBalance(int userId, Double amount , int version) {

        Map<String,Object> params = new HashMap<>();
        params.put(USERID,userId);
        params.put(BALANCE,amount);
        params.put(VERSION,version);
        return namedParameterJdbcTemplate.update(UPDATE_ACCOUNT,params);
    }

    @Override
    public int updateBalanceAndEarMarkAmount(int userId, Double Balanceamount ,Double earMarkAmount, int version) {

        Map<String,Object> params = new HashMap<>();
        params.put(USERID,userId);
        params.put(BALANCE,Balanceamount);
        params.put(EARMARKAMOUNT,earMarkAmount);
        params.put(VERSION,version);
        return namedParameterJdbcTemplate.update(UPDATE_BALANCEAND_EARMARKAMOUNT,params);
    }



    public int updateEarMarkAmount(int userId, Double amount, int version) {

        Map<String,Object> params = new HashMap<>();
        params.put(USERID,userId);
        params.put(EARMARKAMOUNT,amount);
        params.put(VERSION,version);
        return namedParameterJdbcTemplate.update(UPDATE_EARMARK,params);
    }

    public int updateLoanAmount(int userId, Double amount ,int version) {

        Map<String,Object> params = new HashMap<>();
        params.put(USERID,userId);
        params.put(LOANAMOUNT,amount);
        params.put(VERSION,version);
        return namedParameterJdbcTemplate.update(UPDATE_LOANAMOUNT,params);
    }


    protected UserAccountDetails getAccountDetails(ResultSet rs) throws SQLException {

        if(!rs.next()){
            return null;
        }

        UserAccountDetails userAccountDetails = new UserAccountDetails();
        userAccountDetails.setId(rs.getInt("ID"));
        userAccountDetails.setUserid(rs.getInt("USERID"));
        userAccountDetails.setAccount_number(rs.getString("account_number"));
        userAccountDetails.setBalance(rs.getDouble("BALANCE"));
        userAccountDetails.setEarMarkAmount(rs.getDouble("EARMARKAMOUNT"));
        userAccountDetails.setLoanAmount(rs.getDouble("LOANAMOUNT"));
        userAccountDetails.setAvailableBalance(userAccountDetails.getBalance()-userAccountDetails.getEarMarkAmount());
        userAccountDetails.setVersion(rs.getInt("VERSION"));
        userAccountDetails.setCreated_date(rs.getObject("CREATED_DATE", Date.class));
        userAccountDetails.setUpdated_date(rs.getObject("UPDATED_DATE", Date.class));
        return userAccountDetails;

    }



}
