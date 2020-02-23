package de.thbingen.vsys.DrPrepper.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.thbingen.vsys.DrPrepper.model.DnaSequence;
import de.thbingen.vsys.DrPrepper.model.Metadata;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.List;
import java.util.UUID;

@Component
@Data
public class DnaSequenceProducer {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value(value = "${kafka.topic.sequence}")
    private String topicName;

    public void sendMessage(String message) {

        ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(topicName, message);

        future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {

            @Override
            public void onSuccess(SendResult<String, String> result) {
                System.out.println("Sent message=[" + message + "] with offset=[" + result.getRecordMetadata().offset() + "]");
            }
            @Override
            public void onFailure(Throwable ex) {
                System.out.println("Unable to send message=[" + message + "] due to : " + ex.getMessage());
            }
        });
    }

    public boolean sendSequencesInTransaction(Metadata metadata, List<String> dnaSequences) {
        boolean result = kafkaTemplate.executeInTransaction(t -> {
            for (String dnaSequence: dnaSequences) {
                DnaSequence send = new DnaSequence(metadata, dnaSequence);
                String json = null;
                try {
                    json = new ObjectMapper().writeValueAsString(send);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    return false;
                }
                t.send(UUID.fromString(dnaSequence).toString(), json);
            }
            return true;
        });
        return result;
    }
}