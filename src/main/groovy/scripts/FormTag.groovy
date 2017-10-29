package scripts

class FormFactory extends AbstractFactory {
    def newInstance (FactoryBuilderSupport builder, name, value, Map attributes) {
        new Form ()
    }

    @Override
    void setParent (FactoryBuilderSupport builder,Object parent, Object form) {
        if (form instanceof Form)
            parent.form = form
    }
    /*void setChild (FactoryBuilderSupport builder, Object parent, Object child ) {
        if (child instanceof FormProperty) {
            parent.formProperties << child
        }

    }*/
}

class Form {
    def formProperties = []

    String toString () {
        StringBuffer buf = new StringBuffer()
        buf << "<extensionElements>\n"
        formProperties.each {buf << "\t$it\n"}
        buf << "</extensionElements>"
        buf.toString()
    }
}
