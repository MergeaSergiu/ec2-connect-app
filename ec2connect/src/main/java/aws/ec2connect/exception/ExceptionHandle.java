package aws.ec2connect.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;

@ControllerAdvice
public class ExceptionHandle {

    @ExceptionHandler(Ec2Exception.class)
    public ResponseEntity<String> handleEc2Exception(Ec2Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("AWS EC2 Error: " + ex.awsErrorDetails().errorMessage());
    }

}
