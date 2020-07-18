package com.clibank.clibank.model;

import com.clibank.clibank.constants.PaymentTransactionTypes;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClearExistingLoanRes {

    PaymentTransactionTypes paymentTransactionTypes;
    Double remainingTranamt;
}
