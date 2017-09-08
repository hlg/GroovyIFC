package groovy.utils;

import ifc4javatoolbox.step.parser.util.ProgressEvent
import ifc4javatoolbox.step.parser.util.StepParserProgressListener;

public class IfcModel extends ifc4javatoolbox.ifcmodel.IfcModel {

    def builder

    void setProgressListener(callback = { ProgressEvent event-> print ((event.currentState % 20) ? '.' : '.\n')}){
        addStepParserProgressListener([progressActionPerformed: callback] as StepParserProgressListener)
    }

    void setFileName(String fileName) {
        if(typeCacheEnabled) clearModel()
        readStepFile(new File(fileName))
        builder = new IfcBuilder(model: this)
    }
}
