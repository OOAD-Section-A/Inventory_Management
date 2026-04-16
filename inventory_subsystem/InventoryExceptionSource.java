package inventory_subsystem;

import com.scm.exceptions.*;
import com.scm.exceptions.categories.*;

public class InventoryExceptionSource implements
IResourceAvailabilityExceptionSource,
IConcurrencyExceptionSource,
IDataIntegrityExceptionSource {

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
public void fireResourceNotFound(int exceptionId, String resourceType, String resourceId) {
    raise(exceptionId, Severity.MAJOR,
            resourceType + " not found: " + resourceId);
}

@Override
public void fireResourceExhausted(int exceptionId, String resourceType,
                                  String resourceId, int requested, int available) {

    raise(exceptionId, Severity.MAJOR,
            resourceType + " insufficient: " + resourceId +
            " requested=" + requested + " available=" + available);
}

@Override
public void fireResourceBlocked(int exceptionId, String resourceType,
                               String resourceId, String reason) {

    raise(exceptionId, Severity.MINOR,
            resourceType + " blocked: " + resourceId + " reason=" + reason);
}

@Override
public void fireCapacityExceeded(int exceptionId, String resourceType,
                                 String resourceId, int limit) {

    raise(exceptionId, Severity.MAJOR,
            resourceType + " capacity exceeded: " + resourceId + " limit=" + limit);
}

@Override
public void fireConflict(int exceptionId, String entityType,
                         String entityId, String conflictReason) {

    raise(exceptionId, Severity.WARNING,
            entityType + " conflict: " + entityId + " reason=" + conflictReason);
}

@Override
public void fireDeadlock(int exceptionId, String entityType, String entityId, String operation) {
    raise(exceptionId, Severity.MAJOR,
            "Deadlock on " + entityType + " " + entityId + " during " + operation);
}

@Override
public void fireRollbackFailed(int exceptionId, String entityType, String entityId) {
    raise(exceptionId, Severity.MAJOR,
            "Rollback failed for " + entityType + " " + entityId);
}

@Override
public void fireDuplicateSubmission(int exceptionId, String entityType, String entityId) {
    raise(exceptionId, Severity.MINOR,
            "Duplicate submission for " + entityType + " " + entityId);
}

@Override
public void fireDuplicateRecord(int exceptionId, String entityType, String duplicateKey) {
    raise(exceptionId, Severity.MINOR,
            entityType + " duplicate: " + duplicateKey);
}

@Override
public void fireReferentialViolation(int exceptionId, String childEntity,
                                     String parentEntity, String key) {

    raise(exceptionId, Severity.MAJOR,
            "Referential violation: " + childEntity +
            " missing " + parentEntity + " key=" + key);
}

@Override
public void fireDataInconsistency(int exceptionId, String entityType,
                                  String entityId, String description) {

    raise(exceptionId, Severity.MAJOR,
            "Data inconsistency: " + entityType +
            " " + entityId + " " + description);
}

@Override
public void fireWriteFailure(int exceptionId, String entityType,
                             String entityId, String operation) {

    raise(exceptionId, Severity.MAJOR,
            "Write failure: " + entityType +
            " " + entityId + " operation=" + operation);
}

}
