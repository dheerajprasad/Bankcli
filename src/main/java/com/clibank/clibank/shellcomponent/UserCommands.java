package com.clibank.clibank.shellcomponent;

import com.clibank.clibank.model.User;
import com.clibank.clibank.service.ConsoleServiceImpl;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
public class UserCommands {
    public UserCommands(ConsoleServiceImpl consoleServiceImpl) {
        this.consoleServiceImpl = consoleServiceImpl;
    }

    private final ConsoleServiceImpl consoleServiceImpl;

    @ShellMethod("interact with the User Directory")
        public void directory(User user){
        this.consoleServiceImpl.write("Checking with %s" , user.getUserName());
    }
}
