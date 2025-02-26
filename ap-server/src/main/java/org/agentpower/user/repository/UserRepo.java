package org.agentpower.user.repository;

import org.agentpower.user.model.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepo extends JpaRepository<UserModel, String> {
}
