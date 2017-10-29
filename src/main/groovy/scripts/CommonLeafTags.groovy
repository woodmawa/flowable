package scripts

import javax.activation.UnsupportedDataTypeException

class CommonLeafTagFactory extends AbstractFactory {
    def newInstance (FactoryBuilderSupport builder, name, value, Map attributes) {
        if (name == "script") {
            new Script (text: value)
        } else if (name == "description"){
            new Description (text:value)
        } else if (name == "documentation") {
            new Documentation (text:value)
        } else if (name == "potentialOwner") {
            new PotentialOwner (expression:value)
        }else if (name == "formProperty") {
            new FormProperty (name:value)
        }
        else {
            throw  UnsupportedDataTypeException.newInstance("tage name $name is not supported")
        }

    }

    void setParent (FactoryBuilderSupport builder, Object parent, Object child ) {
        if (child instanceof Script) {
            parent.scriptBlock = child
        } else if (child instanceof Description) {
            parent.description = child
        } else if (child instanceof Documentation) {
            parent.documentation = child
        } else if (child instanceof PotentialOwner) {
            parent.potentialOwner = child
        } else if (child instanceof FormProperty) {
            parent.formProperties << child
        }
    }

    @Override
    boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes) {
        if (node instanceof FormProperty) {
            if (attributes.id) {
                node.'id' = attributes.id
                attributes.remove('id')
            } else
                node.id = builder.getNextId ("fp_")
            if (attributes.type) {
                node.'type' = attributes.type
                attributes.remove ('type')
            }
            if (attributes.variable) {
                node.'variable' = attributes.variable
                attributes.remove ('variable')
            }
            if (attributes.writable) {
                node.writable = attributes.writable as Boolean
                attributes.remove ('writable')
            }
            if (attributes.required) {
                node.required = attributes.required as Boolean
                attributes.remove ('reaquired')
            }
            if (attributes.expression) {
                node.expression = attributes.expression
                attributes.remove ('expression')
            }
            if (attributes.datePattern) {
                node.datePattern = attributes.datepattern
                attributes.remove ('datePattern')
            }

        }

        return super.onHandleNodeAttributes(builder, node, attributes)
    }

    boolean isLeaf() { true}

}

class Documentation {
    String text

    String toString () {
        """<documentation>
$text
</documentation>
"""
    }
}

class Description {
    String text

    String toString () {
        """<description>
$text
</description>
"""
    }
}

class Script {
    String text

    String toString () {
        """<script>
$text
</script>"""
    }
}

class PotentialOwner {
    String expression

    String toString () {
        StringBuffer buff = new StringBuffer ()
        buff << "<potentialOwner>\n"
        buff << "\t<resourceAssignmentExpression>\n"
        buff << "\t\t<formalExpression>$expression</formalExpression>\n"
        buff << "\t</resourceAssignmentExpression>\n"
        buff << "</potentialOwner\n"

    }
}

class FormProperty {
    String id
    String name
    String variable
    String type
    String datePattern
    String expression
    Boolean required
    Boolean writable
    EnumValue enumValues = []

    //TODO - need to allow for flowable:value children if type=enum
    String toString() {
        StringBuffer buff = new StringBuffer()

        buff << """<flowable:formProperty id="$id" name="$name" """
        if (type)
            buff << """type="$type" """
        if (type == 'date' && datePattern)
            buff << """datePattern="$datePattern" """
        if (variable)
            buff << """variable="$variable" """
        if (expression)
            buff << """expression="$expression" """
        if (writable)
            buff << """writable="$writable" """
        if (required)
            buff << """required="$required" """

        buff << "/>"
    }
}

//for declaring child <flowable:value if formProperty is enum
class EnumValue {
    String id
    String name

    String toString () {
        """<flowable:value id="$id" name="$name"  """
    }
}

