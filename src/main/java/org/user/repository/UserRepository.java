package org.user.repository;

import org.posts.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("select u.id from #{#entityName} u")
    List<UUID> getAllIds();
}
