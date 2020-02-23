package de.thbingen.vsys.DrPrepper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.thbingen.vsys.DrPrepper.config.DnaSequenceProducer;
import de.thbingen.vsys.DrPrepper.model.Metadata;
import de.thbingen.vsys.DrPrepper.model.TransferableFile;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Properties;

@Slf4j
@SpringBootApplication
public class DrPrepperApplication {

    @Autowired
    private DnaSequenceProducer dnaSequenceProducer;

	public static void main(String[] args) {
		SpringApplication.run(DrPrepperApplication.class, args);
	}

	@KafkaListener(topics = "${kafka.topic.file}", containerFactory = "fileKafkaListenerContainerFactory")
	public void listenWithHeaders(Acknowledgment ack, @Payload String file) {
	    log.debug("DrPrepper - listenWithHeaders(): Start method for file "+file+".");
        TransferableFile transferableFile = null;
        try {
            transferableFile = new ObjectMapper().readValue(file, TransferableFile.class);
        } catch (JsonProcessingException e) {
            log.debug("DrPrepper - listenWithHeaders(): Can't create transferable file object for file "+file+".");
            e.printStackTrace();
        }
        log.debug("DrPrepper - listenWithHeaders(): File "+file+" successfully read.");
        ArrayList<String> dnaSequences = extractDnaSequences(transferableFile);
        log.debug("DrPrepper - listenWithHeaders(): dna sequences extracted.");
        Metadata metadata = new Metadata(transferableFile);
        // Send sequences
        log.debug("DrPrepper - listenWithHeaders(): start writing message queue in one transaction.");
        dnaSequenceProducer.sendSequencesInTransaction(metadata,dnaSequences);
        log.debug("DrPrepper - listenWithHeaders(): finished writing message queue in one transaction.");
        // Commit to files topic
        ack.acknowledge();
        log.debug("DrPrepper - listenWithHeaders(): acknowledge file "+file+".");
    }

	private ArrayList<String> extractDnaSequences(TransferableFile file) {
        ArrayList<String> result = new ArrayList<>();
        try {
            for (String line: Files.readAllLines(file.getFilePath())) {
                if (line.matches("^[ACGT]+$")) {
                    result.add(line);
                }
            }
        } catch (IOException e) {
            log.debug("DrPrepper - extractDnaSequences(): Failed to read file "+file.getFilePath()+".");
            e.printStackTrace();
        }
        return result;
    }

}
