package de.thbingen.vsys.DrPrepper.model;

import lombok.Data;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
public class TransferableFile {
    final private UUID uuid;
    final private String experimentName;
    final private String groupName;
    final private Instant timestamp;
    final private boolean experimentSuccessful;
    final private Path filePath;

}
