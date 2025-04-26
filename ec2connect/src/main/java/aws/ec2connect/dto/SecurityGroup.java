package aws.ec2connect.dto;

import lombok.*;

import java.util.List;


public class SecurityGroup {
    private final String groupName;
    private final List<String> inboundRules;
    private final List<String> outboundRules;

    public SecurityGroup(String groupName, List<String> inboundRules, List<String> outboundRules) {
        this.groupName = groupName;
        this.inboundRules = inboundRules;
        this.outboundRules = outboundRules;
    }

    // Getters
    public String getGroupName() {
        return groupName;
    }

    public List<String> getInboundRules() {
        return inboundRules;
    }

    public List<String> getOutboundRules() {
        return outboundRules;
    }

    @Override
    public String toString() {
        return "SecurityGroupInfo{" +
                "groupName='" + groupName + '\'' +
                ", inboundRules=" + inboundRules +
                ", outboundRules=" + outboundRules +
                '}';
    }
}
