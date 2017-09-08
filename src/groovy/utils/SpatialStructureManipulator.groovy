package groovy.utils

import ifc4javatoolbox.guidcompressor.GuidCompressor
import ifc4javatoolbox.ifc4.IfcBuildingStorey
import ifc4javatoolbox.ifc4.IfcProject
import ifc4javatoolbox.ifcmodel.IfcModel

class SpatialStructureManipulator {

  def model = new IfcModel()

  def recursePlacement(localPlacement) {
    def result = localPlacement.relativePlacement.location.coordinates[2].value
    if (localPlacement.placementRelTo) {
      result += recursePlacement(localPlacement.placementRelTo)
    }
    result
  }

  def fixStoreyAssignment(tolerance) {
    def storeys = model.getCollection(IfcBuildingStorey.class)
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

  void printAsciiTree() {
    def spatialRoot = model.ifcObjects.find {it instanceof IfcProject}
    printAsciiTreeRec(spatialRoot, '')
  }

  void printAsciiTreeRec(spatial, prefix) {
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
        if (it[v].isEmpty()) model.removeIfcObject(it)
      }
    }
    relating.each {k, v ->
      def rel = space[k + '_Inverse']
      if (rel) model.removeIfcObject(rel)
    }
    model.removeIfcObject(space)
  }

}
