package scripts

class EventFactory extends AbstractFactory {

    boolean leaf = true

    def newInstance (FactoryBuilderSupport builder, name, value, Map attributes) {
        def event
        String stemName = name - "Event"
        switch (stemName) {
            case 'boundary' :
                event = new BoundaryEvent (name : value, type:stemName as EventType)
                break
            default :
                event = new Event (name : value, type:stemName as EventType)
                if (name.startsWith ('start'))
                    leaf = false
                break
        }
        event
    }

    void setParent (FactoryBuilderSupport builder, Object process, Object childEvent ) {
        process.events << ["${childEvent.type}Event": childEvent]
    }

    @Override
    boolean onHandleNodeAttributes (FactoryBuilderSupport builder, Object event, Map attributes) {
        if (attributes.id) {
            event.id = attributes.id
            attributes.remove('id')
        } else {
            event.id = builder.getNextId("e_")
        }
        if (attributes.initiator){
            event.initiator = attributes.initiator
            attributes.remove('initiator')
        }
        if (attributes.formKey){
            event.formKey = attributes.formKey
            attributes.remove('formKey')
        }
        if (attributes.attachedTo) {
            event.attachedToRef = attributes.attachedTo
            attributes.remove ('attachedTo')
        }
        if (attributes.duration) {
            event.duration = attributes.duration
            attributes.remove('duration')
        }
        if (attributes.cancelActivity) {
            event.cancelActivity = attributes.cancelActivity as boolean
            attributes.remove ('cancelActivity')
        }

        super.onHandleNodeAttributes(builder, event, attributes )
    }

    boolean isLeaf() {
        leaf
    }

}

class Event {
    EventType type
    String id
    String name
    String initiator
    //optional tags if forms being used
    String formKey  //external form template definition
    Form form   //set if bpmn extensionElement form being used

    String toString() {
        StringBuffer buff = new StringBuffer()
        buff << """<${type}Event id="$id" name="$name" """
        if (initiator)
            buff << """flowable:initiator="$initiator" """
        if (formKey)
            buff << """flowable:formKey:"$formKey" />"""
        else if (formKey == null && form) {
            buff << ">\n"
            form.toString().eachLine {buff << "\t" << "$it\n"}
            buff << "</${type}Event>"
        } else {
            buff << "/>"
        }
        buff
    }
}

class BoundaryEvent extends Event {
    boolean cancelActivity
    String attachedToRef
    String duration

    String toString() {
        StringBuffer buff = new StringBuffer()
        buff << """<${type}Event id="$id" """
        if (name)
            buff << """name="$name" """
        buff << """cancelActivity="$cancelActivity" """
        buff << """attachedToRef="$attachedToRef" >""" << "\n"
        buff << "\t<timerEventDefinition>\n"  //revise
        buff << "\t\t<timeDuration>$duration</timeDuration>\n"
        buff << "\t<timerEventDefinition>\n"
        buff << "</${type}Event>"
    }

}


enum EventType {
    start,
    end,
    intermediateCatch,
    boundary

    String toString () {
        name()
    }
}

//factor for event definition stanza's one of timer,message,signal,error,terminate,cancel
//takes form 'eventDefinition (name)'
class EventDefinitionFactory extends AbstractFactory {

    boolean leaf = true

    def newInstance(FactoryBuilderSupport builder, name, value, Map attributes) {
        String stemName = name - "EventDefinition"
        if (stemName == "timer")
            leaf = false       //has children
        def eventDefinition
        eventDefinition = new EventDefinition(name: name, type: stemName as EventDefinitionType, leaf: leaf)

        eventDefinition
    }

    /*void setParent(FactoryBuilderSupport builder, Object event, Object childDefinitionEvent) {
        process.events << ["name": childEvent]
    }*/

