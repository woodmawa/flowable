package scripts

class DefinitionFactory extends AbstractFactory {

    boolean leaf = false

    def newInstance (FactoryBuilderSupport builder, name, value, Map attributes) {
        def definition = new Definition (name : value)
        builder.definition = definition
        definition
    }

    void setChild (FactoryBuilderSupport builder, Object definition, Object child ) {
        if (child instanceof Process )
            definition.processes << child
    }

    @Override
    boolean onHandleNodeAttributes (FactoryBuilderSupport builder, Object event, Map attributes) {
        if (attributes.id) {
            event.id = attributes.id
            attributes.remove('id')
        } else {
            event.id = builder.getNextId("def_")
        }

        super.onHandleNodeAttributes(builder, event, attributes )
    }

    boolean isLeaf() {
        leaf
    }

}


class Definition {
    String id
    String name

    //register any message, error or signals at root level
    def eventTriggers = []
    def processes = []

    String defaultSpecVersion = "20100524"
    String targetNamespace = "com.softwood"
    Map namespaces = [flowable: "http://flowable.org/bpmn",
                      bpmndi: "http://www.omg.org/spec/BPMN/$defaultSpecVersion/DI",
                      dc    : "http://www.omg.org/spec/DD/$defaultSpecVersion/DC",
                      di    : "http://www.omg.org/spec/BPMN/$defaultSpecVersion/DI",
                      xsd   : "http://www.w3.org/2001/XMLSchema",
                      xsi   : "http://www.w3.org/2001/XMLSchema-instance"]
    String defaultTypeLanguage = "http://www.w3.org/2001/XMLSchema"
    String defaultNamespace = "http://www.omg.org/spec/BPMN/$defaultSpecVersion/MODEL"
    String schemaLocation = "http://www.omg.org/spec/BPMN/20100524/MODEL"

    def declareNamespaces(Map ns) {
        namespaces << ns
    }

    def importsDefinitions =  """
<!--    type imports -->
<import importType="http://www.w3.org/2001/XMLSchema" 	location="DataDefinitions.xsd" 	namespace="http://www.example.org/Messages"/>
<import importType="http://schemas.xmlsoap.org/wsdl/" 	location="Interfaces.wsdl" 	namespace="http://www.example.org/Messages"/>
"""

    /**
     * builds master BPMN XML file format as String
     *
     * @return Bpmn XML definition
     */
    String toString() {
        //build namespaceList
        StringBuffer nsList = new StringBuffer()
        nsList <<  "\t" << /targetNamespace="$targetNamespace"/ << "\n"
        nsList << "\t" << /xmlns="$defaultNamespace"/ << "\n"
        namespaces.each {k,v -> nsList << "\t" << /xmlns:$k="$v"/ << "\n"}
        nsList << "\t" << /typeLanguage="$defaultTypeLanguage"/

        /**
         * build the output bpmn definition xml as string
         */

        StringBuffer xmlString = new StringBuffer()
        xmlString << """<definitions """
        if (name)
            xmlString << """name="$name" """
        if (id)
            xmlString << """id="$id" """ << "\n"
        xmlString << "$nsList>" << "\n"

        if (importsDefinitions) {
            importsDefinitions.eachLine { xmlString << "\t$it\n" }
            xmlString << "\n"
        }
        if (eventTriggers) {
            xmlString << """\t<!-- root level event triggers -->""" << "\n"
            eventTriggers.each { trigger ->
                trigger.toString().eachLine { xmlString << "\t$it\n" }
            }
            xmlString << "\n"
        }
        if (processes) {
            xmlString << """\t<!-- process definition -->""" << "\n"
            processes.each { process -> process.toString().eachLine { xmlString << "\t$it\n" } }
        }

        xmlString << "</definitions>" << "\n"


    }
}