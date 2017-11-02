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
        registerFactory ('signal', new EventTriggerFactory())
        registerFactory ('message', new EventTriggerFactory())
        registerFactory ('error', new EventTriggerFactory())
        registerFactory ('scriptTask', new TaskFactory())
        registerFactory ('serviceTask', new TaskFactory())
        registerFactory ('userTask', new TaskFactory())
        registerFactory ('boundaryEvent', new EventFactory())
        registerFactory('form', new FormFactory())
        registerFactory ('potentialOwner', new CommonLeafTagFactory())
        registerFactory ('documentation', new CommonLeafTagFactory())
        registerFactory ('formProperty', new CommonLeafTagFactory())
        registerFactory ('script', new CommonLeafTagFactory())

    }

    String name = "hardcoded definition name"

    Definition definition

    AtomicLong nextId  = new AtomicLong(0)

    //if stub present just pre pend to next id
    //not sure this is thread safe, resolve
    String getNextId (String stub = null) {

        long id = nextId.incrementAndGet()
        if (stub)
            "$stub$id"
        else
            "$id"

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
        String root = System.getProperty("user.dir")
        URL url = this.getClass().getResource("/")
        File loc = new File(url.toURI())
        String canPath = loc.getCanonicalPath()
        String stem = "$root${File.separatorChar}out${File.separatorChar}test"
        if (canPath.contains(stem))
            isTest = true

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
    error ("new error", id: "error")

    process ("myProcess", id:27) {
        def st = start ('start', id:1) {
            form () {
                formProperty ("propName", id:'frmp1', type:"string")

                formProperty ("amount", id:'frmp2', type:"long")

            }
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
        boundaryEvent (id:'bnd#1', duration: "PT1M", attachedTo: "ut1", cancelActivity: true)
        flow ( source:'bnd#1', target:'100')

        def fini = end  ('terminate', id:100)

    }
}


println processBuilder.exportAsInputStream().text  //toString()