package com.softwood.spring.junit4

import org.flowable.engine.FormService
import org.flowable.engine.HistoryService
import org.flowable.engine.ProcessEngine
import org.flowable.engine.RepositoryService
import org.flowable.engine.RuntimeService
import org.flowable.engine.TaskService
import org.flowable.engine.form.FormProperty
import org.flowable.engine.history.HistoricActivityInstance
import org.flowable.engine.history.HistoricDetail
import org.flowable.engine.history.HistoricDetailQuery
import org.flowable.engine.history.HistoricProcessInstance
import org.flowable.engine.history.HistoricProcessInstanceQuery
import org.flowable.engine.history.HistoricVariableUpdate
import org.flowable.engine.repository.Deployment
import org.flowable.engine.repository.DeploymentBuilder
import org.flowable.engine.repository.Model
import org.flowable.engine.repository.ModelQuery
import org.flowable.engine.repository.ProcessDefinition
import org.flowable.engine.repository.ProcessDefinitionQuery
import org.flowable.engine.runtime.ProcessInstance
import org.flowable.engine.runtime.ProcessInstanceBuilder
import org.flowable.engine.test.FlowableRule
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import scripts.BpmnBuilder
import scripts.BpmnProcessBuilder

/**
 * @author Will Woodman
 */
@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration("classpath:org/flowable/spring/junit4/springTypicalUsageTest-context.xml")
@ContextConfiguration("classpath:org/flowable/spring/junit4/springFullHistoryUsageTest-context.xml")
class BpmnBuilderSimpleTest {
    @Autowired
    private ProcessEngine processEngine

    @Autowired
    private RuntimeService runtimeService

    @Autowired
    private RepositoryService repositoryService

    @Autowired
    private TaskService taskService

    @Autowired
    @Rule
    public FlowableRule flowableSpringRule

    @After
    public void closeProcessEngine() {
        // Required, since all the other tests seem to do a specific drop on the
        // end
        processEngine.close()
    }

    @Test
    void GroovyBPMBuilderSimpleTest() {

        BpmnProcessBuilder bpmn = new BpmnProcessBuilder()
        bpmn.process("builderScriptProcess", id:"p_1") {
            start("startProc", id:"e_1")
            flow ('transition', source:"e_1", target:"e_2")
            end ("endproc", id: "e_2")
        }

        //bpmn.exportToBPMN("xml", "com${File.separatorChar}softwood${File.separatorChar}spring${File.separatorChar}junit4", "BuilderScriptProcess")

        DeploymentBuilder depBldr = repositoryService.createDeployment()
        depBldr.name"named deployment"
        depBldr.addInputStream("builderScriptProcess.bpmn20.xml", bpmn.exportAsInputStream())
        Deployment myDeployment = depBldr.deploy()
        assert myDeployment.id == "1"

        //query for deployments
        List deployments = repositoryService.createDeploymentQuery()
        .orderByDeploymentName()
        .asc()
        .list()
        assert deployments.size() == 1

        //get single process from deployment
        ProcessDefinitionQuery pquery = repositoryService.createProcessDefinitionQuery()
        ProcessDefinition p1 = pquery.singleResult()
        assert p1

        //this but doesnt work - not sure how/why you get a model
        //Model model = repositoryService.getModel(m1.id)
        //assert model
        //A model is a container for meta data and sources of a process model?? - typically edited in modeling env
        //ModelQuery mquery = repositoryService.createModelQuery()
        //def modelsNum = mquery.count()
        //Model m1 = mquery.singleResult()

        //get in memory ref to parsed model
        org.flowable.bpmn.model.BpmnModel bpmnModel = repositoryService.getBpmnModel(p1.id)
        assert bpmnModel
    }

