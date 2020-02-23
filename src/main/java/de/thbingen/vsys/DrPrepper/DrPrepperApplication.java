package de.thbingen.vsys.DrPrepper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.thbingen.vsys.DrPrepper.config.DnaSequenceProducer;
import de.thbingen.vsys.DrPrepper.model.Metadata;
import de.thbingen.vsys.DrPrepper.model.TransferableFile;
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

@SpringBootApplication
public class DrPrepperApplication {

    @Autowired
    private DnaSequenceProducer dnaSequenceProducer;

	public static void main(String[] args) {
		SpringApplication.run(DrPrepperApplication.class, args);
	}

	@KafkaListener(topics = "${kafka.topic.file}", containerFactory = "fileKafkaListenerContainerFactory")
	public void listenWithHeaders(Acknowledgment ack, @Payload String file) {
        TransferableFile transferableFile = null;
        try {
            transferableFile = new ObjectMapper().readValue(file, TransferableFile.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        ArrayList<String> dnaSequences = extractDnaSequences(transferableFile);
        Metadata metadata = new Metadata(transferableFile);
        // Send sequences
        dnaSequenceProducer.sendSequencesInTransaction(metadata,dnaSequences);
        // Commit to files topic
        ack.acknowledge();
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
            e.printStackTrace();
        }
        return result;
    }

}
