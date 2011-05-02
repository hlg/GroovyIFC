/* Copyright (c) 2010-2011 Helga Tauscher
 * http://github.com/hlg/GroovyIFC
 *
 * This file is part of Groovy Ifc Tools, which are distributed
 * under the terms of the GNU General Public License version 3
 */

def brepBuilder = new BrepBuilder()
brepBuilder.init()

if (this.args.size() < 2) {
  println "usage: groovy dae2ifc.groovy <infile.dae> <outfile.ifc>"
  return
}

println "converting ${args[0]} to ${args[1]}"
def collada = new XmlSlurper().parseText(new File(args[0]).text)
def meshes = collada.library_geometries.geometry.mesh

def collectPoints(mesh) {
  def pointSourceId = mesh.vertices.input.find { it.@semantic == "POSITION" }.@source.text()[1..-1]
  def pointAtoms = mesh.source.find {it.@id == pointSourceId}.float_array.text().split(' ').collect {Double.parseDouble(it)}
  def points = []
  (0..(pointAtoms.size().intdiv(3) - 1)).each { pNum ->
    points << [0, 1, 2].collect {offSet -> pointAtoms[pNum * 3 + offSet]}
  }
  points
}

def shell = []
meshes.each { mesh ->
  def loops = []
  if (!mesh.polylist.isEmpty()) {
    assert mesh.polylist.input.@source.text()[1..-1] == mesh.vertices.@id.toString() // input.grep{it.@semantic == "VERTEX"}
    mesh.polylist.each { polylist ->
      def polyIndizes = polylist.p.text().split(' ').collect {Integer.parseInt(it)}
      polylist.vcount.text().split(' ').each { num ->
        num = Integer.parseInt(num)
        loops << [polyIndizes[0..(num - 1)]]
        if (num < polyIndizes.size()) {
          polyIndizes = polyIndizes[num..-1]
        }
      }
    }
  }
  if (!mesh.polygons.isEmpty()) {
    assert mesh.polygons.input.@source.text()[1..-1] == mesh.vertices.@id.toString() // input.grep{it.@semantic == "VERTEX"}
    mesh.polygons.each { polygon ->
      def loopWithHoles = [polygon.ph.p.text().split(' ').collect {Integer.parseInt(it)}]
      polygon.ph.h.each { hole ->
        loopWithHoles << hole.text().split(' ').collect { Integer.parseInt(it) }
      }
      loops << loopWithHoles
    }
  }
  shell << [collectPoints(mesh), loops]
}
brepBuilder.addBrep(shell)

def outfile = new File(args[1])
brepBuilder.ifcBuilder.write(outfile.parent, outfile.name)

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
