package aws.ec2connect.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.ArrayList;
import java.util.List;

@Service
public class EC2Service {

    private final Ec2Client ec2Client;

    public EC2Service(Ec2Client ec2Client) {
        this.ec2Client = ec2Client;
    }

    public List<String> getInstances() {
        List<String> instances = new ArrayList<>();
        DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder().build();
        DescribeInstancesResponse describeInstancesResponse = ec2Client.describeInstances(describeInstancesRequest);
        describeInstancesResponse.reservations().stream()
                .flatMap(reservation -> reservation.instances().stream())
                .forEach(instance -> instances.add("Instance ID: " + instance.instanceId() +
                        ", status:" + instance.state().name() +
                        ", type:" + instance.instanceType().name() +
                        ", platform:" + instance.platformDetails() +
                        ", publicDnsName:" + instance.publicDnsName()));
        return instances;
    }

    public String stopInstance(String instanceId) {
        DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder().
                instanceIds(instanceId).build();
        DescribeInstancesResponse describeResponse = ec2Client.describeInstances(describeInstancesRequest);
        String status  = describeResponse.reservations().getFirst().instances().getFirst().state().nameAsString();
        if (status.equalsIgnoreCase("stopped")) {
            return "Instance " + instanceId + " is already stopped.";
        }
        StopInstancesRequest stopInstancesRequest = StopInstancesRequest.builder().instanceIds(instanceId).build();
        StopInstancesResponse stopInstancesResponse = ec2Client.stopInstances(stopInstancesRequest);
        return "Stopping instance: " + stopInstancesResponse.stoppingInstances().getFirst().instanceId() +". It will take a few minutes to perform all checks.";
    }

    public String startInstance(String instanceId) {
        DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder().instanceIds(instanceId).build();
        DescribeInstancesResponse describeResponse = ec2Client.describeInstances(describeInstancesRequest);
        String status  = describeResponse.reservations().getFirst().instances().getFirst().state().nameAsString();
        if (status.equalsIgnoreCase("running")) {
            return "Instance " + instanceId + " is already started.";
        }
        StartInstancesRequest startInstancesRequest = StartInstancesRequest.builder().instanceIds(instanceId).build();
        StartInstancesResponse startInstancesResponse = ec2Client.startInstances(startInstancesRequest);
        return "Starting instance: " + startInstancesResponse.startingInstances().getFirst().instanceId();
    }

    public String getInstanceDetails(String instanceId) {
        String instanceDetails;
        DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder().instanceIds(instanceId).build();
        DescribeInstancesResponse describeInstancesResponse = ec2Client.describeInstances(describeInstancesRequest);
        Instance instance = describeInstancesResponse.reservations().getFirst().instances().getFirst();

        instanceDetails = "Instance ID: " + instance.instanceId() +
                ", status: " + instance.state().name() +
                ", type: " + instance.instanceType().name() +
                ", platform: " + instance.platformDetails() +
                ", AMI: " + instance.imageId()+
                ", AZ: " + instance.placement().availabilityZone() +
                ", public IPv4: " + instance.publicIpAddress() +
                ", private IPv4: " + instance.privateIpAddress() +
                ", VPC: " + instance.vpcId() +
                ", Security Groups: " + instance.securityGroups().getFirst() +
                ", publicDnsName: " + instance.publicDnsName();

        return instanceDetails;
    }
    
}
