package org.agentpower.user.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class UserModel {
    @Id
    private String id;
    private String username;
    private String email;
    private String phone;
    private String password;

    private String createdTime;
    private String createdBy;
    private String updatedTime;
    private String updatedBy;
    private String deletedTime;
    private String deletedBy;
    private int status;
}
