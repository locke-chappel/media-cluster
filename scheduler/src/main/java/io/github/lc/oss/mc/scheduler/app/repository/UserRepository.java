package io.github.lc.oss.mc.scheduler.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.lc.oss.mc.scheduler.app.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    User findByUsernameIgnoreCase(String username);

    User findByExternalId(String externalId);
}
