package com.mycompany.app;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import com.fasterxml.jackson.databind.*;

import java.util.HashMap;
import java.util.Map;

public class LambdaHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private static final String SNS_TOPIC_ARN = "xxxxx";  // Replace with your actual SNS Topic ARN
    private static final String PHONE_NUMBER = "xxxxxx"; // Replace with the user's phone number

    private static final CloudWatchClient cloudWatch = CloudWatchClient.create();
    private static final SnsClient snsClient = SnsClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {


        Map<String, String> queryParams = (Map<String, String>) event.get("queryStringParameters");

        String instanceId = queryParams.get("instanceId");
        String alarmName = queryParams.get("alarmName");
        double threshold = Double.parseDouble(queryParams.get("threshold"));

        if (instanceId == null || instanceId.isEmpty()) {
             return generateResponse(400, "Error: EC2 instance ID must be provided.");
        }
        if (alarmName == null || alarmName.isEmpty()) {
            return generateResponse(400, "Error: Alarm name must be provided.");
        }

        try {
            // Step 1: Create CloudWatch Alarm
            createCPUAlarm(instanceId, alarmName, threshold);

            // Step 2: Trigger SMS notification via SNS
            sendSMSNotification(instanceId, threshold);

            return generateResponse(200, "Alarm created and SMS sent successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            return generateResponse(400, "Error occurred: " + e.getMessage());
        }
    }


    private void createCPUAlarm(String instanceId, String alarmName, double threshold) {

        // Define the alarm
        PutMetricAlarmRequest request = PutMetricAlarmRequest.builder()
                .alarmName(alarmName)
                .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
                .threshold(threshold)
                .period(300)  // 5-minute evaluation period
                .evaluationPeriods(1)
                .statistic(Statistic.AVERAGE)
                .actionsEnabled(true)
                .alarmActions(SNS_TOPIC_ARN)  // SNS Topic ARN for the action (sending SMS)
                .namespace("AWS/EC2")  // Namespace for EC2 metrics
                .metricName("CPUUtilization")  // Metric name
                .dimensions(Dimension.builder()  // Set dimension for the EC2 instance
                        .name("InstanceId")
                        .value(instanceId)
                        .build())
                .build();

        // Create the alarm
        cloudWatch.putMetricAlarm(request);
        System.out.println("CloudWatch Alarm for CPU utilization created successfully for EC2 instance: " + instanceId);
    }

    private void sendSMSNotification(String instanceId, double threshold) {

        // Define the message
        String message = "ALERT: A CloudWatch alarm for high CPU utilization over " + threshold + " % has been created for your EC2 instance " + instanceId + " !";

        PublishRequest publishRequest = PublishRequest.builder()
                .message(message)
                .phoneNumber(PHONE_NUMBER)
                .build();

        snsClient.publish(publishRequest);
        System.out.println("SMS sent successfully!");
    }

    private Map<String, Object> generateResponse(int statusCode, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", statusCode);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        response.put("headers", headers);

        try {
            response.put("body", objectMapper.writeValueAsString(new Message(message)));
        } catch (Exception e) {
            response.put("body", "{\"message\":\"Serialization error\"}");
        }

        return response;
    }

    // Helper class for the response body
    public static class Message {
        public String message;

        public Message(String message) {
            this.message = message;
        }
    }
}