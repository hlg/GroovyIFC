package groovyifc.scripts

import groovyifc.utils.IfcBuilder
import groovy.xml.MarkupBuilder

IfcBuilder ifc = new IfcBuilder()

def representationContext = ifc.GeometricRepresentationContext {
  contextIdentifier = Label('space boundary connection geometries')
  coordinateSpaceDimension = DimensionCount(3)
  worldCoordinateSystem = Axis2Placement3D {
    location = cartesianPoint(0, 0, 0)
  }
}
def surfaceLabel = ifc.Label('Surface')
def surface3dLabel = ifc.Label('SurfaceModel')
def openingLoop = ifc.PolyLoop{
  def openingPointList = [[0, 200, 80], [0, 200, 230], [0, 400, 230], [0, 400, 80]].collect { x, y, z -> cartesianPoint(x, y, z) }
  polygon = openingPointList + [openingPointList[0]]
}
def openingRepresentationItem = ifc.FaceBasedSurfaceModel {
  fbsmFaces = [OpenShell {
    cfsFaces = [Face {
      bounds = [FaceOuterBound {
        bound = openingLoop
        orientation = true
      }]
    }]
  }]
}
def opening = ifc.OpeningElement {
  globalId = GloballyUniqueId('2OfS6jKEH2aheuWECCuR0h') // model.newGlobalUniqueId
  name = Label('window in outer wall')
  representation = ProductDefinitionShape {
    representations = [ShapeRepresentation {
      representationIdentifier = surfaceLabel
      representationType = surface3dLabel
      contextOfItems = representationContext
      items = [openingRepresentationItem] as Set
    }]
  }
}

def wallRepresentationItem = ifc.FaceBasedSurfaceModel {
  fbsmFaces = [OpenShell {
    cfsFaces = [Face {
      bounds = [FaceOuterBound {
        bound = PolyLoop {
          def wallPointList = [[0, 0, 0], [0, 0, 300], [0, 600, 300], [0, 600, 0]].collect { x, y, z -> cartesianPoint(x, y, z) }
          polygon = wallPointList + [wallPointList[0]]
        }
        orientation = true
      }, FaceBound {
        bound = openingLoop
        orientation = false
      }]
    }]
  }]
}
def wall = ifc.Wall{
  globalId = GloballyUniqueId('39MN8Pc8P64RbAmbS0QVsI')
  name = Label("outer wall with window")
  representation = ProductDefinitionShape {
    representations = [ShapeRepresentation {
      representationIdentifier = surfaceLabel
      representationType = surface3dLabel
      contextOfItems = representationContext
      items = [wallRepresentationItem] as Set
    }]
  }
}
ifc.RelDefinesByProperties {
  globalId = GloballyUniqueId('1PEf4iSFX4A8SZpRUS8ZDF')
  relatedObjects = [wall] as Set
  relatingPropertyDefinition = PropertySet {
    globalId = GloballyUniqueId('3Fjclt6y1D7gvUpbzhW3Zk')
    name = Label('externalOrInternal')
    hasProperties = [
      PropertySingleValue {
        name = Identifier('isExternal')
        nominalValue = true
      }
    ] as Set
  }
}
ifc.RelVoidsElement {
  globalId = GloballyUniqueId('26TpkN3Sv0HRYrEorzVGa1')
  relatedOpeningElement = opening
  relatingBuildingElement = wall
}

def externalSpace = ifc.ExternalSpatialElement{
  globalId = GloballyUniqueId('0t6TQX8gv0MRIrXF5ppAnm')
}
def wallBoundary = ifc.RelSpaceBoundary1stLevel {
  relatingSpace = externalSpace
  relatedBuildingElement = wall
  globalId = GloballyUniqueId('2upm7$erzD_OqYAlviWG0p')
  internalOrExternalBoundary = InternalOrExternalEnum('EXTERNAL')
  physicalOrVirtualBoundary = PhysicalOrVirtualEnum('PHYSICAL')
  connectionGeometry = ConnectionSurfaceGeometry {
    surfaceOnRelatedElement = wallRepresentationItem
  }
}

ifc.RelSpaceBoundary1stLevel {
  relatingSpace = externalSpace
  relatedBuildingElement = opening
  parentBoundary = wallBoundary
  globalId = GloballyUniqueId('3Fjclt6y1D7gvUpbzhW3Zk')
  internalOrExternalBoundary = InternalOrExternalEnum('EXTERNAL')
  physicalOrVirtualBoundary = PhysicalOrVirtualEnum('VIRTUAL')
  connectionGeometry = ConnectionSurfaceGeometry {
    surfaceOnRelatedElement = openingRepresentationItem
  }
}

ifc.write(new File(args[0]+'.ifc'), author='Tauscher', organization='Ifc2CityGML project')

println 'IFC written'

new File(args[0]+'.gml').withWriter { writer ->
  def gml = new MarkupBuilder(writer)
  gml.CityModel( xmlns:'http://www.opengis.net/citygml/2.0',
                'xmlns:gml': 'http://www.opengis.net/gml',
                'xmlns:bldg':'http://www.opengis.net/citygml/building/2.0'){
    /*
    'cityObjectMember' { 'bldg:Building' {
      'bldg:boundedBy' { 'bldg:WallSurface' {
      */
    'cityObjectMember' { 'bldg:WallSurface' {
        'gml:name'('outer wall with window')
        'bldg:lod3MultiSurface' { 'gml:MultiSurface' {
          'gml:surfaceMember' { 'gml:Polygon' {
            'gml:exterior' { 'gml:LinearRing' {
              'gml:posList'('0 0 0 0 0 300 0 600 300 0 600 0 0 0 0')
            }}
            'gml:interior' { 'gml:LinearRing' {
              'gml:posList'('0 200 80 0 400 80 0 400 230 0 200 230 0 200 80')
            }}
          }}
        }}
        'bldg:opening' { 'bldg:Window' {
          'gml:name'('window in outer wall')
          'bldg:lod3MultiSurface' { 'gml:MultiSurface' {
            'gml:surfaceMember' { 'gml:Polygon' {
              'gml:exterior' { 'gml:LinearRing' {
                'gml:posList' ('0 200 80 0 200 230 0 400 230 0 400 80 0 200 80')
              }}
            }}
          }}
        }}
     //  }}
    }}
  }}

println 'CityGML written'
