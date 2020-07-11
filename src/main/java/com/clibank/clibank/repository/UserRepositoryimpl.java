package com.clibank.clibank.repository;

import com.clibank.clibank.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository
public class UserRepositoryimpl implements UserRepository {

    private static final String USERNAME="USERNAME";
    private static final String ID="ID";
    private static final String GET = "SELECT ID ,USERNAME FROM USER_DETAILS";
    private static final String GET_USER_BY_USERNAME = "SELECT ID ,USERNAME,CREATED_DATE,UPDATED_DATE FROM USER_DETAILS WHERE USERNAME=:USERNAME";
    private static final String GET_USER_BY_USERID = "SELECT ID ,USERNAME,CREATED_DATE,UPDATED_DATE FROM USER_DETAILS WHERE ID=:ID";
    private static final String CREATE_USER = "INSERT INTO USER_DETAILS (USERNAME,CREATED_DATE,UPDATED_DATE) VALUES (:USERNAME,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)";


    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public User getUserByName(String userName){
        Map<String,String> params = new HashMap<>();
        params.put(USERNAME,userName);
        return namedParameterJdbcTemplate.query(GET_USER_BY_USERNAME,params, (ResultSetExtractor<User>) this::getUser);

     }

    @Override
    public User getUserByid(int userId) {
        Map<String,Object> params = new HashMap<>();
        params.put(ID,userId);
        return namedParameterJdbcTemplate.query(GET_USER_BY_USERID,params, (ResultSetExtractor<User>) this::getUser);

    }

    public String createuser(String userName){
        Map<String,String> params = new HashMap<>();
        params.put(USERNAME,userName);
        namedParameterJdbcTemplate.update(CREATE_USER,params);
        return userName;

    }



    public List<User> findallusers(){
        System.out.println("findallusers" );
        return  namedParameterJdbcTemplate.query(GET,this::getUserList);
    }


    protected List<User> getUserList(ResultSet rs) throws SQLException{

        List<User> users = new ArrayList<>();
        User user = getUser(rs);
        System.out.println("users avaialble "+ user!=null  );

        while (user!=null){
            users.add(user);
           user = getUser(rs);

        }
        return users;

    }

    protected User getUser(ResultSet rs) throws SQLException{

        if(!rs.next()){
            return null;
        }

        User user = new User();
        user.setId(rs.getInt("ID"));
        user.setUserName(rs.getString("USERNAME"));
        user.setCreated_date(rs.getObject("CREATED_DATE", Date.class));
        user.setUpdated_date(rs.getObject("UPDATED_DATE", Date.class));
        return user;

    }


}
