package scripts


class FlowFactory extends AbstractFactory {
    def newInstance (FactoryBuilderSupport builder, name, value, Map attributes) {
        new Flow (name : value)
    }

    void setParent (FactoryBuilderSupport builder, Object parent, Object childFlow ) {

        if (parent instanceof Task) {
            // running as child of task its boundary for
            //add contained flow etc to 'overall steps in the process
            parent.parentProcess.steps << childFlow
        }
    }

    boolean onHandleNodeAttributes (FactoryBuilderSupport builder, Object flow, Map attributes) {
        if (attributes.source && attributes.target) {
            flow.source = attributes.source
            flow.target = attributes.target
            attributes.remove ('source')
            attributes.remove ('target')
        }
        if (attributes.id) {
            flow.id = attributes.id
            attributes.remove('id')
        } else {
            flow.id = builder.getNextId("f_")
        }
        true
    }

    boolean isLeaf() { false}

}

class Flow {
    String id
    String name
    def source
    def target
    ConditionExpression conditionExpression

    String toString() {
        StringBuffer buff = new StringBuffer()
        buff << """<sequenceFlow id="$id" """
        if (name)
            buff << """name="$name" """
        buff << """sourceRef="$source" targetRef="$target" """
        //if with condition expression expand
        if (conditionExpression) {
            buff << ">\n"
            conditionExpression.toString().eachLine {buff << "\t$it\n"}
            buff << "</sequenceFlow>"
        } else
            buff << "/>"
    }
}
