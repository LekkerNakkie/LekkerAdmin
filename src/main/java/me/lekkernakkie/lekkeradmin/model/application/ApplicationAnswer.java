package me.lekkernakkie.lekkeradmin.model.application;

public class ApplicationAnswer {

    private final String fieldKey;
    private final String fieldLabel;
    private final String fieldValue;
    private final int fieldOrder;

    public ApplicationAnswer(String fieldKey, String fieldLabel, String fieldValue, int fieldOrder) {
        this.fieldKey = fieldKey;
        this.fieldLabel = fieldLabel;
        this.fieldValue = fieldValue;
        this.fieldOrder = fieldOrder;
    }

    public String getFieldKey() {
        return fieldKey;
    }

    public String getFieldLabel() {
        return fieldLabel;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public int getFieldOrder() {
        return fieldOrder;
    }
}