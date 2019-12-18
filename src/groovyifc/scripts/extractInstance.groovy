// usage: groovy extractInstance.groovy <input.ifc> #<entity number> <extracted.ifc>

import groovy.transform.Field

if (args.size() < 3) {
  println "usage: groovy extractInstance.groovy <input.ifc> #<entity number> <output.ifc>"
  System.exit(1)
}

def pattern = /#[0-9]+/
table = [:]
include = [] as Set

def schemaPattern = /FILE_SCHEMA\(\((.*)\)\)/
@Field schema


originalFile = new File(args[0])
originalFile.eachLine{ line ->
  def schemaMatch = (line.replaceAll("\\s","") =~ schemaPattern).collect{it[1]}
  if (schemaMatch) { schema = schemaMatch[0][1..-2]; return }
  def matches = (line =~ pattern).collect{ it as String } // workaround failing groovy magic
  if (!matches) return
  table[matches[0]] = [line, matches.size() > 1 ? matches[1..-1] : []]
}

def traverseRefs(root, todo){
  assert table[root]
  todo(root)
  table[root][1].each{ ref ->
    traverseRefs(ref, todo)
  }
}

entities = args[1].split(',')
entities.each{ searched ->
  if (table[searched]) {
    traverseRefs(searched, {no -> include << no})
    println "Finished extracting entity $searched"
  } else {
    println "Enitity $searched not found in ${args[0]}"
  }
}
writeExtract()

def writeExtract(){
  def file = new File(args[2])
  def date = new Date().format("yyyy-MM-dd'T'HH:mm:ss", TimeZone.getTimeZone("UTC")) 
  file.withWriter{ writer ->
    writer << """ISO-10303-21;
HEADER;
FILE_DESCRIPTION(('Extracted entit${entities.size()>1?'ies':'y'} ${entities.join(',#')} from $originalFile.name'),'2;1');
FILE_NAME('$file.name','$date',('',''),(''),'','','');
FILE_SCHEMA(('$schema'));
ENDSEC;

DATA;
""".denormalize()
    include = include.sort{ Integer.valueOf(it[1..-1])}
    def extracted = include.collect{ table[it][0] }
    include.eachWithIndex{ no, i -> (0..extracted.size-1).each{ k -> extracted[k] = extracted[k].replaceAll( "$no(\\D)","#${i+1}\$1") } } 
    extracted.each{ writer.writeLine(it) }
    writer << 'ENDSEC;\nEND-ISO-10303-21;'
  }
} 

// TODO: extract specific instances via some GPath-like syntax, needs proper parser


