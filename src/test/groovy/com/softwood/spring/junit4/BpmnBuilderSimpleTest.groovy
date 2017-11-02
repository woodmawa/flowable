package com.softwood.spring.junit4

import org.flowable.bpmn.converter.BpmnXMLConverter
import org.flowable.engine.FormService
import org.flowable.engine.HistoryService
import org.flowable.engine.ProcessEngine
import org.flowable.engine.RepositoryService
import org.flowable.engine.RuntimeService
import org.flowable.engine.TaskService
import org.flowable.engine.common.api.io.InputStreamProvider
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

import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamReader

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
        bpmn.definition('start -> end', id: 'def_1') {
            process("builderScriptProcess", id:"p_1") {
                start("startProc", id:"e_1")
                flow ('transition', source:"e_1", target:"e_2")
                end ("endproc", id: "e_2")
            }
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
        bpmn.definition('proceess with script node', id:'def_2'){
            process("builderScriptProcess", id:"p_2") {
                start("startProc", id:"start")
                flow ('transition', source:"start", target:"st_1")
                scriptTask ('myScript', id:'st_1') {
                    script ("""out:println 'hello script'""")
                }
                flow ('transition', source:"st_1", target:"end")
                end ("endproc", id: "end")
            }
        }

        println "process: \n ${bpmn.toString()}"

        DeploymentBuilder depBldr = repositoryService.createDeployment()
        depBldr.name"named deployment"
        depBldr.addInputStream("builderWithScriptTagProcess.bpmn20.xml", bpmn.exportAsInputStream())
        Deployment myDeployment = depBldr.deploy()
        assert myDeployment.id != null

        //query for deployments
        List deployments = repositoryService.createDeploymentQuery()
                .orderByDeploymentName()
                .asc()
                .list()
        assert deployments.size() > 0

        //get single process from deployment (note .processDefinitionId () gets concatenated with version etc to string , so use key
        ProcessDefinitionQuery pquery = repositoryService.createProcessDefinitionQuery().processDefinitionKey('p_2')
        ProcessDefinition p2 = pquery.singleResult()
        assert p2

        //creat instance and let it run with one script node
        ProcessInstance pi = flowableSpringRule.runtimeService.startProcessInstanceById(p2.id)

         HistoryService hs = flowableSpringRule.getHistoryService()

        // Start-task should be added to history
        HistoricActivityInstance historicStartEvent = hs.createHistoricActivityInstanceQuery().processInstanceId(pi.id).activityId("start").singleResult()
        assert historicStartEvent.activityName == "startProc"
        assert historicStartEvent.id != null
        println "-> : " + historicStartEvent

        List<HistoricActivityInstance> hiList = hs.createHistoricActivityInstanceQuery().processInstanceId(pi.id).list()
        assert hiList?.size() == 3

        List<HistoricDetail> hdList = hs.createHistoricDetailQuery().orderByProcessInstanceId().asc().list()//.processInstanceId(pi.id).list()
        println "query details : " + hdList

    }

    @Test
    void GroovyBPMBuilderLoadInitialParamsForScriptNodeTest() {

        BpmnProcessBuilder bpmn = new BpmnProcessBuilder()
        bpmn.definition ('process started with params at startup', id : "def_3") {
            process("builderScriptProcessWithParams", id:"p_3") {
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
        }

        println "process: \n ${bpmn.toString()}"

        DeploymentBuilder depBldr = repositoryService.createDeployment()
        depBldr.name"named deployment"
        depBldr.addInputStream("builderWithScriptTagProcess.bpmn20.xml", bpmn.exportAsInputStream())
        Deployment myDeployment = depBldr.deploy()
        assert myDeployment.id != null

        //query for deployments
        List deployments = repositoryService.createDeploymentQuery()
                .orderByDeploymentName()
                .asc()
                .list()
        assert deployments.size() > 0

        //get single process from deployment
        ProcessDefinitionQuery pquery = repositoryService.createProcessDefinitionQuery().processDefinitionKey('p_3')
        ProcessDefinition p3 = pquery.singleResult()
        assert p3

        //manually build some params to inject at start of the process - idx default is sorted var name
        Map procVar = new HashMap()
        procVar.name = "william"
        procVar.income = 1001
        procVar.loanAmount = 100
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
    void GroovyBPMBuilderServiceNodeTest() {

        def serv = new com.softwood.services.BpmnTestService()
        def servClass = com.softwood.services.BpmnTestService.class.name

        BpmnProcessBuilder bpmn = new BpmnProcessBuilder()
        bpmn.definition ('process with script and service task', id:'def_4') {
            process("builderScriptServiceNode", id:"p_4") {
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
        }

        println "process: \n ${bpmn.toString()}"

        DeploymentBuilder depBldr = repositoryService.createDeployment()
        depBldr.name"named deployment"
        depBldr.addInputStream("builderScriptServiceNode.bpmn20.xml", bpmn.exportAsInputStream())
        Deployment myDeployment = depBldr.deploy()
        assert myDeployment.id != null

        //query for deployments
        List deployments = repositoryService.createDeploymentQuery()
                .orderByDeploymentName()
                .asc()
                .list()
        assert deployments.size() > 0

        //get single process from deployment
        ProcessDefinitionQuery pquery = repositoryService.createProcessDefinitionQuery().processDefinitionKey('p_4')
        ProcessDefinition p4 = pquery.singleResult()
        assert p4

        //manually build some params to inject at start of the process - idx default is sorted var name
        Map procVar = new HashMap()
        procVar.creditCheck = true
        procVar.name = "william"
        procVar.income = 1001.01
        procVar.loan = 100.45
        procVar.email = "will.woodman@btinternet.com"

        //creat instance and let it run with one script node and starting variables
        ProcessInstance pi = flowableSpringRule.runtimeService.startProcessInstanceById(p4.id, procVar)

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
        bpmn.definition('process with start form defintion, started by formService ', id:'def_5') {
            process("builderScriptStartFormInit", id:"p_5") {
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
        }

        println "process: \n ${bpmn.toString()}"

        DeploymentBuilder depBldr = repositoryService.createDeployment()
        depBldr.name"named deployment"
        depBldr.addInputStream("builderScriptStartFormInit.bpmn20.xml", bpmn.exportAsInputStream())
        Deployment myDeployment = depBldr.deploy()
        assert myDeployment.id != null

        //query for deployments
        List deployments = repositoryService.createDeploymentQuery()
                .orderByDeploymentName()
                .asc()
                .list()
        assert deployments.size() > 0

        //get single process from deployment
        ProcessDefinitionQuery pquery = repositoryService.createProcessDefinitionQuery().processDefinitionKey('p_5')
        ProcessDefinition p5 = pquery.singleResult()
        assert p5

        //manually build some params to inject at start of the process - idx default is sorted var name
        FormService fs = flowableSpringRule.formService
        List<FormProperty> formList = fs.getStartFormData(p5.id).getFormProperties()
        assert formList.size() == 4

        //present form props as strings - internal convertion to right types is handling in the API
        Map initFormProps = new HashMap()
        initFormProps.name = "william"
        initFormProps.loan = "100"
        initFormProps.income = "10,000.50"
        initFormProps.creditCheck = "true"

        //submit form and start the process
        fs.submitStartFormData(p5.id, initFormProps)

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

    @Test
    void GroovyBPMBuilderStartWithMessageInvocationTest() {

        BpmnProcessBuilder bpmn = new BpmnProcessBuilder()
        bpmn.definition ("builderScriptStartByMessage", id:"def_6") {
            message ("the payment", id:"payment")
            process ("Message Started Process", id:'p_6') {
                start("start of Process", id: "start") {
                    messageEventDefinition(messageRef: "payment")
                }
                flow('transition', source: "start", target: "end")
                end("endproc", id: "end")
            }
        }

        println "process: \n ${bpmn.toString()}"

        BpmnXMLConverter xmlConvertor = new BpmnXMLConverter()

        XMLInputFactory inputFactory = XMLInputFactory.newInstance()
        XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(bpmn.exportAsInputStream())

        xmlConvertor.validateModel(xmlReader)

        DeploymentBuilder depBldr = repositoryService.createDeployment()
        depBldr.name"named deployment"
        depBldr.addInputStream("builderScriptStartMessageStart.bpmn20.xml", bpmn.exportAsInputStream())
        Deployment myDeployment = depBldr.deploy()
        assert myDeployment.id != null

        //query for deployments
        List deployments = repositoryService.createDeploymentQuery()
                .orderByDeploymentName()
                .asc()
                .list()
        assert deployments.size() > 0


        Deployment deployment = repositoryService.createDeploymentQuery().processDefinitionKey('p_6').singleResult()

        //get single process from deployment with event subscription matching on the message.name
        ProcessDefinitionQuery pquery = repositoryService.createProcessDefinitionQuery().messageEventSubscriptionName("the payment")
        ProcessDefinition p6 = pquery.singleResult()
        assert p6

        //manually build some params to inject at start of the process - idx default is sorted var name
        RuntimeService rs = flowableSpringRule.runtimeService

        //present form props as strings - internal convertion to right types is handling in the API
        Map initProps = new HashMap()
        initProps.name = "william"
        initProps.loan = "100"
        initProps.income = "10,000.50"
        initProps.creditCheck = "true"

        //start process by invoking a message using the message.name, not the id - business key stored as attribute on proc
        //note: you have to start the message by its name, not its id
        def procInst = rs.startProcessInstanceByMessage("the payment", "the business key", initProps)

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

    @Test
    void GroovyBPMBuilderStartWithSignalInvocationTest() {

        BpmnProcessBuilder bpmn = new BpmnProcessBuilder()
        bpmn.definition ("builderScriptStartByMessage", id:"def_7") {
            signal ("the signal", id:"mySignal")
            process ("Message Started Process", id:'p_7') {
                start("start of Process", id: "start") {
                    signalEventDefinition(messageRef: "mySignal")
                }
                flow('transition', source: "start", target: "end")
                end("endproc", id: "end") {
                    terminateEventDefinition (terminateAll: true)  //tried this and works
                }
            }
        }

        println "process: \n ${bpmn.toString()}"

        BpmnXMLConverter xmlConvertor = new BpmnXMLConverter()

        XMLInputFactory inputFactory = XMLInputFactory.newInstance()
        XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(bpmn.exportAsInputStream())

        xmlConvertor.validateModel(xmlReader)

        DeploymentBuilder depBldr = repositoryService.createDeployment()
        depBldr.name"named deployment"
        depBldr.addInputStream("builderScriptStartMessageStart.bpmn20.xml", bpmn.exportAsInputStream())
        Deployment myDeployment = depBldr.deploy()
        assert myDeployment.id != null

        //query for deployments
        List deployments = repositoryService.createDeploymentQuery()
                .orderByDeploymentName()
                .asc()
                .list()
        assert deployments.size() > 0


        Deployment deployment = repositoryService.createDeploymentQuery().processDefinitionKey('p_7').singleResult()

        //get single process from deployment with event subscription matching on the message.name
        ProcessDefinitionQuery pquery = repositoryService.createProcessDefinitionQuery().processDefinitionKey('p_7')
        ProcessDefinition p7 = pquery.singleResult()
        assert p7

        //manually build some params to inject at start of the process - idx default is sorted var name
        RuntimeService rs = flowableSpringRule.runtimeService

        //present form props as strings - internal convertion to right types is handling in the API
        Map initProps = new HashMap()
        initProps.name = "william"
        initProps.loan = "100"
        initProps.income = "10,000.50"
        initProps.creditCheck = "true"

        //start process by invoking a message using the message.name, not the id - business key stored as attribute on proc
        //note: you have to start the message by its name, not its id
        //def procInst = rs.startProcessInstanceByMessage("the payment", "the business key", initProps)
        def procInst = rs.signalEventReceived("the signal",initProps)
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

    @Test
    void GroovyBPMBuilderUserTaskBoundaryEventWithTimerTest() {

        BpmnProcessBuilder bpmn = new BpmnProcessBuilder()
        bpmn.definition ("builderScriptUsertaskWithBoundaryAndTimerInterrupt", id:"def_8") {
            process ("Message Started Process", id:'p_8') {
                start("start of Process", id: "start") {
                }
                flow(source: "start", target: "waitingForCallback")
                userTask ("waiting for callback", id: 'waitingForCallback') {
                    boundaryEvent (id:'escalationTimeout', cancelActivity:true, attachedTo:'waitingForCallback') {
                        timerEventDefinition (duration: 'PT1S') //wait 1 seconds
                    }
                    flow(source: "escalationTimeout", target: "escalateScript")
                    scriptTask ("escalated", id:'escalateScript') {
                        script """out:println "escalation timeout fired - get out" """
                    }
                    flow(source: "escalateScript", target: "end")

                }
                flow(source: "waitingForCallback", target: "end")


                end("endproc", id: "end")
            }
        }

        println "process: \n ${bpmn.toString()}"

        BpmnXMLConverter xmlConvertor = new BpmnXMLConverter()

        XMLInputFactory inputFactory = XMLInputFactory.newInstance()
        XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(bpmn.exportAsInputStream())

        xmlConvertor.validateModel(xmlReader)

        DeploymentBuilder depBldr = repositoryService.createDeployment()
        depBldr.name"named deployment"
        depBldr.addInputStream("builderScriptUsertaskWithBoundaryAndTimerInterrupt.bpmn20.xml", bpmn.exportAsInputStream())
        Deployment myDeployment = depBldr.deploy()
        assert myDeployment.id != null

        //query for deployments
        List deployments = repositoryService.createDeploymentQuery()
                .orderByDeploymentName()
                .asc()
                .list()
        assert deployments.size() > 0


        Deployment deployment = repositoryService.createDeploymentQuery().processDefinitionKey('p_8').singleResult()

        //get single process from deployment with event subscription matching on the message.name
        ProcessDefinitionQuery pquery = repositoryService.createProcessDefinitionQuery().processDefinitionKey('p_8')
        ProcessDefinition p8 = pquery.singleResult()
        assert p8

        //manually build some params to inject at start of the process - idx default is sorted var name
        RuntimeService rs = flowableSpringRule.runtimeService

        //present form props as strings - internal convertion to right types is handling in the API
        Map initProps = new HashMap()
        initProps.name = "william"
        initProps.loan = "100"
        initProps.income = "10,000.50"
        initProps.creditCheck = "true"

        //start process by invoking a message using the message.name, not the id - business key stored as attribute on proc
        //note: you have to start the message by its name, not its id
        //def procInst = rs.startProcessInstanceByMessage("the payment", "the business key", initProps)
        def procInst = rs.startProcessInstanceByKey("p_8",initProps)
        sleep (2000) //give time for escalation to fire
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
