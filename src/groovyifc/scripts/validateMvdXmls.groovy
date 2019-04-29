import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

def factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
new File(args[1]).withReader { xsdReader -> 
  def schema = factory.newSchema(new StreamSource(xsdReader))
  def validator = schema.newValidator()
  new File(args[0]).withReader { xmlReader -> 
    validator.validate(new StreamSource(xmlReader))
  }
}

