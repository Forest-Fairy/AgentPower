package org.agentpower.user.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class UserModel {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String uid;
    private String username;
    private String email;
    private String phone;
    private String password;

    private String nickname;
    private String avatarUrl;
    private String sign;

    private String createdTime;
    private String createdBy;
    private String updatedTime;
    private String updatedBy;
    private String deletedTime;
    private String deletedBy;
    private int status;

    public static class Status {
        public static final int DELETED = -1;
        public static final int ERROR = 0;
        public static final int NORMAL = 1;
        public static final int NOT_EDIT = 2;
        public static final int BANNED = 3;
    }
}
