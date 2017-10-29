package com.softwood.spring.junit4

import org.flowable.engine.HistoryService
import org.flowable.engine.ProcessEngine
import org.flowable.engine.RepositoryService
import org.flowable.engine.RuntimeService
import org.flowable.engine.TaskService
import org.flowable.engine.history.HistoricActivityInstance
import org.flowable.engine.history.HistoricDetail
import org.flowable.engine.history.HistoricDetailQuery
import org.flowable.engine.history.HistoricProcessInstance
import org.flowable.engine.history.HistoricProcessInstanceQuery
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
}
