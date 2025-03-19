package org.agentpower.user.repository;

import org.agentpower.user.model.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepo extends JpaRepository<UserModel, String> {
    UserModel findByUidAndPassword(String uid, String password);

    UserModel findByPhoneAndPassword(String infoToken, String password);

    UserModel findByEmailAndPassword(String infoToken, String password);

    UserModel findByUsernameAndPassword(String infoToken, String password);

    UserModel findByEmailOrPhone(String email, String phone);
}
