package de.thbingen.vsys.DrPrepper.model;

import lombok.Data;

@Data
public class Metadata {
    private String id;
    private String group;
    private String timestamp;
    private String name;
    private boolean passed;

    public Metadata(TransferableFile transferableFile) {
        this.id = transferableFile.getUuid().toString();
        this.group = transferableFile.getGroupName();
        this.timestamp = transferableFile.getTimestamp().toString();
        this.name = transferableFile.getGroupName();
        this.passed = transferableFile.isExperimentSuccessful();
    }
}
