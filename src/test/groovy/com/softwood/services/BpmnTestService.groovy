package com.softwood.services

import org.flowable.engine.delegate.DelegateExecution
import org.flowable.engine.delegate.JavaDelegate


//external service can that be called be invoked from workflow instances
class BpmnTestService implements JavaDelegate {

    void execute (DelegateExecution exec) {

        LoanDetails ld = new LoanDetails()

        //read and set vraiables into execution binding
        exec.with {
            //get variables from binding and build LoanDetails instance
            ld.creditCheck = getVariable('creditCheck')
            ld.customerName = getVariable ('name')
            ld.email = getVariable ('email')
            ld.income = getVariable ('income')
            ld.loan = getVariable('loan')
            //set the loanDetail instance into the proc variables binding
            setVariable ("loanDetail", ld )
        }
        //print to console during test
        println "service : created loan detail and added as process variable "
    }

}

//proc variables must be serializable to store in binding
class LoanDetails implements Serializable {
    boolean creditCheck
    String customerName
    BigDecimal income
    BigDecimal loan
    String email

    String toString () {
        "loan > customer : $customerName, email : $email, loan: $loan, creditCheck : $creditCheck, income : $income"
    }
}
