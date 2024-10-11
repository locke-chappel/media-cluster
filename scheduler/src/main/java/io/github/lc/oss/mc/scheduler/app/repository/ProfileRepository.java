package io.github.lc.oss.mc.scheduler.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.lc.oss.mc.scheduler.app.entity.Profile;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, String> {

}
