// usage: groovy extractInstance.groovy <input.ifc> #<entity number> <extracted.ifc>

import groovy.transform.Field

if (args.size() < 3) {
  println "usage: groovy extractInstance.groovy <input.ifc> #<entity number> <output.ifc>"
  System.exit(1)
}

def pattern = /#[0-9]+/
table = [:]
include = [] as HashSet
reindex = false // There must be a more efficient way using the table to reindex

def schemaPattern = /FILE_SCHEMA\(\((.*)\)\)/
@Field schema

originalFile = new File(args[0])
originalFile.eachLine{ line, i ->
  def schemaMatch = (line.replaceAll("\\s","") =~ schemaPattern).collect{it[1]}
  if (schemaMatch) { schema = schemaMatch[0][1..-2]; return }
  def matches = (line =~ pattern).collect{ it as String } // workaround failing groovy magic
  if (!matches) return
  int mi = matches[0][1..-1] as int
  // println mi
  table[mi] = [i, matches.size() > 1 ? matches[1..-1].collect{it[1..-1] as int} : []]
}


def traverseRefs(root, todo, lookup){
  todo(root)
  lookup(root).each{ ref ->
    traverseRefs(ref, todo, lookup)
  }
}

def collectData(){
  includeIt = include.sort().iterator() // should already be sorted
  println "including ${include.size()} elements"
  def current = includeIt.next()
  def included = []
  originalFile.eachLine{ line, i ->
     if(i==current) {
       // println i
       included << line
       if(includeIt.hasNext()){
         current = includeIt.next()
       }
     }
  }
  included
}

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
    extracted = collectData() // don't put this in memory first, replace and write on the go
    println "Finished collecting"
    if(reindex) include.eachWithIndex{ no, i -> 
       (0..extracted.size()-1).each{ k -> extracted[k] = extracted[k].replaceAll( "$no(\\D)","#${i+1}\$1") } 
    } 
    extracted.each{ writer.writeLine(it) }
    writer << 'ENDSEC;\nEND-ISO-10303-21;'
    println "Finished writing"
  }
} 

// TODO: extract specific instances via some GPath-like syntax, needs proper parser

entities = args[1].split(',')
entities.each{ searched ->
  def searchedIdx = searched[1..-1] as int
  if (table[searchedIdx]) {
    traverseRefs(searchedIdx, {no ->
      // assert table[no]  
      include << table[no][0]
    }, {no -> table[no][1]})
    println "Finished extracting entity $searched"
  } else {
    println "Enitity $searched not found in ${args[0]}"
  }
}
writeExtract()

