This is a growing set of groovy tools to handle [IFC](http://buildingsmart-tech.org/specifications/ifc-overview/) data. GroovyIFC is currently based on the Open IFC Java Toolbox (see prerequisites). The tool set started out as dae2ifc.groovy, a skript to write IFC geometry from collada files.

The heart of the script - IfcBuilder - can be easily used to write arbitrary IFC entities programmatically from arbitrary input data - in a compact and concious fashion. Apart from writing, the combination of Groovy and OpenIfcTools is a very handy way for ad-hoc queries on IFC data. Another use case is quick and dirty programmatic manipulation of IFC files for research or prototyping purposes (see DeepSpatialHierarchy.groovy). A set of utility methods is bundled in IfcModelHelper.

People asked for it, so I decided to put it out in the wild. Hope it'll gonna be useful to anybody. Comes with no warranty.

Prerequisites
=============

* [Groovy](http://groovy-lang.org/)
* [IFC Tools Project](http://www.ifctoolsproject.com/): the current development version of GroovyIfc is based on their Java Toolbox 2.0.1


How to write IFC
================
This example shows how dae2ifc.groovy builds its IFC model

    def ifcBuilder = new IfcBuilder()
    ifcBuilder.proxy {
      globalId = globallyUniqueId('123456')
      representation = productDefinitionShape {
        representations = [
          shapeRepresentation {
            representationIdentifier = label('Body')
            representationType = label('Brep')
            items = [] as Set
          }
	    ]  
      }
    }
    ifcBuilder.write("path/to/file", "file.ifc")

This results in "file.ifc" with the following lines in its data part:

    #1= IFCSHAPEREPRESENTATION($,'Body','Brep',());
    #2= IFCPRODUCTDEFINITIONSHAPE($,$,(#1));
    #3= IFCPROXY('123456',$,$,$,$,$,#2,$,$);


How to read IFC
===============
Just read in and use GPath. You could do something like this:

    def model = new IfcModel()
    model.readStepFile("file.ifc")
    def products = model.ifcObjects.findAll{ it instanceof IfcProduct }
    println products.representation.representations.representationType
    // find all products without representations or with representations without items
    products.findAll{prod ->
      ! prod.representation?.representations.any{it -> !it.items.isEmpty()}
    }

Nice to read, isn't it?

How to improve
==============
I'd love to see these improvements, but I'm afraid I won't find the time. Don't hesitate to fork the project.

dae2ifc.groovy:
* validate manifold Brep geometry
* optimize ifc file: write each point once only (currently multiple times - once for every polygon)

other use cases:
* sophisticated example for reading/querying IFC (e.g. representation counter)
