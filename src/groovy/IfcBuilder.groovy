import openifctools.com.openifcjavatoolbox.ifcmodel.IfcModel
import openifctools.com.openifcjavatoolbox.ifc2x3tc1.*

class IfcBuilder extends BuilderSupport {

  IfcModel model

  public static IfcBuilder newInstance() {
    def builder = new IfcBuilder()
    builder.@model = new IfcModel()
    builder
  }

  protected void setParent(Object parent, Object child) {}

  public void setProperty(String name, Object value) {
    if (value instanceof List) {value = new LIST(value)}
    if (value instanceof Set) {value = new SET(value)}
    if (value instanceof Boolean) {value = new BOOLEAN(value)}
    current[name] = value
  }

  protected Object cartesianPoint(x, y, z) {
    def point = createNode('cartesianPoint')
    point.coordinates = new LIST([x, y, z].collect {new IfcLengthMeasure(it)})
    model.addIfcObject(point)
    point
  }

  protected Object direction(x, y, z) {
    def direction = createNode('direction')
    direction.directionRatios = new LIST([x, y, z].collect {new DOUBLE(it)})
    model.addIfcObject(direction)
    direction
  }

  protected Object createNode(Object name) {
    ifcClass(name).newInstance()
  }

  protected Object createNode(Object name, Object value) {
    ifcClass(name).newInstance(value)
  }

  private ifcClass(name) {
    def className = "openifctools.com.openifcjavatoolbox.ifc2x3tc1.Ifc" + name[0].toUpperCase() + name[1..-1]
    Class.forName(className)
  }

  protected Object createNode(Object name, Map attributes) {
    def ifcObj = createNode(name)
    attributes.each { k, v -> ifcObj[k] = v }
    ifcObj
  }

  protected Object createNode(Object name, Map attributes, Object value) {
    createNode(name, attributes)   // syntax with value needed?
  }

  protected void setClosureDelegate(Closure closure, Object node) {
    super.setClosureDelegate(closure, node)
    closure.resolveStrategy = Closure.DELEGATE_FIRST
  }

  protected void nodeCompleted(Object parent, Object node) {
    if (node instanceof IfcClass) { model.addIfcObject(node) }
  }


  def write(String path, String fileName) {
    model.writeStepfile(new File(path, fileName))
  }
}

class BrepBuilder {
  IfcBuilder ifcBuilder = IfcBuilder.newInstance()
  def world
  def proxy

  void init() {
    world = ifcBuilder.geometricRepresentationContext {
      coordinateSpaceDimension = dimensionCount(3)
      worldCoordinateSystem = axis2Placement3D {
        location = cartesianPoint(0, 0, 0)
        axis = direction(0, 0, 1)
        refDirection = direction(1, 0, 0)
      }
    }
    proxy = ifcBuilder.proxy {
      representation = productDefinitionShape {
        representations = [shapeRepresentation {
          representationIdentifier = label('Body')
          representationType = label('Brep')
          contextOfItems = world
          items = [] as Set
        }]
      }
    }
  }


  void addBrep(shellParts) {

    proxy.representation.representations.first().items.add(
            ifcBuilder.facetedBrep {
              outer = closedShell {
                def collectedFaces = []
                shellParts.each { points, polygons ->
                  points = points.collect { x, y, z -> cartesianPoint(x, y, z) }
                  polygons.each { polyLoops ->
                    collectedFaces << face {
                      def outerLoop = polyLoops.remove(0)
                      def collectedBounds = [faceOuterBound {
                        bound = polyLoop {
                          polygon = outerLoop.collect { i -> points[i] }
                        }
                        orientation = true
                      }]
                      polyLoops.each { innerLoop ->
                        collectedBounds << faceBound {
                          bound = polyLoop {
                            polygon = innerLoop.collect { i -> points[i] }
                          }
                          orientation = false
                        }
                      }
                      bounds = collectedBounds as Set
                    }
                  }
                }
                cfsFaces = collectedFaces as Set
              }
            }
    )
  }

}
