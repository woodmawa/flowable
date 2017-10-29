package com.softwood

import org.flowable.engine.ProcessEngines
import org.flowable.engine.RuntimeService
import org.flowable.engine.TaskService
import org.flowable.engine.test.Deployment
import org.flowable.engine.test.FlowableRule
import org.flowable.task.api.Task
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
// import assert static methods

import static org.junit.Assert.*

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:org/flowable/spring/junit4/springTypicalUsageTest-context.xml")
class MyProcessTest {

    @Rule
    public FlowableRule flowableRule = new FlowableRule()

    @Test
    @Deployment
    void ruleUsageExample () {
        RuntimeService runtimeService = flowableRule.getRuntimeService()

        /*runtimeService.startProcessInstanceByKey("ruleUsage")

        TaskService taskService = flowableRule.getTaskService()
        Task task = taskService.createTaskQuery().singleResult()
        assertEquals("My Task", task.getName())

        taskService.complete(task.getId());
        assertEquals(0, runtimeService.createProcessInstanceQuery().count())*/
    }
}
