package me.lekkernakkie.lekkeradmin.model.application;

public enum ApplicationStatus {
    PENDING_NAME_VALIDATION,
    PENDING_REVIEW,
    APPROVED,
    APPROVED_PENDING_NAME_FIX,
    DENIED,
    COMPLETED
}