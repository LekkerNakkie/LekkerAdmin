package me.lekkernakkie.lekkeradmin.service.audit;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.database.repository.AuditLogRepository;
import me.lekkernakkie.lekkeradmin.model.audit.AuditLogEntry;

public class AuditLogService {

    private final AuditLogRepository repository;

    public AuditLogService(LekkerAdmin plugin) {
        this.repository = new AuditLogRepository(plugin);
    }

    public void save(AuditLogEntry entry) {
        repository.save(entry);
    }

    public void log(String actionType,
                    String actorId,
                    String actorName,
                    String targetId,
                    String targetName,
                    String applicationId,
                    String details) {

        AuditLogEntry entry = new AuditLogEntry();
        entry.setActionType(actionType);
        entry.setActorId(actorId);
        entry.setActorName(actorName);
        entry.setTargetId(targetId);
        entry.setTargetName(targetName);
        entry.setApplicationId(applicationId);
        entry.setDetails(details);
        entry.setCreatedAt(System.currentTimeMillis());

        save(entry);
    }
}