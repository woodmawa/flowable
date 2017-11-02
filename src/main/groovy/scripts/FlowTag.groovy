package scripts


class FlowFactory extends AbstractFactory {
    def newInstance (FactoryBuilderSupport builder, name, value, Map attributes) {
        new Flow (name : value)
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

    boolean isLeaf() { true}

}

class Flow {
    String id
    String name
    def source
    def target
    String toString() {
        StringBuffer buff = new StringBuffer()
        buff << """<sequenceFlow id="$id" """
        if (name)
            buff << """name="$name" """
        buff << """sourceRef="$source" targetRef="$target" />"""
    }
}
