package groovyifc.scripts
import groovy.xml.MarkupBuilder

new File('simpleExample.mvdxml').withWriter { writer ->
  def builder = new MarkupBuilder(writer)
  builder.mvcXML(name:'example MVD for mvdXML documentation sensor signals', 
                 xmlns: 'http://buildingsmart-tech.org/mvd/XML/1.1',
                 'xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance',
                 'xsi:schemaLocation': 'http://buildingsmart-tech.org/mvd/XML/1.1 ../mvdXML_V1.1.xsd'){
    Templates { ConceptTemplate ( uuid: 'concept1', name: 'Port Assigment',
                                  applicableSchema: 'IFC4', applicableEntity: 'IfcDistributionElement'){
      Definitions { Definition { Body('Distribution ports are defined by IfcDistributionPort and attached by the IfcRelNests relationship. Ports can be distinguished by the IfcDistributionPort attributes Name, PredefinedType, and FlowDirection') }}
      Rules { AttributeRule(AttributeName: 'IsNestedBy'){
        EntityRules { EntityRule(EntityName: 'IfcRelNests') {
          AttributeRules { AttributeRule(AttributeName: 'RelatedObjects' ) {
            EntityRules { EntityRule(EntityName: 'IfcDistributionPort') {
              AttributeRules { 
                AttributeRule(AttributeName:'Name', RuleId:'Name')
                AttributeRule(AttributeName:'PredefinedType', RuleId:'Type')
                AttributeRule(AttributeName:'FlowDirection', RuleId:'Flow')
              }
            }}
          }}
        }}
      }}
    }}
    Views { ModelView(name: 'Sensor signal view', applicableSchema: 'IFC4', code: 'Sensor') { 
      Definitions { Definition { Body('ModelView for mvdXML 1.1 documentation.') }}
      ExchangeRequirements { ExchangeRequirement(uuid: 'requriement1', name: 'Distribution signal', code: 'ERM1', applicabitlity: 'export') {
         Definitions { Definition { Body('Simple example for checking sensor elements to always submit signals.') }}
      }}
      Roots { ConceptRoot(name: 'Sensor', applicableRootEntity: 'IfcSensor'){ 
        Definitions { Definition { Body('Concept to validate that every sensor element has a port defined that submits signals') }}
        Concepts { Concept(name: 'Port Assignment') { 
          Definitions { Definition { Body('Concept to validate that every sensor has a port defined that submits signals.') }} 
          Template(ref: 'concept1')
          Requirements { Requirement(exchangeRequirement: 'requirement1', requirement: 'mandatory', applicability: 'export') }
          TemplateRules { TemplateRule(Parameters: "Name[Value]='Output' AND Type[Value]='SIGNAL' AND Flow[Value]='SOURCE'", Description: 'Transmits signal.') {
          }}
        }}
      }}
    }}
  }
}

// slightly simplified version from mvdXML1.1 specs introductory simple example, removed CDATA sections, removed GUIDs where not necessary (why do they need uuids at all? why are standard xml-ids not enough?) removed definition body's language attribute

// furter simplified below: removed definitions all together


new File('simpleExample.mvdxml').withWriter { writer ->
  def builder = new MarkupBuilder(writer)
  builder.mvcXML(name:'example MVD for mvdXML documentation sensor signals', 
    xmlns: 'http://buildingsmart-tech.org/mvd/XML/1.1',
    'xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance',
    'xsi:schemaLocation': 'http://buildingsmart-tech.org/mvd/XML/1.1 ../mvdXML_V1.1.xsd'){
    Templates { ConceptTemplate ( 
      uuid: '00000000-0000-0000-0000-000000000001', 
      name: 'Port Assigment',
      applicableSchema: 'IFC4', 
      applicableEntity: 'IfcDistributionElement'){
      Rules { AttributeRule(AttributeName: 'IsNestedBy'){
        EntityRules { EntityRule(EntityName: 'IfcRelNests') {
          AttributeRules { AttributeRule(AttributeName: 'RelatedObjects' ) {
            EntityRules { EntityRule(EntityName: 'IfcDistributionPort') {
              AttributeRules { 
                AttributeRule(AttributeName:'Name', RuleId:'Name')
                AttributeRule(AttributeName:'PredefinedType', RuleId:'Type')
                AttributeRule(AttributeName:'FlowDirection', RuleId:'Flow')
              }
            }}
          }}
        }}
      }}
    }}
    Views { ModelView(
      uuid: '00000000-0000-0000-0000-000000000002', 
      name: 'Sensor signal view', 
      applicableSchema: 'IFC4',
      code: 'Sensor') { 
      ExchangeRequirements { ExchangeRequirement(
        uuid: '00000000-0000-0000-0000-000000000003', 
        name: 'Distribution signal', 
        code: 'ERM1', 
        applicability: 'export') {
      }}
      Roots { ConceptRoot(
        uuid: '00000000-0000-0000-0000-000000000004', 
        name: 'Sensor',
        applicableRootEntity: 'IfcSensor'){ 
        Concepts { Concept(name: 'Port Assignment') { 
          uuid: '00000000-0000-0000-0000-000000000005', 
          Template(ref: '00000000-0000-0000-0000-00000001')
          Requirements { Requirement(
            exchangeRequirement: '00000000-0000-0000-0000-000000000003', 
            requirement: 'mandatory', 
            applicability: 'export') }
          TemplateRules { TemplateRule(
            Parameters: "Name[Value]='Output' AND Type[Value]='SIGNAL' AND Flow[Value]='SOURCE'", 
            Description: 'Transmits signal.') {
          }}
        }}
      }}
    }}
  }
}


// Templates (applicable entity implicit, reference TODO by var name?, uuid only generated when published)
concept1 = IfcDistributionElement  {
  isNestedBy: IfcRelNests {
    relatedObjects: IfcDistributionPort {
      name: IfcLabel
      predefinedType: IfcDistributionPortTypeEnum
      flowDirection: IfcFlowDirectionEnum
    }
  }
}

// Roots
IfcSensor {
  concept1.isNestedBy.relatedObjects[ name == 'Output' || type == 'SIGNAL' || flow == 'SOURCE' ]
}

// for now no exchange requirements needed (just make one behind the scenes and stuff everything inside) but should keep them in mind

