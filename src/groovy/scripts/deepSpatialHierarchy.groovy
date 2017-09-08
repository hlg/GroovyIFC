package groovy.scripts

import groovy.utils.IfcModel
import ifc4javatoolbox.guidcompressor.GuidCompressor
import groovy.utils.SpatialStructureManipulator
import ifc4javatoolbox.ifc4.*
import ifc4javatoolbox.step.parser.util.ProgressEvent

/**
 * @author Helga Tauscher http://github.com/hlg
 * @author Robert Kühn http://github.com/rokondo
 *
 * This file is part of Groovy Ifc Tools, which are distributed
 * under the terms of the GNU General Public License version 3
 * http://github.com/hlg/GroovyIFC

 * adds geometrical representation to IfcSpatialStructureElements
    + repesentation is taken from an IfcSpace matched by its @longName attribute
    + see marker.site, marker.building, marker.storey
 * groups IfcSpaces to complex IfcSpaces
    + group spaces and singular spaces are matched based on their @longName attribute
    + marker.subStorey<X> (space group) consists of spaces marker.subStoreySpace<X>
 * groups IfcBuildingStoreys to complex IfcBuildingStoreys
    + complex storeys are identified by their @longName attribute matching  marker.storeyGroup
    + singular storeys are assigned by matching their elevation attribute
 * assumes all spaces are direct children of storeys in the spatial structure of the original model
*/

def printProgress = { ProgressEvent event->
  if (event.currentState == 1) println event.message
  print ((event.currentState % 50) ? '.' : '.\n')
}

def model = new IfcModel(progressListener: printProgress, fileName: args[0])
def spatialStructure = new SpatialStructureManipulator(model: model)
spatialStructure.printAsciiTree()

def copyRepresentation(spaceOrigin, spaceDest) {
  spaceDest.representation = spaceOrigin.representation
  spaceDest.objectPlacement = spaceOrigin.objectPlacement
}

def marker = [site: 'CIB_Site', building: 'CIB_Gebaude', storeyGroup: 'CIB_Milestone', storey: 'Bruttogeschoss|BGF',
        subStorey: 'Bauabschnitt', subStoreySpace: '-BA']

def spaces = model.getCollection(IfcSpace.class)
def storeys = model.getCollection(IfcBuildingStorey.class)
def site = model.ifcObjects.find {it instanceof IfcSite}
def building = model.ifcObjects.find {it instanceof IfcBuilding}

spaces.findAll {it.longName.value =~ /$marker.site/}.each { dummySite ->
  copyRepresentation(dummySite, site)
  spatialStructure.removeDummySpace(dummySite)
}

spaces.findAll {it.longName.value =~ /$marker.building/}.each {dummyBuilding ->
  copyRepresentation(dummyBuilding, building)
  spatialStructure.removeDummySpace(dummyBuilding)
}

spaces.findAll {it.longName.value =~ /$marker.storey/}.each { dummyStorey ->
  dummyStorey.Decomposes_Inverse.each { parentStorey ->    // STEP modelling WTF? set of size 0 or 1
    copyRepresentation(dummyStorey, parentStorey.RelatingObject)
    spatialStructure.removeDummySpace(dummyStorey)
  }
}

spaces.findAll {it.longName.toString() =~ /$marker.subStorey/}.each {dummySubStorey ->
  dummySubStorey.Decomposes_Inverse.each { parentStorey ->
    // TODO: could also be modeled as storey with compositionType PARTIAL
    dummySubStorey.compositionType = new IfcElementCompositionEnum('COMPLEX')
    def storeySection = dummySubStorey.longName.toString()[-1];
    def subSpaces = parentStorey.relatedObjects.findAll {
      it.name.value =~ /$marker.subStoreySpace$storeySection/
    }
    parentStorey.removeAllRelatedObjects(subSpaces)
    model.builder.relAggregates {
      globalId = GloballyUniqueId(GuidCompressor.newIfcGloballyUniqueId)
      relatingObject = dummySubStorey
      relatedObjects = subSpaces as Set
    }
  }
}
def sortedGroupStoreys = spaces.findAll {it.longName.toString() =~ /$marker.storeyGroup/}.collect {dummyStoreyGroup ->
  dummyStoreyGroup.Decomposes_Inverse.collect { parentStorey ->
    def storeyGroup = model.builder.buildingStorey {
      globalId = GloballyUniqueId(GuidCompressor.newIfcGloballyUniqueId)
      name = new IfcLabel(marker.storeyGroup, false)
      longName = new IfcLabel(marker.storeyGroup, false)
      compositionType = new IfcElementCompositionEnum('COMPLEX')
      elevation = parentStorey.relatingObject.elevation
    }
    copyRepresentation(dummyStoreyGroup, storeyGroup)
    spatialStructure.removeDummySpace(dummyStoreyGroup)
    storeyGroup
  }
}.flatten().sort {it.elevation.value}.reverse()

storeys.groupBy { storey ->
  sortedGroupStoreys.find {storey.elevation.value >= it.elevation.value}
}.each { groupDummy, storeysInGroup ->
  assert groupDummy != null
  building.IsDecomposedBy_Inverse.each { parentStorey ->
    parentStorey.addRelatedObjects(groupDummy)
    parentStorey.removeAllRelatedObjects(storeysInGroup)
  }
  model.builder.relAggregates {
    globalId = GloballyUniqueId(GuidCompressor.newIfcGloballyUniqueId)
    relatingObject = groupDummy
    relatedObjects = storeysInGroup as Set
  }
}

// save and verify the changed model by parsing it again
model.writeStepfile(new File(args[1]))
model.fileName = args[1]
spatialStructure.printAsciiTree()
println model.numberOfElements
