package com.softwood.spring.junit4

import org.flowable.engine.ProcessEngines

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

import org.flowable.engine.ProcessEngine
import org.flowable.engine.RuntimeService
import org.flowable.engine.TaskService
import org.flowable.engine.test.Deployment
import org.flowable.engine.test.FlowableRule
import org.flowable.task.api.Task
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

/**
 * @author Will Woodman
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:org/flowable/spring/junit4/springTypicalUsageTest-context.xml")
public class SpringJunit4Test {

    @Autowired
    private ProcessEngine processEngine

    @Autowired
    private RuntimeService runtimeService

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
    @Deployment (resources=["com/softwood/spring/junit4/SpringJunit4Test.simpleProcessTest.bpmn20.xml"])
    public void testGetEngineFromCache() {
        //default engine uses flowable.cfg.xml on classpath to construct an engine instance
        assertNotNull(ProcessEngines.getDefaultProcessEngine())
        assertNotNull(ProcessEngines.getProcessEngine("default"))
    }

    @Test
    @Deployment
    public void simpleProcessTest() {
        runtimeService.startProcessInstanceByKey("simpleProcess");
        Task task = taskService.createTaskQuery().singleResult();
        assertEquals("My Task", task.getName());

        // ACT-1186: ActivitiRule services not initialized when using
        // SpringJUnit4ClassRunner together with @ContextConfiguration
        assertNotNull(flowableSpringRule.getRuntimeService());

        taskService.complete(task.getId());
        assertEquals(0, runtimeService.createProcessInstanceQuery().count());

    }

}