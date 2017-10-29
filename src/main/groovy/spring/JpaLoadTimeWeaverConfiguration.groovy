package spring

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableLoadTimeWeaving
import org.springframework.context.annotation.LoadTimeWeavingConfigurer
import org.springframework.instrument.classloading.LoadTimeWeaver

//@Configuration
//@EnableLoadTimeWeaving
//only needed for openJpa load time weaving -wasnt working
class JpaLoadTimeWeaverConfiguration implements LoadTimeWeavingConfigurer {

        @Override
        public LoadTimeWeaver getLoadTimeWeaver() {
            def ltw = new org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver()
            //ltw.addClassTransformer(myClassFileTransformer);  - do i have one of these ??
            // ...
            return ltw
        }
}
