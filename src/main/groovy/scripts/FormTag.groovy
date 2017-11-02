package scripts

class FormFactory extends AbstractFactory {

    boolean leaf = false

    def newInstance (FactoryBuilderSupport builder, name, value, Map attributes) {
        Form form = new Form ()
        if (value) {  //assume that parent.flowable:formKey value is being set with value = template name
            form.formKey = value
            leaf = true
        }

        return form
    }

    @Override
    void setParent (FactoryBuilderSupport builder,Object parent, Object form) {
        if (form instanceof Form) {
            parent.form = form
            parent.formKey = form.formKey
        }
    }

    @Override
    boolean onHandleNodeAttributes (FactoryBuilderSupport builder, Object form, Map attributes) {
        if (attributes.formKey) {
            form.formKey = attributes.formKey
            attributes.remove('formKey')
        }
        //carry with parent handler
        super.onHandleNodeAttributes(builder, form, attributes)
        true
    }


    boolean isLeaf() {
        leaf
    }
}

class Form {
    String formKey
    def formProperties = []

    String toString () {
        StringBuffer buff = new StringBuffer()
        buff << "<extensionElements>\n"
        formProperties.each {buff << "\t$it\n"}
        buff << "</extensionElements> "
        buff.toString()
    }
}
