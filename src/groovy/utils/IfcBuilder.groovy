package utils

/* Copyright (c) 2010-2017 Helga Tauscher
 * http://github.com/hlg/GroovyIFC
 *
 * This file is part of Groovy Ifc Tools, which are distributed
 * under the terms of the GNU General Public License version 3
 */

import ifc4javatoolbox.ifcmodel.IfcModel
import ifc4javatoolbox.ifc4.*

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

  protected Object positive(length) {
    new IfcPositiveLengthMeasure(new IfcLengthMeasure(length))
  }

  protected Object createNode(Object name) {
    ifcClass(name).newInstance()
  }

  protected Object createNode(Object name, Object value) {
    if (value instanceof String) {value = new STRING(value, false)}
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
    if (node instanceof ClassInterface) { model.addIfcObject(node) }
  }


  def write(String path, String fileName) {
    model.writeStepfile(new File(path, fileName))
  }
}

