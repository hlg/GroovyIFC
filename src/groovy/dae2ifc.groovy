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
  // TODO polylist.each ...
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
