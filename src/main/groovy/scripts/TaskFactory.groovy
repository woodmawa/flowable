package scripts

class TaskFactory extends AbstractFactory {
    def newInstance (FactoryBuilderSupport builder, name, value, Map attributes) {
        String stem = name - "Task"  //get the task type from the node name
        def task
        switch (stem) {
            case "script" :
                task = new ScriptTask (name : value, type:stem as TaskType)
                break
            case "service" :
                task = new ServiceTask (name : value, type:stem as TaskType)
                break
            case "user" :
                task = new UserTask (name : value, type:stem as TaskType)
                break
            default :
                task = new Task (name : value, type:stem as TaskType)
                break
        }
        task
    }

    @Override
    boolean onHandleNodeAttributes (FactoryBuilderSupport builder, Object task, Map attributes) {
        if (attributes.id) {
            task.id = attributes.id
            attributes.remove('id')
        } else {
            task.id = (builder as BpmnProcessBuilder).getNextId("t_")
        }
        if (task.type == TaskType.script) {
            if (attributes.scriptFormat) {
                task.format = attributes.scriptFormat
                attributes.remove('scriptFormat')
            } else {
                task.format = ScriptType.groovy
            }
            if (attributes.script) {
                task.scriptBlock = new Script(text: attributes.script)
                attributes.remove('script')
            }
        }
        if (task.type == TaskType.service) {
            if (attributes.class || attributes.service) {
                def clazzName = attributes.'class'
                //if passed Class definition - take the full name of the Class
                if (clazzName instanceof Class)
                    clazzName = clazzName.name
                task.serviceClassForName = clazzName  //add service also
                attributes.remove('class')
            } else {
                task.serviceClass = null  //no service defined
            }
        }
        if (task.type == TaskType.user) {
            if (attributes.potentialOwner ) {
                task.potentialOwner = attributes.potentialOwner  //add service also
                attributes.remove('potentialOwner')
            } else {
                task.potentialOwner = null  //no potentialOwner set
            }
        }
        //let parent handle now
        super.onHandleNodeAttributes(builder, task, attributes)
    }

    boolean isLeaf() {
        false
        /*if (task.type == 'script')
            false
        else
            true}*/
    }

}

class Task {
    TaskType type
    String id
    String name
    String toString() {
        //do switch on taskTYpe

        """<${type}Task id="$id" name="$name" />"""
    }
}

class UserTask extends Task {
    Documentation documentation
    PotentialOwner potentialOwner
    String assignee
    String formKey
    Form form

    String toString() {
        //do switch on taskType

        StringBuffer buff = new StringBuffer()
        buff << """<${type}Task id="$id" name="$name" """
        if (assignee)
            buff << """ flowable:assignee="$assignee" """
        buff << " >\n"
        if (documentation)
            documentation.toString().eachLine { buff << "\t$it\n"}
        if (potentialOwner) {
            potentialOwner.toString().eachLine {buff << "\t$it\n"}
        }
        if (form){
            form.toString().eachLine {buff << "\t$it\n"}
        }
        buff << "</${type}Task>\n"
    }
}

class ScriptTask extends Task {
    ScriptType format
    Script scriptBlock

    String toString() {
        StringBuffer buff = new StringBuffer()
        buff << """<${type}Task id="$id" name="$name" scriptFormat="$format">""" << "\n"
        scriptBlock.toString().eachLine {buff << "\t$it\n"}
        buff << "</${type}Task>"
    }
}

class ServiceTask extends Task {
    ScriptType format
    String serviceClassForName
    String toString() {
        //do switch on taskType

        """<${type}Task id="$id" name="$name" flowable:class="$serviceClassForName" />"""
    }
}


// enumeration of available tasktypes
enum TaskType {
    script,
    service,
    user

    String toString () {
        name()
    }
}

//enumeration of script types - return in uri format
enum ScriptType {
    groovy,
    javascript

    String toString () {
        String val =  "${name()}"
        val
    }
/*        if (name==groovy)
            "groovy"
        else
            "text/x-${name()}"
    }*/
}