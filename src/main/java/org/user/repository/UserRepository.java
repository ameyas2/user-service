package org.user.repository;

import jakarta.transaction.Transactional;
import org.posts.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Modifying
    @Transactional
    @Query("update User u set profileImageLocation = :location where id = :id")
    int updateImageLocation(@Param("location") String location, @Param("id") UUID id);
}
