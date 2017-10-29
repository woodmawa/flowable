package scripts

import groovy.xml.StreamingMarkupBuilder

def mkp = new StreamingMarkupBuilder().bind {bldr ->
    mkp.xmlDeclaration()
    mkp.declareNamespace (semantic: "http://www.omg.org/spec/BPMN/20100524/MODEL")
    process(id:"the process") {
        def start = startEvent (id:"theStart")
//        println start.dump()
        //sequenceFlow (bldr, start.id, end.id)
        def end = endEvent (id:"the end")
    }
    //mkp.decalareNamespace (xsi : "http://www.w3.org/2001/XMLSchema-instance")
}

def sequenceFlow (builder, source, target) {
    builder.sequenceFlow (id:1, sourceRef:source, targetRef:target)
}

println "class: "+ mkp.class + ", with value: "+ mkp.toString()