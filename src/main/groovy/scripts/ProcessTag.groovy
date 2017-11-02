package scripts

class ProcessFactory extends AbstractFactory {
    def newInstance (FactoryBuilderSupport builder, name, value, Map attributes) {
        def process = new Process (name: value)
        process
    }

    void setChild (FactoryBuilderSupport builder, Object process, Object child ) {
        def sourceEventId
        def targetEventId
        if (child instanceof Event && child.type == EventType.start) {
            sourceEventId = child.id
        }
        if (child instanceof Event && child.type == EventType.end) {
            targetEventId = child.id
        }
        process.steps << child
    }

    boolean onHandleNodeAttributes (FactoryBuilderSupport builder, Object process, Map attributes) {
        if (attributes.id) {
            process.id = attributes.id
            attributes.remove('id')
        } else {
            process.id = (builder as BpmnProcessBuilder).getNextId("p_")
        }
        super.onHandleNodeAttributes(builder, process, attributes )
    }

}

class Process {
    String id
    String name
    String description
    boolean isExecutable = true //default state for process
    boolean isClosed = false
    def steps = []      //list of all top level steps through process
    def events = [:]
    def eventTriggers = [:]

    String toString () {
        StringBuffer buff = new StringBuffer()
        buff << /<process id="$id" name="$name">/ << "\n"
        for (step in steps) {
            def tag = step.toString()
            tag.eachLine { buff << "\t$it\n"}   //each tag may be multiline string
        }
        buff << "</process>"
        buff.toString()
    }
}
