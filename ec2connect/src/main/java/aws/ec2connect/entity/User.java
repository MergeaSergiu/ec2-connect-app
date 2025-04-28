package aws.ec2connect.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;


@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "_user")
public class User {

    @Id
    @GeneratedValue
    private Integer userId;

    @Email
    private String email;

    @Size(min = 6)
    private String password;

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public Integer getUserId() {
        return userId;
    }

    public @Email String getEmail() {
        return email;
    }

    public @Size(min = 6) String getPassword() {
        return password;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public void setEmail(@Email String email) {
        this.email = email;
    }

    public void setPassword(@Size(min = 6) String password) {
        this.password = password;
    }
}
