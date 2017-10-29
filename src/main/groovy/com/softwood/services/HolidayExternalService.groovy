package com.softwood.services

import org.flowable.engine.delegate.DelegateExecution
import org.flowable.engine.delegate.JavaDelegate


//external service can that be called be invoked from workflow instances
class HolidayExternalService implements JavaDelegate {

    void execute (DelegateExecution exec) {
        println "service : calling extrenal behaviour for employee ${exec.getVariable("employee")}"
    }

}
