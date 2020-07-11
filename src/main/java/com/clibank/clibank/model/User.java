package com.clibank.clibank.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    Integer id;
    String userName;
    private Date created_date;
    private Date updated_date;
}
