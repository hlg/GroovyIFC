package utils

import openifctools.com.openifcjavatoolbox.guidcompressor.GuidCompressor
import openifctools.com.openifcjavatoolbox.ifc2x3tc1.IfcBuildingStorey
import openifctools.com.openifcjavatoolbox.ifc2x3tc1.IfcProject
import openifctools.com.openifcjavatoolbox.ifcmodel.IfcModel
import openifctools.com.openifcjavatoolbox.step.parser.util.StepParserProgressListener
import openifctools.com.openifcjavatoolbox.step.parser.util.ProgressEvent

class IfcModelHelper {
  def ifcModel = new IfcModel()
  def builder = new IfcBuilder(model: ifcModel)

  void setProgressListener(closure = {ProgressEvent event-> print ((event.currentState % 20) ? '.' : '.\n')}){
    ifcModel.addStepParserProgressListener([progressActionPerformed: closure] as StepParserProgressListener)
  }

  void setFileName(String fileName) {
    ifcModel.clearModel()
    ifcModel.readStepFile(new File(fileName))
  }

  def recursePlacement(localPlacement) {
    def result = localPlacement.relativePlacement.location.coordinates[2].value
    if (localPlacement.placementRelTo) {
      result += recursePlacement(localPlacement.placementRelTo)
    }
    result
  }

  def fixStoreyAssignment(tolerance) {
    def storeys = ifcModel.getCollection(IfcBuildingStorey.class)
    def rangeMap = storeys.collectEntries { [it, [it.elevation.value + tolerance[0], it.elevation.value + tolerance[1]]] }
    storeys.each { storey ->
      println "\n*** $storey $storey.elevation"
      storey.containsElements_Inverse.each { rel ->
        def matched = [:]
        rel.relatedElements.each { element ->
          // TODO: assertion (z-axis parallel coordinate systems)
          def z = recursePlacement(element.objectPlacement)
          def matchingStorey = rangeMap.find { k, v -> v[0] <= z && v[1] >= z }.key
          if (matchingStorey && (storey != matchingStorey)) {
            println "assigning element with z = $z to $matchingStorey"
            matched[element] = matchingStorey
          }
        }
        matched.each { element, newStorey ->
          rel.removeRelatedElements(element)
          if (!newStorey.containsElements_Inverse) {
            builder.relContainedInSpatialStructure {
              globalId = GloballyUniqueId(GuidCompressor.newIfcGloballyUniqueId)
              relatingStructure = newStorey
              relatedElements = [element] as Set
            }
          } else {
            newStorey.containsElements_Inverse.each { newRel ->
              newRel.addRelatedElements(element)
            }
          }
        }
      }
    }

  }

  def printAsciiTree() {
    def spatialRoot = ifcModel.ifcObjects.find {it instanceof IfcProject}
    printAsciiTreeRec(spatialRoot, '')
  }

  def printAsciiTreeRec(spatial, prefix) {
    println "$prefix-$spatial $spatial.name - ${spatial.class.name}";
    def children = spatial.isDecomposedBy_Inverse?.relatedObjects?.flatten()  // TODO: Ifc 2x4 nests_Inverse
    if (children) {
      if (children.size() > 1) {
        children[0..-2].each { space ->
          printAsciiTreeRec(space, prefix + ' |')
        }
      }
      printAsciiTreeRec(children[-1], prefix + '  ')
    }
  }

  def removeDummySpace(space) {
    // workaround: there is no easy generic way to find referencing relation objects with IFC toolbox
    def related = [HasAssignments: 'RelatedObjects', Decomposes: 'RelatedObjects', IsDefinedBy: 'RelatedObjects',
            HasAssociations: 'RelatedObjects', ServicedBySystems: 'RelatedBuildings']
    def relating = [IsDecomposedBy: 'RelatingObject', ReferencedBy: 'RelatingProduct', ContainsElements: 'RelatingStructure',
            ReferencesElements: 'RelatingStructure', HasCoverings: 'RelatingSpace', BoundedBy: 'RelatingSpace']
    related.each {k, v ->
      space[k + '_Inverse'].collect {it}.each {
        it.invokeMethod('remove' + v, space)
        if (it[v].isEmpty()) ifcModel.removeIfcObject(it)
      }
    }
    relating.each {k, v ->
      def rel = space[k + '_Inverse']
      if (rel) ifcModel.removeIfcObject(rel)
    }
    ifcModel.removeIfcObject(space)
  }

}
