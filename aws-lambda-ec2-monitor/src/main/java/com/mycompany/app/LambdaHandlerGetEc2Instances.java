package com.mycompany.app;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;

import java.util.List;
import java.util.stream.Collectors;

public class LambdaHandlerGetEc2Instances implements RequestHandler<Object, List<String>> {

    private static final Ec2Client ec2Client = Ec2Client.create();

    @Override
    public List<String> handleRequest(Object object, Context context) {
        DescribeInstancesRequest request = DescribeInstancesRequest.builder().build();
        DescribeInstancesResponse response = ec2Client.describeInstances(request);

        return response.reservations().stream()
                .flatMap(reservation -> reservation.instances().stream())
                .map(instance -> "Instance ID: " + instance.instanceId() +
                        ", status:" + instance.state().name() +
                        ", type:" + instance.instanceType().name() +
                        ", platform:" + instance.platformDetails() +
                        ", publicDnsName:" + instance.publicDnsName())
                .collect(Collectors.toList());
    }
}
