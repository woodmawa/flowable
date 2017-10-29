package scripts

import groovy.xml.XmlUtil
import org.xml.sax.InputSource

import javax.xml.transform.OutputKeys
import javax.xml.transform.Source
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.sax.SAXTransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicLong

class BpmnProcessBuilder extends FactoryBuilderSupport {
    {
        registerFactory('process', new ProcessFactory())
        registerFactory('flow', new FlowFactory())
        registerFactory ('start', new EventFactory())
        registerFactory ('end', new EventFactory())
        registerFactory ('scriptTask', new TaskFactory())
        registerFactory ('serviceTask', new TaskFactory())
        registerFactory ('userTask', new TaskFactory())
        registerFactory('form', new FormFactory())
        registerFactory ('potentialOwner', new CommonLeafTagFactory())
        registerFactory ('documentation', new CommonLeafTagFactory())
        registerFactory ('formProperty', new CommonLeafTagFactory())
        registerFactory ('script', new CommonLeafTagFactory())

    }

    AtomicLong nextId = new AtomicLong(0)

    String defaultSpecVersion = "20100524"
    String targetNamespace = "com.softwood"
    Map namespaces = [flowable: "http://flowable.org/bpmn",
                        bpmndi: "http://www.omg.org/spec/BPMN/$defaultSpecVersion/DI",
                        dc    : "http://www.omg.org/spec/DD/$defaultSpecVersion/DC",
                        di    : "http://www.omg.org/spec/$defaultSpecVersion/DI",
                        xsd   : "http://www.w3.org/2001/XMLSchema",
                        xsi   : "http://www.w3.org/2001/XMLSchema-instance"]
    String defaultTypeLanguage = "http://www.w3.org/2001/XMLSchema"
    String defaultNamespace = "http://www.omg.org.spec/BPMN/$defaultSpecVersion/MODEL"
    String schemaLocation = "http://www.omg.org/spec/BPMN/20100524/MODEL"

    def declareNamespaces(Map ns) {
        namespaces << ns
    }

    String id = getNextId("def_")
    String name = "hardcodedname"

    Process process

    //if stub present just pre pend to next id
    //not sure this is thread safe, resolve
    String getNextId (String stub = null) {

        long id = nextId.incrementAndGet()
        if (stub)
            "$stub$id"
        else
            "$id"
    }

    int indentLevel = 1
    String indent(indentLevel) {
        StringBuffer indent = new StringBuffer()
        indentLevel.times {
            indent << "\t"
        }
        indent.toString()
    }

    //Todo sort stream writer stuff - broken
    void exportToBPMN (fileType) {
        File exportFile
        if (fileType == "xml")
            exportFile = new File("processes/${}.bpmn20.xml")
        else if (fileType == "bpmn")
            exportFile = new File("processes/${}.bpmn")

        exportFile.createNewFile()
        exportFile.text = toString()
    }

    //get the bpmn as 'InputStream' for consumers
    InputStream exportAsInputStream () {
        String xml = this.toString()
        new ByteArrayInputStream(xml.getBytes("UTF-8"))
    }

    String toString() {
        StringBuffer nsList = new StringBuffer()
        nsList <<  "\t" << /targetNamespace="$targetNamespace"/ << "\n"
        nsList << "\t" << /xmlns="$defaultNamespace"/ << "\n"
        namespaces.each {k,v -> nsList << "\t" << /xmlns:$k="$v"/ << "\n"}
        nsList << "\t" << /typeLanguage="$defaultTypeLanguage"/

        String xmlDeclaration = """<?xml version="1.0" encoding="UTF-8"?>"""

        def structureAndMessages =  """
<!--    Structures and Messages -->
<import importType="http://www.w3.org/2001/XMLSchema" 	location="DataDefinitions.xsd" 	namespace="http://www.example.org/Messages"/>
<import importType="http://schemas.xmlsoap.org/wsdl/" 	location="Interfaces.wsdl" 	namespace="http://www.example.org/Messages"/>
"""

        String processDefinition = """
<!-- process definition -->
$process
"""

        /**
         * build the output bpmn definition xml as string
         */

        StringBuffer xmlString = new StringBuffer()
        xmlString << "$xmlDeclaration " << "\n"
        xmlString << """<definitions name="$name" id="$id" """ << "\n"
        xmlString << "$nsList>" << "\n\n"
        structureAndMessages.eachLine { xmlString << "\t$it\n"}
        processDefinition.eachLine {xmlString << "\t$it\n"}
        //xmlString << "$processDefinition" << "\n"
        xmlString << "</definitions>" << "\n"


        //nearly does whats requireed
        /*def root = new XmlParser().parseText(xmlString)
        def xmlOutput = new StringWriter()
        def xmlNodePrinter = new XmlNodePrinter(new PrintWriter(xmlOutput))
        xmlNodePrinter.preserveWhitespace = true
        xmlNodePrinter.expandEmptyElements = true
        xmlNodePrinter.quote = "'" // Use single quote for attributes
        xmlNodePrinter.namespaceAware = true


        xmlNodePrinter.print(root)
        xmlOutput*/

        //prettyFormat (xmlString, 4)
        //formatXml (xmlString)
        //XmlUtil.serialize(xmlString.toString())
        //build own pretty printer!!
    }
}


BpmnProcessBuilder processBuilder = new BpmnProcessBuilder(name:"myProcessDefinition")
def proc = processBuilder.process ("myProcess", id:27) {
    def st = start ('start', id:1) {
        form ('') {
            formProperty ("propName", id:'frm1', type:"string")
        }
    }
    flow ('fistStep', source:st.id, target:'scr1', id:'10')
    scriptTask ('doScript', id:'scr1', scriptFormat:'groovy', script: "out:println 'hello'")
    userTask ('openDoor', id:'ut1'){
        documentation ("some notes")
        potentialOwner ("will")
    }
    flow ('fistStep', source:'scr1', target:'100')

    def fini = end  ('terminate', id:100)

}

println processBuilder.exportAsInputStream().text  //toString()