package com.clibank.clibank.shellcomponent;

import com.clibank.clibank.model.User;
import com.clibank.clibank.service.ConsoleService;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
public class UserCommands {
    public UserCommands(ConsoleService consoleService) {
        this.consoleService = consoleService;
    }

    private final ConsoleService consoleService;

    @ShellMethod("interact with the User Directory")
        public void directory(User user){
        this.consoleService.write("Checking with %s" , user.getUserName());
    }
}
