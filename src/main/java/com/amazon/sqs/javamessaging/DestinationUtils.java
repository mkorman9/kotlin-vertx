package com.amazon.sqs.javamessaging;

public class DestinationUtils {
    public static SQSQueueDestination createDestination(String queueName, String queueUrl) {
        return new SQSQueueDestination(queueName, queueUrl);
    }
}
