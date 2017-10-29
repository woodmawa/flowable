package com.softwood.spring.junit4

import org.flowable.engine.ProcessEngine
import org.flowable.engine.RepositoryService
import org.flowable.engine.RuntimeService
import org.flowable.engine.TaskService
import org.flowable.engine.repository.Deployment
import org.flowable.engine.repository.DeploymentBuilder
import org.flowable.engine.repository.Model
import org.flowable.engine.repository.ModelQuery
import org.flowable.engine.repository.ProcessDefinition
import org.flowable.engine.repository.ProcessDefinitionQuery
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
@ContextConfiguration("classpath:org/flowable/spring/junit4/springTypicalUsageTest-context.xml")
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
            flow ('transition', source:"e_1", target:"e-2")
            end ("endproc", id: "e_2")
        }

        println bpmn.toString()

        DeploymentBuilder depBldr = repositoryService.createDeployment()
        depBldr.name"named deployment"
        //resourcename is  name of process, stream is actual defn
        InputStream ins = bpmn.exportAsInputStream()
        depBldr.addInputStream("builderScriptProcess", ins)
        //depBldr.addClasspathResource("com/softwood/spring/junit4/BpmnBuilderSimpleTest.bpmn20.xml")
        Deployment myDeployment = depBldr.deploy()
        assert myDeployment.id == "1"
        //assert myDeployment.key

        //println  "proc def : \n" + bpmn.toString()

        //Deployment myDeployment = depBldr.addString("p_1", bpmn.toString()).deploy()

        List deployments = repositoryService.createDeploymentQuery()
        .orderByDeploymentName()
        .asc()
        .list()
        assert deployments.size() == 1

        ModelQuery mquery = repositoryService.createModelQuery()
        Model m1 = mquery.singleResult()

        ProcessDefinitionQuery pquery = repositoryService.createProcessDefinitionQuery()
        ProcessDefinition p1 = pquery.singleResult()

        Model model = repositoryService.getModel("d_1")
        //assert model

        org.flowable.bpmn.model.BpmnModel bpmnModel = repositoryService.getBpmnModel("p_1")
        assert bpmnModel
    }
}
