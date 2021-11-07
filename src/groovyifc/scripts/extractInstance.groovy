// usage: groovy extractInstance.groovy <input.ifc> #<entity number> <extracted.ifc>

import groovy.transform.Field

if (args.size() < 3) {
  println "usage: groovy extractInstance.groovy <input.ifc> #<entity number> <output.ifc>"
  System.exit(1)
}

boolean reindex = false // There must be a more efficient way using the table to reindex
def pattern = /#[0-9]+/
table = [:]
include = [] as HashSet
isLinux = false // System.properties['os.name'].toLowerCase().contains('linux')

def schemaPattern = /FILE_SCHEMA\(\((.*)\)\)/
@Field schema

originalFile = new File(args[0])
originalFile.eachLine{ line, i ->
  def schemaMatch = (line.replaceAll("\\s","") =~ schemaPattern).collect{it[1]}
  if (schemaMatch) { schema = schemaMatch[0][1..-2]; return }
  if(!isLinux){ // TODO fully skip
    def matches = (line =~ pattern).collect{ it as String } // workaround failing groovy magic
    if (!matches) return
    int mi = matches[0][1..-1] as int
    // println mi
    table[mi] = [i, matches.size() > 1 ? matches[1..-1].collect{it[1..-1] as int} : []]
  }
}


def traverseRefs(root, todo, lookup){
  todo(root)
  lookup(root).each{ ref ->
    traverseRefs(ref, todo, lookup)
  }
}

def grepEntity(no, out, err){
  println "search $no"
  def proc = ['grep',"$no\\s*=", args[0]].execute()
  proc.consumeProcessOutput(out, err)
  proc.waitForOrKill(5000) 
  proc.exitValue()
}

data = new StringBuffer()
entities = args[1].split(',')
entities.each{ searched ->
  if (isLinux){
    println args[0]
    traverseRefs(searched, {no -> include << no}, {no -> 
      def out = new StringBuffer()
      def err = new StringBuffer()
      if (!grepEntity(no, out, err)){ // exitValue 0: successful grep
        data << out.toString() // table[no] = [out] // toString?
        def matches = (out =~ pattern).collect{ it as String } // not necessary here?
        (matches && matches.size()>1) ? matches[1..-1] - include : []
      }
    })
  } else {
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
}

writeExtract( isLinux ? {
  data.toString().split('\n')
} : {
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
})

def writeExtract(collectData){
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
    */ 
    extracted.each{ writer.writeLine(it) }
    writer << 'ENDSEC;\nEND-ISO-10303-21;'
    println "Finished writing"
  }
} 

// TODO: extract specific instances via some GPath-like syntax, needs proper parser


