package com.clibank.clibank.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLoanDetails {
    private Integer id;
    private Integer userid;
    private String account_number;
    private Double balance;
    private Double earMarkAmount;
    private Integer payToUserId;
    private Double availableBalance;
    private Integer version;
    private Date created_date;
    private Date updated_date;
}
