package de.thbingen.vsys.DrPrepper.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DnaSequence {
    Metadata metadata;
    String dna_sequence;
}