    @Override
    boolean onHandleNodeAttributes (FactoryBuilderSupport builder, Object eventDefinition, Map attributes) {
        if (attributes.id) {
            eventDefinition.id = attributes.id
            attributes.remove('id')
        } else {
            eventDefinition.id = builder.getNextId("ed_")
        }
        if (attributes.errorRef){
            eventDefinition.eventRef = attributes.errorRef
            attributes.remove('errorRef')
        }
        if (attributes.messageRef) {
            eventDefinition.eventRef = attributes.messageRef
            attributes.remove ('messageRef')
        }
        if (attributes.signalRef) {
            eventDefinition.eventRef = attributes.signalRef
            attributes.remove('signalRef')
        }
        if (attributes.terminateAll) {
            eventDefinition.terminateAll = attributes.terminateAll
            attributes.remove('terminateAll')
        }

        /*if (attributes.cancelActivity) {
            eventDefinition.cancelActivity = attributes.cancelActivity as boolean
            attributes.remove ('cancelActivity')
        }*/

        super.onHandleNodeAttributes(builder, eventDefinition, attributes )
    }
}

//handles nested eventDefinition clauses
class EventDefinition {
    EventDefinitionType definitionType
    String id
    String name
    String eventRef
    boolean terminateAll
    String businessCalendarName
    boolean leaf  //has nochildren
    //TimerDefinition timerBlock

    String toString () {
        StringBuffer buff = new StringBuffer ()
        buff << "<${definitionType}EventDefinition "
        if (id)
            buff << """id="$id" """
        if (eventRef)
            buff << "${definitionType}" << """Ref="${eventRef}" """
        if (terminateAll)
            buff << """flowable:terminateAll="$terminateAll" """
        if (leaf)
            buff "/>"

        /*if (timerBlock) {
            timerBlock.toString().eachLine {buff << "\t$it\n"}
        }*/
        else
            buff << ">\n</${definitionType}EventDefinition>"
    }
}

enum EventDefinitionType {
    message,
    timer,
    signal,
    error,
    terminate,
    cancel

    String toString () {
        name()
    }
}

//factory class for signal, message and error type blocks
class EventTriggerFactory extends AbstractFactory {

    boolean leaf = true

    def newInstance (FactoryBuilderSupport builder, name, value, Map attributes) {
        def eventTrigger
        switch (name) {
            case 'message' :
                eventTrigger = new Message (name : value, type:name )
                break
            case 'signal' :
                eventTrigger = new Signal (name : value, type:name )
                break
            case 'error' :
                eventTrigger = new Error (name : value, type:name )
                break
            default :
                //exception - event = new Event (name : value, type:name )
                break
        }
        eventTrigger
    }

    void setParent (FactoryBuilderSupport builder, Object definition, Object eventTrigger ) {
        //store trigger instance with name on builder as top level entity along with process
        definition.eventTriggers << eventTrigger
    }

    @Override
    boolean onHandleNodeAttributes (FactoryBuilderSupport builder, Object event, Map attributes) {
        if (attributes.id) {
            event.id = attributes.id
            attributes.remove('id')
        } else {
            event.id = builder.getNextId("et_")
        }

        super.onHandleNodeAttributes(builder, event, attributes )
    }

    boolean isLeaf() {
        leaf
    }

}

//covers signal, message and error type blocks
abstract class EventTrigger {
    String id
    String name
    EventTriggerType type

}
//top level process/event  triggers
class Signal extends EventTrigger {
    String toString() {
        StringBuffer buff = new StringBuffer()
        buff << """<signal id="$id" """
        if (name)
            buff << """name="$name """
        buff << "/>"
    }
}

class Message extends EventTrigger{
    String toString() {
        StringBuffer buff = new StringBuffer()
        buff << """<message id="$id" """
        if (name)
            buff << """name="$name """
        buff << "/>"
    }
}

class Error extends EventTrigger {
    String errorCode

    String toString() {
        StringBuffer buff = new StringBuffer()
        buff << """<error id="$id" """
        if (name)
            buff << """name="$name """
        if (errorCode)
            buff << """errorCode="$errorCode" """
        buff << "/>"
    }
}

enum EventTriggerType {
    message,
    signal,
    error

    String toString () {
        name()
    }
}
