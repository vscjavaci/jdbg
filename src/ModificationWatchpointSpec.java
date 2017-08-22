package jdbg;

import com.sun.jdi.*;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.EventRequest;

class ModificationWatchpointSpec extends WatchpointSpec {
    ModificationWatchpointSpec(ReferenceTypeSpec refSpec, String fieldId)
                                  throws MalformedMemberNameException {
        super(refSpec, fieldId);
    }

    /**
     * The 'refType' is known to match, return the EventRequest.
     */
    @Override
    EventRequest resolveEventRequest(ReferenceType refType)
                                      throws NoSuchFieldException {
        Field field = refType.fieldByName(fieldId);
        EventRequestManager em = refType.virtualMachine().eventRequestManager();
        EventRequest wp = em.createModificationWatchpointRequest(field);
        wp.setSuspendPolicy(suspendPolicy);
        wp.enable();
        return wp;
    }

    @Override
    public String toString() {
        return MessageOutput.format("watch modification of",
                                    new Object [] {refSpec.toString(),
                                                   fieldId});
    }
}
