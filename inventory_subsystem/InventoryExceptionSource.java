package inventory_subsystem;

import com.scm.exceptions.*;
import com.scm.exceptions.categories.*;

public class InventoryExceptionSource implements
        IResourceAvailabilityExceptionSource,
        IConcurrencyExceptionSource {

    private SCMExceptionHandler handler;

    @Override
    public void registerHandler(SCMExceptionHandler handler) {
        this.handler = handler;
    }

    private void raise(int id, Severity severity, String detail) {
        if (handler == null) return;

        SCMExceptionEntry entry = SCMExceptionRegistry.getInstance().get(id);

        handler.handle(new SCMExceptionEvent(
                id,
                entry.getName(),
                severity,
                entry.getSubsystem(),
                entry.getErrorMessage(),
                detail
        ));
    }

    @Override
    public void fireResourceNotFound(int id, String type, String resourceId) {
        raise(id, Severity.MAJOR,
                type + " not found: " + resourceId);
    }

    @Override
    public void fireResourceExhausted(int id, String type,
                                      String resourceId,
                                      int requested, int available) {

        raise(id, Severity.MAJOR,
                type + " insufficient: " + resourceId +
                        " requested=" + requested +
                        " available=" + available);
    }

    @Override
    public void fireConflict(int id, String entityType,
                             String entityId, String reason) {

        raise(id, Severity.WARNING,
                entityType + " conflict: " + entityId +
                        " reason=" + reason);
    }
}