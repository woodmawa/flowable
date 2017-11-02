package scripts


class GatewayFactory extends AbstractFactory {
    def newInstance (FactoryBuilderSupport builder, name, value, Map attributes) {
        String stemName = name - "Gateway"
        new Gateway (name : value, type: stemName as GatewayType)
    }


    boolean onHandleNodeAttributes (FactoryBuilderSupport builder, Object gateway, Map attributes) {
        if (attributes.id) {
            gateway.id = attributes.id
            attributes.remove('id')
        } else {
            gateway.id = builder.getNextId("gw_")
        }
        if (attributes.default){
            gateway.defaultFlow = attributes.default
            attributes.remove ('default')
        }

        super.onHandleNodeAttributes(builder, gateway, attributes)
    }

    boolean isLeaf() { true}

}

class Gateway {
    String id
    String name
    GatewayType type
    String defaultFlow

    String toString() {
        StringBuffer buff = new StringBuffer()

        buff << """<${type}Gateway id="$id" """
        if (name)
            buff << """name="$name" """
        if (defaultFlow)
            buff << """default="$defaultFlow" """
        buff << "/>"
        buff
    }
}

enum GatewayType {
    exclusive,
    parallel

    String toString () {
        name()
    }
}
