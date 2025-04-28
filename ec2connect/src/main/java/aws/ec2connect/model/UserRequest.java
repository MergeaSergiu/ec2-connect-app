package aws.ec2connect.model;


public record UserRequest(
        String email,
        String password
) {
}
