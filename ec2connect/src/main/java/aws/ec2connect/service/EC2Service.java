package aws.ec2connect.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsRequest;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsResponse;
import software.amazon.awssdk.services.cloudwatch.model.MetricAlarm;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.ArrayList;
import java.util.List;

@Service
public class EC2Service {

    private final Ec2Client ec2Client;
    private final PricingService pricingService;
    private final CloudWatchClient cloudWatchClient;

    public EC2Service(Ec2Client ec2Client,
                      PricingService pricingService,
                      CloudWatchClient cloudWatchClient) {
        this.ec2Client = ec2Client;
        this.pricingService = pricingService;
        this.cloudWatchClient = cloudWatchClient;
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

    public List<String> getInstanceTypes(String instanceTypeName) {
//        List<String> instanceTypesResponse = new ArrayList<>();
//        DescribeInstanceTypesRequest typesRequest = DescribeInstanceTypesRequest.builder()
//                .build();
//
//        DescribeInstanceTypesResponse response = ec2Client.describeInstanceTypes(typesRequest);
//        for (InstanceTypeInfo instanceTypes : response.instanceTypes().stream()
//                .filter(instance ->
//                        instance.instanceType().toString().equalsIgnoreCase(instanceTypeName) ||
//                                instance.instanceType().toString().toLowerCase().contains(instanceTypeName.toLowerCase())
//                )
//                .toList()){
//            String instanceType = instanceTypes.instanceType().toString();
//            String price = pricingService.getPriceForInstanceType(instanceType, "US East (N. Virginia)");
//            if(price.contains("N/A")) continue;
//            instanceTypesResponse.add(
//                    "Instance Type: " + instanceType+
//                            ", vCPUs: " + instanceTypes.vCpuInfo().defaultVCpus() +
//                            ", Memory: " + instanceTypes.memoryInfo().sizeInMiB() + " MiB" +
//                            ", Price per Hour on Linux: " + price
//            );
//        }
//        return instanceTypesResponse;

        List<String> instanceTypesResponse = new ArrayList<>();
        String nextToken = null;

        do {
            // Request to describe instance types, applying pagination if necessary
            DescribeInstanceTypesRequest.Builder typesRequestBuilder = DescribeInstanceTypesRequest.builder()
                    .maxResults(10);  // Limit the number of results per page (you can adjust this)

            if (nextToken != null) {
                typesRequestBuilder.nextToken(nextToken);  // Add the nextToken for pagination
            }

            DescribeInstanceTypesRequest typesRequest = typesRequestBuilder.build();
            DescribeInstanceTypesResponse response = ec2Client.describeInstanceTypes(typesRequest);

            // Filter instance types by checking if their name contains or equals the instanceTypeName
            for (InstanceTypeInfo instanceTypes : response.instanceTypes()) {
                String instanceType = instanceTypes.instanceType().toString();
                if (instanceType.equalsIgnoreCase(instanceTypeName) || instanceType.toLowerCase().contains(instanceTypeName.toLowerCase())) {
                    String price = pricingService.getPriceForInstanceType(instanceType, "US East (N. Virginia)");

                    // Skip if price contains "N/A"
                    if (price.contains("N/A")) {
                        continue;
                    }

                    // Add instance type information to the list
                    instanceTypesResponse.add(
                            "Instance Type: " + instanceType +
                                    ", vCPUs: " + instanceTypes.vCpuInfo().defaultVCpus() +
                                    ", Memory: " + instanceTypes.memoryInfo().sizeInMiB() + " MiB" +
                                    ", Price per Hour on Linux: " + price
                    );
                }
            }

            // Update nextToken for the next page if there is more data
            nextToken = response.nextToken();

        } while (nextToken != null);  // Continue if there is another page of results

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

    public List<String> getAlarmsForInstance(String instanceId) {
        List<String> alarms = new ArrayList<>();
        String nextToken = null;

        do {
            // Request to describe alarms
            DescribeAlarmsRequest request = DescribeAlarmsRequest.builder()
                    .nextToken(nextToken)
                    .build();

            // Get the response
            DescribeAlarmsResponse response = cloudWatchClient.describeAlarms(request);

            // Loop through alarms and check if they are associated with the specified EC2 instance
            for (MetricAlarm alarm : response.metricAlarms()) {
                boolean matchesInstance = alarm.dimensions().stream()
                        .anyMatch(d -> d.name().equals("InstanceId") && d.value().equals(instanceId));

                if (matchesInstance) {
                    alarms.add(alarm.alarmName());  // Add the alarm name if it matches
                }
            }
            nextToken = response.nextToken();  // Handle pagination if needed
        } while (nextToken != null);  // If the response is paginated

        return alarms;
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
