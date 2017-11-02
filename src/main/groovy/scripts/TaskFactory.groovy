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

    void setParent (FactoryBuilderSupport builder, Object parent, Object task ) {
        if (parent instanceof Process) {
            //parent.steps << task done in the parent for all its children
            //keep reference to process for each task here
            task.parentProcess = parent
        }
        if (parent instanceof Task) {
            // running child of task within another task            //add contained sub task  etc to 'overall steps in the process
            parent.parentProcess.steps << task
            task.parentProcess = parent.parentProcess   //task and subtask set with  same parentProcess
        }

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
            if (attributes.resultVariable) {
                task.resultVariable = attributes.resultVariable
                attributes.remove ('resultVariable')
            }

        }
        if (task.type == TaskType.service) {
            if (attributes.expression) {
                task.expression  = attributes.expression
                attributes.remove ('expression')
            }
            if (attributes.isForCompensation) {
                task.isForCompensation = attributes.isForCompensation
                attributes.remove ('isForCompensation')
            }
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
    Process ParentProcess

    String toString() {
        //do switch on taskTYpe

        """<${type}Task id="$id" name="$name" />"""
    }
}

class UserTask extends Task {
    Documentation documentation
    PotentialOwner potentialOwner
    HumanPerformer humanPerformer
    String assignee
    String candidateUsers
    String candidateGroups
    String formKey
    Form form
    String dueDate //either java.util.Date (${someDatevar}), java.util.String ISO8601, or ISO8601 time-duration

    String toString() {
        //do switch on taskType

        StringBuffer buff = new StringBuffer()
        buff << """<${type}Task id="$id" name="$name" """
        if (dueDate)
            buff << """flowable:dueDate="$dueDate" """
        if (assignee)
            buff << """flowable:assignee="$assignee" """
        if (candidateUsers)
            buff << """flowable:assignee="$candidateUsers" """
        if (candidateGroups)
            buff << """flowable:assignee="$candidateGroups" """
        if (formKey)
            buff << """flowable:formKey="$formKey" """

        buff << " >\n"
        if (documentation)
            documentation.toString().eachLine { buff << "\t$it\n"}
        if (humanPerformer) {
            humanPerformer.toString().eachLine {buff << "\t$it\n"}
        }
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
    String resultVariable

    String toString() {
        StringBuffer buff = new StringBuffer()
        buff << """<${type}Task id="$id" name="$name" scriptFormat="$format" """
        if (resultVariable)
            buff << """flowable:resultVariable="$resultVariable" """
        buff <<"\n"
        scriptBlock.toString().eachLine {buff << "\t$it\n"}
        buff << "</${type}Task>"
    }
}

class ServiceTask extends Task {
    ScriptType format
    String serviceClassForName
    boolean isForCompensation
    String expression

    String toString() {
        //do switch on taskType
        StringBuffer buff = new StringBuffer()
        buff << """<${type}Task id="$id" name="$name" """
        if (expression)
            buff << """flowable:expression="$expression" """
        if (isForCompensation)
            buff << """isForCompensation="$isForCompensation"  """
        buff """flowable:class="$serviceClassForName" """
        buff << "/>"
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