This is a groovy skript to write IFC geometry from collada files using OpenIfcTools. It does only cover a small subset of the collada standard (basically the part that is used when exporting solid polygonal geometry from Google Sketchup) and it writes only IFC proxy entities. However the heart of the script - IfcBuilder - can be easily used to write arbitrary IFC entities programmatically from arbitrary input data - in a compact and concious fashion. Apart from writing, the combination of Groovy and OpenIfcTools is a very handy way for ad-hoc queries on IFC data. See the the RepresentationCounter example.

People asked for it, so I decided to put it out in the wild. Hope it's gonna be useful to anybody. Comes with no warranty.

Prerequisites
=============

* [Groovy](http://groovy.codehaus.org/)
* [Open Java Toolbox](http://www.openifctools.com/Open_IFC_Tools/ifc_features.html)


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

* validate manifold Brep geometry
* optimize ifc file: write each point once only (currently multiple times - once for every polygon)
* sophisticated example for reading/querying IFC (e.g. representation counter)
