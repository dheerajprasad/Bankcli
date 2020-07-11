package com.clibank.clibank.controller;

import com.clibank.clibank.model.User;
import com.clibank.clibank.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.controller;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RestController;

import java.util.List;

//@RestController
public class TestController {
    @Autowired
    UserService userService;

  //  @GetMapping("/getUsers")
    public List<User> getusers(){
       return userService.getusers();
    }

}
