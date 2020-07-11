package com.clibank.clibank.converter;

import com.clibank.clibank.model.User;
import com.clibank.clibank.service.UserService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;
@Component
public class UserConverter implements Converter<String, User> {

    public UserConverter(UserService userService) {
        this.userService = userService;
    }

    private final UserService userService;

    private final Pattern pattern = Pattern.compile("\\(#(\\d+)\\).*");

    //(#42)

    @Override
    public User convert(String s) {
        Long id = Long.parseLong(s);
        return this.userService.findById(id);
      /*  Matcher matcher = this.pattern.matcher(s);
        if(matcher.find()){
            String group = matcher.group(1);
            if(StringUtils.hasText(group)){
                Long id = Long.parseLong(group);

            }
        }
        return  null;*/

    }
}
