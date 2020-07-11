package com.clibank.clibank.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDetails {
    private int id;
    private int debit_userid;
    private String debit_account_id;
    private int credit_userid;
    private String credit_account_id;
    private double transaction_amount;
    private String transaction_type;
    private Date created_date;
    private Date updated_date;
}
