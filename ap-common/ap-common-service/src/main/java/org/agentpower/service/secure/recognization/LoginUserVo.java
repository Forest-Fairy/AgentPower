package org.agentpower.service.secure.recognization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUserVo {
    private String id;
    private String username;
    private String email;
    private String phone;
    /** role 的格式  role1;role2; */
    private String role;
}