package com.clibank.clibank.constants;

public enum PaymentTransactionTypes {
        PAYMENT_TRANCTION_SUCESS("000"),
        DEBIT_TRANFAILURE("001"),
        DEBIT_SUCCESS_CREDIT_FAILURE("002"),
        DEBIT_SUCCESS_CREDIT_FAILURE_DEBIT_REVERT_FAILURE("003"),
        DEBIT_SUCCESS_CREDIT_SUCCESS_TRANSACTION_CREATE_FAUIRE("004"),
        DEBIT_SUCCESS_CREDIT_SUCCESS_TRANSACTION_CREATE_FAUIRE_DEBIT_REVERT_FAILURE("005"),
        DEBIT_SUCCESS_CREDIT_SUCCESS_TRANSACTION_CREATE_FAUIRE_DEBIT_REVERT_SUCCESS_CREDIT_REVERT_FAILURE("006"),
        LOAN_TRANSACTION_SUCCESS("007"),
        LOAN_UPDATE_AMOUNT_FAIURE("008"),
    LOAN_UPDATE_SUCCESS_TRAN_INS_FAILURE_LOAN_REVERT_SUCCESS("009"),
    LOAN_UPDATE_SUCCESS_TRAN_INS_FAILURE_LOAN_REVERT_FAILURE("010"),
        INVALID_PAYMENT_TRANSACTION_NO_DEBIT_BALANCE("011"),
    INVALID_PAYMENT_TRANSACTION_NO_LOAN_ALLOWED_EXISTING_LOAN_PRESENT("011"),
    PAYMNET_AND_LOAN_SUCCESS("012"),
    LOAN_SUCCESS_PAYMNET_FAILURE("013"),
    LOAN_SUCCESS_PAYMNET_FAILURE_REVERT_LOAN_AMT_FAILURE("013"),
    TOP_UP_FAILURE("014"),
    TOP_UP_SUCCESS("015"),
    TOP_UP_SUCCESS_LOANPAYMENT_SUCCESS("015"),
    TOP_UP_SUCCESS_LOANPAYMENT_SUCCESS_UPDATE_LOAN_AMOUNT_FAILURE("016"),
    TOP_UP_SUCCESS_LOAN_REPAYMENT_FAILURE("017"),
    PAYMENT_TO_LOAN_SUCCESS("018"),
    PAYMENT_TO_LOAN_SUCCESS_TRANSACTION_CREATION_FAILURE("019"),
    PAYMENT_TO_LOAN_SUCCESS_TRANSACTION_CREATION_FAILURE_REVERT_LOANAMOUNT_FAILURE("019"),
    PAYMENT_DEBIT_SUCCESS_CREDIT_SUCCESS_DEBIT_EARMARK_REMOVE_FAILURE_CREDIT_REVERT_SUCCESS("021"),
    PAYMENT_DEBIT_SUCCESS_CREDIT_SUCCESS_DEBIT_EARMARK_REMOVE_FAILURE_CREDIT_REVERT_FAILURE("022"),
    DEBIT_SUCCESS_CREDIT_SUCCESS_TRANSACTION_CREATE_FAUIRE_CREDIT_REVERT_FAILURE("023"),
    DEBIT_SUCCESS_CREDIT_SUCCESS_TRANSACTION_CREATE_FAUIRE_CREDIT_REVERT_SUCCESS_DEBIT_REVERT_FAILURE("024"),
    DEBIT_SUCCES_CREDIT_SUCCESS_TRAN_CREATE_FAILURE_CREDIT_REVERT_SUCCESS_DEBIT_REVERT_SUCCESS("025"),
    DEBIT_SUCCESS_CREDIT_SUCCESS_TRNRECORD_CREATE_SUCCESS_REMOVE_DEBIT_EAR_MARK_FAIURE_TRAN_SUCCESS("026"),
    LOAN_REPAY_DISPUTE_PRENDING("027"),
    LOAN_REPAY_UPDATE_LOAN_AMOUNT_FAILURE("028"),
    PAYMENT_INITIATED_WITH_LOAN_AND_PAYMENT_SUCCESS_LOAN_FAILURE("029"),
    LOAN_REPAYMENT_NOT_ALLOWED_FOR_USER("030"),
    LOAN_UPDATE_SUCCESS_TRAN_CREATE_SUCCESS_LOAN_LOCK_REVERT_FAILURE("031");



private String code;

    private PaymentTransactionTypes(String code){
            this.code = code;
    }

    public String value(){
            return  this.code;
    }


}
