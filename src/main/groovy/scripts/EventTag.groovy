package scripts

class EventFactory extends AbstractFactory {

    boolean leaf = true

    def newInstance (FactoryBuilderSupport builder, name, value, Map attributes) {
        def event = new Event (name : value, type:name as EventType)
        if (name.startsWith ('start'))
            leaf = false
        event
    }

    void setParent (FactoryBuilderSupport builder, Object process, Object childEvent ) {
        process.events << ["${childEvent.type}Event": childEvent]
    }

    boolean onHandleNodeAttributes (FactoryBuilderSupport builder, Object event, Map attributes) {
        if (attributes.id) {
            event.id = attributes.id
            attributes.remove('id')
        } else {
            event.id = builder.getNextId("e_")
        }
        true
    }

    boolean isLeaf() {
        leaf
    }

}

class Event {
    EventType type
    String id
    String name
    Form form

    String toString() {
        StringBuffer buff = new StringBuffer()
        buff << """<${type}Event id="$id" name="$name" """
        if (form) {
            buff << ">\n"
            form.toString().eachLine {buff << "\t" << "$it\n"}
            buff << "</${type}Event>"
        } else {
            buff << "/>"
        }
        buff
    }
}

enum EventType {
    start,
    end,
    intermediateCatch

    String toString () {
        name()
    }
}