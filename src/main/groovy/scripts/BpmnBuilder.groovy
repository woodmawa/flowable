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
        registerFactory ('definition', new DefinitionFactory())
        registerFactory('process', new ProcessFactory())
        registerFactory('flow', new FlowFactory())
        registerFactory ('start', new EventFactory())
        registerFactory ('end', new EventFactory())
        registerFactory ('intermediateCatchEvent', new EventFactory())
        registerFactory ('intermediateThrowEvent', new EventFactory())
        registerFactory ('boundaryEvent', new EventFactory())

        registerFactory ('exclusiveGateway', new GatewayFactory())
        registerFactory ('parallelGateway', new GatewayFactory())

        registerFactory ('signal', new EventTriggerFactory())
        registerFactory ('message', new EventTriggerFactory())
        registerFactory ('error', new EventTriggerFactory())
        registerFactory ('timerEventDefinition', new EventDefinitionFactory())
        registerFactory ('messageEventDefinition', new EventDefinitionFactory())
        registerFactory ('errorEventDefinition', new EventDefinitionFactory())
        registerFactory ('signalEventDefinition', new EventDefinitionFactory())
        registerFactory ('terminateEventDefinition', new EventDefinitionFactory())
        registerFactory ('cancelEventDefinition', new EventDefinitionFactory())
        registerFactory ('compensateEventDefinition', new EventDefinitionFactory())

        registerFactory ('scriptTask', new TaskFactory())
        registerFactory ('serviceTask', new TaskFactory())
        registerFactory ('userTask', new TaskFactory())
        registerFactory ('form', new FormFactory())
        registerFactory ('potentialOwner', new CommonLeafTagFactory())
        registerFactory ('documentation', new CommonLeafTagFactory())
        registerFactory ('formProperty', new CommonLeafTagFactory())
        registerFactory ('script', new CommonLeafTagFactory())
        registerFactory ('condition', new CommonLeafTagFactory())

    }

    String name = "hardcoded definition name"

    Definition definition

    AtomicLong nextId  = new AtomicLong(0)


    /**
     * if stub present just pre pend to next id
     * todo - not sure this is thread safe, resolve
     *
     * @param stub
     * @return
     */
    String getNextId (String stub = null) {

        long id = nextId.incrementAndGet()
        if (stub)
            "$stub$id"
        else
            "$id"

    }

    /**
     * not really that used at this point
     * @param conf - Map of configuration attributes
     * @return this - current builder
     */
    BpmnProcessBuilder configure (Map conf) {
        if (conf.name)
            name = conf.name
        //others as required later
        this
    }

    /**
     * export definition as BPMN xml to filestore
     *
     * @param fileType (xml or bpmn)
     * @param directory (relative 'resources' package
     * @param fileName (name for file less file extension)
     */
    void exportToBPMN (fileType, dir=null, fileName=null) {

        boolean isTest = false

        //get project base directory
        String root = System.getProperty("user.dir")
        URL url = this.getClass().getResource("/")
        File loc = new File(url.toURI())
        String canPath = loc.getCanonicalPath()
        //build stem seeing if class is in <projroot>/out/test - if so your in testing
        String stem = "$root${File.separatorChar}out${File.separatorChar}test"
        if (canPath.contains(stem))
            isTest = true

        //so now build the path to resources in either test or main depending on isTest flag
        String resourcesPath
        if (isTest)
            resourcesPath = "$root${File.separatorChar}src${File.separatorChar}test${File.separatorChar}resources"
        else
            resourcesPath = "$root${File.separatorChar}src${File.separatorChar}main${File.separatorChar}resources"

        String procDir = dir ?: "processes"
        if (procDir.endsWith("$File.separatorChar"))
            procDir = procDir - "$File.separatorChar"
        String procFileName = fileName ?: "defaultBpmnProcessSpecification"
        String completeFileName
        File exportFile
        if (fileType == "xml")
            completeFileName = "$resourcesPath${File.separatorChar}$procDir${File.separatorChar}${procFileName}.bpmn20.xml"
        else if (fileType == "bpmn")
            completeFileName = "$procDir${File.separatorChar}${procFileName}.bpmn"

        exportFile = new File(completeFileName)
        println "path: $procDir, file:$procFileName, full: $completeFileName"

        exportFile.with {
            if (exists())
                delete()
            createNewFile()
            text = toString()
        }

    }

    /**
     *     get the bpmn as 'InputStream' for consumers
     *     @Return InputStream - bpmn definition as input stream
     */
    InputStream exportAsInputStream () {
        String xml = this.toString()
        new ByteArrayInputStream(xml.getBytes("UTF-8"))
    }


    String toString() {
        StringBuffer buff = new StringBuffer()
        String xmlDeclaration = """<?xml version="1.0" encoding="UTF-8"?>""" << "\n"

        buff << xmlDeclaration
        buff << definition.toString()

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

def definition = processBuilder.definition (id:"def#1") {
    message ( "new message", id:"newMessage")
    signal ("new signal", id: "signal")
    error ("new error", id: "error", errorCode: "123")

    process ("myProcess", id:27) {
        def st = start ('start', id:1) {
            form () {
                formProperty ("propName", id:'frmp1', type:"string")
                formProperty ("amount", id:'frmp2', type:"long")
            }
            timerEventDefinition (cycle:"PT1M")
        }
        flow ('fistStep', source:st.id, target:'scr1', id:'10')
        scriptTask ('doScript', id:'scr1', scriptFormat:'groovy', script: "out:println 'hello'")
        userTask ('openDoor', id:'ut1', assignee: "fozzie"){
            documentation ("some notes")
            potentialOwner ("will")
            form () {
                formProperty ("Customer", id:'cust', required:true, type: 'string')
            }
        }
        flow ('fistStep', source:'scr1', target:'100')
        //using 8601 std labelling for durations
        boundaryEvent (id:'bnd#1',  attachedTo: "ut1", cancelActivity: true){
            //timerEventDefinition (duration:'PT1M')
            compensateEventDefinition ()
        }
        intermediateCatchEvent (id:'intCat') {
            signalEventDefinition (signalRef:'payment')
        }
        parallelGateway ("mygateway", id:'gw1', default:'gw-flow-2')
        flow (id:'gw-flow-1', source: 'gw1', target: 'end') {
            condition ("\${a>10}")
        }
        flow (id:'gw-flow-2', source:'gw1', target:'end')
        flow (id:'gw-flow-3', source:'gw1', target:'end') {
            condition ("\${a>30}")
        }
        flow ( source:'bnd#1', target:'100') {
            condition ("\${order.price >100}")
        }

        def fini = end  ('terminate', id:100) {
            terminateEventDefinition (terminateAll:true)
        }

    }
}


println processBuilder.exportAsInputStream().text  //toString()