package aws.ec2connect.controller;

import aws.ec2connect.dto.SecurityGroup;
import aws.ec2connect.service.EC2Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.ec2.model.InstanceTypeInfo;

import java.util.List;

@RestController
@RequestMapping("/ec2")
public class Ec2Controller {

    private final EC2Service ec2Service;

    public Ec2Controller(EC2Service ec2Service) {
        this.ec2Service = ec2Service;
    }

    @GetMapping
    public ResponseEntity<List<String>> listInstances() {
        return ResponseEntity.ok(ec2Service.getInstances());
    }

    @GetMapping("/{instanceId}")
    public ResponseEntity<String> instanceDetails(@PathVariable String instanceId){
        return ResponseEntity.ok(ec2Service.getInstanceDetails(instanceId));
    }

    @PostMapping("/stop")
    public ResponseEntity<String> stopInstances(@RequestParam String instanceId) {
        return ResponseEntity.ok(ec2Service.stopInstance(instanceId));
    }

    @PostMapping("/start")
    public ResponseEntity<String> startInstances(@RequestParam String instanceId) {
        return ResponseEntity.ok(ec2Service.startInstance(instanceId));
    }

    @GetMapping("/sc/{groupId}")
    public ResponseEntity<List<SecurityGroup>> getSecurityGroupDetails(@PathVariable String groupId) {
        List<SecurityGroup> securityGroups = ec2Service.getSecurityGroupRules(groupId);
        return ResponseEntity.ok(securityGroups);
    }

    @GetMapping("/sc")
    public ResponseEntity<List<String>> getSecurityGroups() {
        return ResponseEntity.ok(ec2Service.getAllSecurityGroups());
    }

    @PostMapping("/sc")
    public ResponseEntity<String> addSecurityGroup(@RequestParam String groupName,
                                                   @RequestParam String description,
                                                   @RequestParam String vpcId,
                                                   @RequestParam String myIpAddress) {
        return ResponseEntity.ok(ec2Service.createSecurityGroup(groupName,description,vpcId, myIpAddress));
    }

    @GetMapping("/types")
    public ResponseEntity<List<String>> getInstanceTypes() {
        return ResponseEntity.ok(ec2Service.getInstanceTypes());
    }

    @GetMapping("/imagesRH")
    public ResponseEntity<List<String>> getImagesForRedHat() {
        return ResponseEntity.ok(ec2Service.getImagesForRedHat());
    }

//    @GetMapping("/imagesWindows")
//    public ResponseEntity<List<String>> getImagesForWindows() {
//        return ResponseEntity.ok(ec2Service.getImagesForWindows());
//    }



}
