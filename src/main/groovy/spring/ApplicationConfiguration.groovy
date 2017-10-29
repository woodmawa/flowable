package spring

import org.flowable.engine.ProcessEngine
import org.flowable.engine.ProcessEngineConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * setup basic spring configuration
 */
@Configuration
class ApplicationConfiguration {
    /*@Bean
    ProcessEngineConfiguration processEngineConfiguration () {
        new org.flowable.spring.SpringProcessEngineConfiguration()
    }

    @Bean
    ProcessEngine processEngine () {
        ProcessEngine engine = new org.flowable.spring.ProcessEngineFactoryBean(processEngineConfiguration())
    }*/
}
