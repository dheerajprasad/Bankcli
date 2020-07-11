package com.clibank.clibank.repository;

import com.clibank.clibank.model.User;

public interface UserRepository {

    User getUserByName(String userName);
    User getUserByid(int userId);
    String createuser(String userName);
}
