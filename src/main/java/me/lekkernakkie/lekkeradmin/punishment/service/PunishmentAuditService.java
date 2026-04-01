package me.lekkernakkie.lekkeradmin.punishment.service;

import me.lekkernakkie.lekkeradmin.LekkerAdmin;
import me.lekkernakkie.lekkeradmin.database.repository.AuditLogRepository;
import me.lekkernakkie.lekkeradmin.model.audit.AuditLogEntry;
import me.lekkernakkie.lekkeradmin.model.punishment.PunishmentEntry;

public class PunishmentAuditService {

    private final AuditLogRepository repository;

    public PunishmentAuditService(LekkerAdmin plugin) {
        this.repository = new AuditLogRepository(plugin);
    }

    public void log(String action, String actorName, String actorUuid, PunishmentEntry entry) {

        AuditLogEntry log = new AuditLogEntry();

        log.setActionType(action);
        log.setActorName(actorName);
        log.setActorId(actorUuid);
        log.setTargetName(entry.getMinecraftName());
        log.setTargetId(entry.getMinecraftUuid());
        log.setDetails(entry.getPunishmentType() + " reason=" + entry.getReason());
        log.setCreatedAt(System.currentTimeMillis());

        repository.save(log);
    }
}