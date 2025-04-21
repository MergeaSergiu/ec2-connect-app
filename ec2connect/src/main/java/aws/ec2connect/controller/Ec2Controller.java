package aws.ec2connect.controller;

import aws.ec2connect.service.EC2Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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


}
