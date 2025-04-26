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

    public List<aws.ec2connect.dto.SecurityGroup> getSecurityGroupRules(String groupId){

        List<aws.ec2connect.dto.SecurityGroup> securityGroupsDTO = new ArrayList<>();

        DescribeSecurityGroupsRequest describeSecurityGroupsRequest = DescribeSecurityGroupsRequest.builder().groupIds(groupId).build();

        DescribeSecurityGroupsResponse describeSecurityGroupsResponse = ec2Client.describeSecurityGroups(describeSecurityGroupsRequest);
        List<SecurityGroup> securityGroups = describeSecurityGroupsResponse.securityGroups();

        for(SecurityGroup securityGroup : securityGroups){
            String groupName = securityGroup.groupName();

            List<String> inboundRules = securityGroup.ipPermissions()
                    .stream()
                    .map(IpPermission::toString)
                    .toList();

            List<String> outboundRules = securityGroup.ipPermissionsEgress()
                    .stream()
                    .map(IpPermission::toString)
                    .toList();

            aws.ec2connect.dto.SecurityGroup info = new aws.ec2connect.dto.SecurityGroup(groupName, inboundRules, outboundRules);
            securityGroupsDTO.add(info);
        }

        return securityGroupsDTO;
    }

    public List<String> getAllSecurityGroups(){
        List<String> securityGroupsResponse = new ArrayList<>();
        DescribeSecurityGroupsRequest describeSecurityGroupsRequest = DescribeSecurityGroupsRequest.builder().build();
        DescribeSecurityGroupsResponse describeSecurityGroupsResponse = ec2Client.describeSecurityGroups(describeSecurityGroupsRequest);
        List<SecurityGroup> securityGroups = describeSecurityGroupsResponse.securityGroups();
        for(SecurityGroup securityGroup : securityGroups){
            securityGroupsResponse.add(
                    "Security Group Name: " + securityGroup.groupName() +
                    "Security Group Id:" + securityGroup.groupId() +
                    "VPC Id:" + securityGroup.vpcId() +
                    "Description:" + securityGroup.description());
        }
        return securityGroupsResponse;
    }

    public String createSecurityGroup(String groupName,
                                            String groupDescription,
                                            String vpcId,
                                            String myIpAddress)
    {
        CreateSecurityGroupRequest createRequest = CreateSecurityGroupRequest.builder()
                .groupName(groupName)
                .description(groupDescription)
                .vpcId(vpcId)
                .build();
        IpRange ipRange = IpRange.builder()
                .cidrIp(myIpAddress + "/32")
                .build();

        CreateSecurityGroupResponse createSecurityGroupResponse=  ec2Client.createSecurityGroup(createRequest);
        IpPermission inboundRule1 = IpPermission.builder()
                .ipProtocol("tcp")
                .fromPort(22) // Port 22 for SSH
                .toPort(22)
                .ipRanges(ipRange)
                .build();

        IpPermission inboundRule2 = IpPermission.builder()
                .ipProtocol("tcp")
                .toPort(80) // Port for HTTP
                .fromPort(80)
                .ipRanges(ipRange)
                .build();

        AuthorizeSecurityGroupIngressRequest ingressRequest = AuthorizeSecurityGroupIngressRequest.builder()
                .groupName(groupName)
                .ipPermissions(inboundRule1, inboundRule2)
                .build();

        ec2Client.authorizeSecurityGroupIngress(ingressRequest);

        return "Security Group created";
    }

    public List<String> getInstanceTypes() {
        List<String> instanceTypesResponse = new ArrayList<>();
        DescribeInstanceTypesRequest typesRequest = DescribeInstanceTypesRequest.builder()
                .maxResults(15)
                .build();

        DescribeInstanceTypesResponse response = ec2Client.describeInstanceTypes(typesRequest);
        for(InstanceTypeInfo instanceTypes : response.instanceTypes()){
            instanceTypesResponse.add(
                    "Instance Type: " + instanceTypes.instanceType().toString()+
                            ", Memory: " + instanceTypes.memoryInfo().sizeInMiB() + " MiB"
            );
        }
        return instanceTypesResponse;
    }

    public List<String> getImagesForRedHat(){
        List<String> imageResponse = new ArrayList<>();
        DescribeImagesRequest describeImagesRequest = DescribeImagesRequest.builder()
                .owners("309956199498")
                .maxResults(5)
                .build();

        DescribeImagesResponse describeImagesResponse = ec2Client.describeImages(describeImagesRequest);
        List<Image> images = describeImagesResponse.images();

        for (Image image : images) {
            imageResponse.add(
                    "AMI ID: " + image.imageId() +
                    ", Name: " + image.name() +
                            ", Description: " + image.description()
            );
        }
        return imageResponse;
    }

//    public List<String> getImagesForWindows(){
//        List<String> imageResponse = new ArrayList<>();
//        DescribeImagesRequest describeImagesRequest = DescribeImagesRequest.builder()
//                .filters(
//                        Filter.builder()
//                                .name("name")  // This is the key for the filter (filtering by image name)
//                                .values("Windows_Server-2022-*")  // Pattern to match AMI names
//                                .build())
//                .maxResults(25)  // Limit to 25 results
//                .build();
//
//        DescribeImagesResponse describeImagesResponse = ec2Client.describeImages(describeImagesRequest);
//        List<Image> images = describeImagesResponse.images();
//
//        for (Image image : images) {
//            imageResponse.add(
//                    "AMI ID: " + image.imageId() +
//                            ", Name: " + image.name()+
//                            ", Description: " + image.description()
//            );
//        }
//        return imageResponse;
//    }
    
}
