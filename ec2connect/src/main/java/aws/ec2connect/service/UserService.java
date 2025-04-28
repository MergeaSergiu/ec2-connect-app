package aws.ec2connect.service;

import aws.ec2connect.entity.User;
import aws.ec2connect.model.UserRequest;
import aws.ec2connect.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String createUser(UserRequest userRequest) {
        if(userRequest.email() == null || userRequest.email().isEmpty()) throw new IllegalArgumentException("Email is required");
        if(userRequest.password() == null || userRequest.password().isEmpty()) throw new IllegalArgumentException("Password is required");

        if(userRequest.password().length() < 6) throw new IllegalArgumentException("Password must be at least 6 characters");

        User user = new User(userRequest.email(), userRequest.password());
//                .builder()
//                .email(userRequest.email())
//                .password(userRequest.password())
//                .build();

        userRepository.save(user);
        return "User " + user.getEmail() + " created";
    }
}