    @Test
    void GroovyBPMBuilderScriptNodeTest() {

        BpmnProcessBuilder bpmn = new BpmnProcessBuilder()
        bpmn.process("builderScriptProcess", id:"p_1") {
            start("startProc", id:"start")
            flow ('transition', source:"start", target:"st_1")
            scriptTask ('myScript', id:'st_1') {
                script ("""out:println 'hello script'""")
            }
            flow ('transition', source:"st_1", target:"end")
            end ("endproc", id: "end")
        }

        println "process: \n ${bpmn.toString()}"

        DeploymentBuilder depBldr = repositoryService.createDeployment()
        depBldr.name"named deployment"
        depBldr.addInputStream("builderWithScriptTagProcess.bpmn20.xml", bpmn.exportAsInputStream())
        Deployment myDeployment = depBldr.deploy()
        assert myDeployment.id > 0

        //query for deployments
        List deployments = repositoryService.createDeploymentQuery()
                .orderByDeploymentName()
                .asc()
                .list()
        assert deployments.size() > 0

        //get single process from deployment
        ProcessDefinitionQuery pquery = repositoryService.createProcessDefinitionQuery()
        ProcessDefinition p1 = pquery.singleResult()
        assert p1

        //creat instance and let it run with one script node
        ProcessInstance pi = flowableSpringRule.runtimeService.startProcessInstanceById(p1.id)

         HistoryService hs = flowableSpringRule.getHistoryService()

        // Start-task should be added to history
        HistoricActivityInstance historicStartEvent = hs.createHistoricActivityInstanceQuery().processInstanceId(pi.id).activityId("start").singleResult()
        assert historicStartEvent.activityName == "startProc"
        assert historicStartEvent.id > 0
        println "-> : " + historicStartEvent

        List<HistoricActivityInstance> hiList = hs.createHistoricActivityInstanceQuery().processInstanceId(pi.id).list()
        assert hiList?.size() == 3

        List<HistoricDetail> hdList = hs.createHistoricDetailQuery().orderByProcessInstanceId().asc().list()//.processInstanceId(pi.id).list()
        println "query details : " + hdList

    }

    @Test
    void GroovyBPMBuilderLoadInitialParamsForScriptNodeTest() {

        BpmnProcessBuilder bpmn = new BpmnProcessBuilder()
        bpmn.process("builderScriptProcessWithParams", id:"p_2") {
            start("startProc", id:"start")
            flow ('transition', source:"start", target:"st_1")
            scriptTask ('myScript', id:'st_1') {
                script ("""out:println 'found process variables in binding'
    out: println "provVars >  name: " + name + ", email : " + email + ",  loanAmount : " + loanAmount
""")
            }
            flow ('transition', source:"st_1", target:"end")
            end ("endproc", id: "end")
        }

        println "process: \n ${bpmn.toString()}"

        DeploymentBuilder depBldr = repositoryService.createDeployment()
        depBldr.name"named deployment"
        depBldr.addInputStream("builderWithScriptTagProcess.bpmn20.xml", bpmn.exportAsInputStream())
        Deployment myDeployment = depBldr.deploy()
        assert myDeployment.id > 0

        //query for deployments
        List deployments = repositoryService.createDeploymentQuery()
                .orderByDeploymentName()
                .asc()
                .list()
        assert deployments.size() > 0

        //get single process from deployment
        ProcessDefinitionQuery pquery = repositoryService.createProcessDefinitionQuery()
        ProcessDefinition p2 = pquery.singleResult()
        assert p2

        //manually build some params to inject at start of the process - idx default is sorted var name
        Map procVar = new HashMap()
        procVar.name = "william"
        procVar.income = 1001
        procVar.loanAmount = 100
        procVar.email = "will.woodman@btinternet.com"

        //creat instance and let it run with one script node and starting variables
        ProcessInstance pi = flowableSpringRule.runtimeService.startProcessInstanceById(p2.id, procVar)

        HistoryService hs = flowableSpringRule.getHistoryService()

        List<HistoricDetail> histVar = hs.createHistoricDetailQuery()
                .variableUpdates()
                .orderByVariableName()
                .asc().list()//.processInstanceId(pi.id).list()

        assert histVar != null
        histVar.eachWithIndex {var, idx -> println "idx: $idx -> ${var.variableName}"}
        HistoricVariableUpdate histvar = histVar.get (3)

        println "var (3) name: ${histvar.variableName}, value:${histvar.value}, type:${histvar.variableTypeName}"

    }

