package com.clibank.clibank.promptprovider;

import com.clibank.clibank.service.UserService;
import org.jline.utils.AttributedString;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

@Component
public class ConnectedPromptProvider implements PromptProvider {
    private final UserService userService;


    public ConnectedPromptProvider(UserService userService) {
        this.userService = userService;
    }


    @Override
    public AttributedString getPrompt() {
        String msg  = String.format("Banking cli (%s)>", this.userService.isConnected() ? "Logged in  as " +  this.userService.getLoggedInuser().getUserName() : "Not Logged in");
        return new AttributedString(msg);
    }
}
