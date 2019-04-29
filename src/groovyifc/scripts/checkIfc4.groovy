package groovyifc.scripts

import groovyifc.utils.IfcModel

def model = new IfcModel(fileName: args[0])
println "successfully read model with ${model.ifcObjects.size()} IFC entities"