    @Test
    void GroovyBPMBuilderServiceNodeTest() {

        def serv = new com.softwood.services.BpmnTestService()
        def servClass = com.softwood.services.BpmnTestService.class.name

        BpmnProcessBuilder bpmn = new BpmnProcessBuilder()
        bpmn.process("builderScriptServiceNode", id:"p_3") {
            start("startProc", id:"start")
            flow ('transition', source:"start", target:"loanProcess")
            serviceTask ('loan Process', id: "loanProcess", class: com.softwood.services.BpmnTestService )
            flow ('transition', source:"loanProcess", target:"script1")
            scriptTask ('printLoanDetail', id:'script1') {
                script ("""out:println 'loan details : ' + loanDetail
""")
            }
            flow ('transition', source:"script1", target:"end")
            end ("endproc", id: "end")
        }

        println "process: \n ${bpmn.toString()}"

        DeploymentBuilder depBldr = repositoryService.createDeployment()
        depBldr.name"named deployment"
        depBldr.addInputStream("builderScriptServiceNode.bpmn20.xml", bpmn.exportAsInputStream())
        Deployment myDeployment = depBldr.deploy()
        assert myDeployment.id > 0

        //query for deployments
        List deployments = repositoryService.createDeploymentQuery()
                .orderByDeploymentName()
                .asc()
                .list()
        assert deployments.size() > 0

        //get single process from deployment
        ProcessDefinitionQuery pquery = repositoryService.createProcessDefinitionQuery()
        ProcessDefinition p3 = pquery.singleResult()
        assert p3

        //manually build some params to inject at start of the process - idx default is sorted var name
        Map procVar = new HashMap()
        procVar.creditCheck = true
        procVar.name = "william"
        procVar.income = 1001.01
        procVar.loan = 100.45
        procVar.email = "will.woodman@btinternet.com"

        //creat instance and let it run with one script node and starting variables
        ProcessInstance pi = flowableSpringRule.runtimeService.startProcessInstanceById(p3.id, procVar)

        HistoryService hs = flowableSpringRule.getHistoryService()

        List<HistoricDetail> histVar = hs.createHistoricDetailQuery()
                .variableUpdates()
                .orderByVariableName()
                .asc().list()//.processInstanceId(pi.id).list()

        assert histVar != null
        histVar.eachWithIndex {var, idx -> println "idx: $idx -> ${var.variableName}"}
        HistoricVariableUpdate histvar = histVar.get (3)

        println "var (3) name: ${histvar.variableName}, value:${histvar.value}, type:${histvar.variableTypeName}"

    }

    @Test
    void GroovyBPMBuilderStartFormTest() {

        BpmnProcessBuilder bpmn = new BpmnProcessBuilder()
        bpmn.process("builderScriptStartFormInit", id:"p_4") {
            start("startProc", id:"start"){
                form () {
                    //only form proprty types supported are string/long/enum/date/boolean
                    formProperty ("Name", id:'name', required:true, type:String)
                    formProperty ("Loan", id:'loan', required:true, type:long)
                    formProperty ("Income", id:'income', required:true, type:"string")
                    formProperty ("Credit Check", id:'creditCheck', required:true, type:"Boolean")
                }
            }
            flow ('transition', source:"start", target:"st_1")
            scriptTask ('myScript', id:'st_1') {
                script ("""out:println 'found process variables in binding'
//out: println "provVars >  Name: " + name + ", Income : " + income + ",  CreditCheck : " + creditCheck
def var = income
def str = var.class.name
//this causes "java.lang.VerifyError: (class: Script1, method: run signature: ()Ljava/lang/Object;) Stack size too large"
out: println "income type : " + str
//out: println "creditCheck type : " + creditCheck.class.name
//out: println "loan type : " + loan.class.name
""")
            }
            flow ('transition', source:"st_1", target:"end")
            end ("endproc", id: "end")
        }

        println "process: \n ${bpmn.toString()}"

        DeploymentBuilder depBldr = repositoryService.createDeployment()
        depBldr.name"named deployment"
        depBldr.addInputStream("builderScriptStartFormInit.bpmn20.xml", bpmn.exportAsInputStream())
        Deployment myDeployment = depBldr.deploy()
        assert myDeployment.id > 0

        //query for deployments
        List deployments = repositoryService.createDeploymentQuery()
                .orderByDeploymentName()
                .asc()
                .list()
        assert deployments.size() > 0

        //get single process from deployment
        ProcessDefinitionQuery pquery = repositoryService.createProcessDefinitionQuery()
        ProcessDefinition p4 = pquery.singleResult()
        assert p4

        //manually build some params to inject at start of the process - idx default is sorted var name
        FormService fs = flowableSpringRule.formService
        List<FormProperty> formList = fs.getStartFormData(p4.id).getFormProperties()
        assert formList.size() == 4

        //present form props as strings - internal convertion to right types is handling in the API
        Map initFormProps = new HashMap()
        initFormProps.name = "william"
        initFormProps.loan = "100"
        initFormProps.income = "10,000.50"
        initFormProps.creditCheck = "true"

        //submit form and start the process
        fs.submitStartFormData(p4.id, initFormProps)

        HistoryService hs = flowableSpringRule.getHistoryService()

        List<HistoricDetail> histVar = hs.createHistoricDetailQuery()
                .variableUpdates()
                .orderByVariableName()
                .asc().list()//.processInstanceId(pi.id).list()

        assert histVar != null
        histVar.eachWithIndex {var, idx -> println "idx: $idx -> ${var.variableName}"}
        HistoricVariableUpdate histvar = histVar.get (2)

        println "var (2) name: ${histvar.variableName}, value:${histvar.value}, type:${histvar.variableTypeName}"

    }

}
