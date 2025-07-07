package org.user.dao;

import com.hazelcast.core.HazelcastInstance;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.posts.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;

@Repository
@Log4j2
public class UserDAO {

    @Autowired
    private UserRepository userRepository;

    private List<UUID> userIDs;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @PostConstruct
    public void init() {
        userIDs = hazelcastInstance.getList("user_ids");
        if(userIDs.isEmpty()) {
            List<UUID> ids = userRepository.getAllIds();
            userIDs.addAll(ids);
        }
        log.info("Total user ids loaded : {}", userIDs.size());
    }

    public Collection<User> getAllUsers() {
        log.debug("Getting all users");
        return userRepository.findAll();
    }

    public Optional<User> getUserById(UUID id) {
        log.debug("Get user: {}", id);
        return userRepository.findById(id);
    }

    public User saveUser(User user) {
        userRepository.save(user);
        log.debug("Adding  user with id: {}", user.getId());
        userIDs.add(user.getId());
        return user;
    }

    public void deleteUser(UUID id) {
        userRepository.deleteById(id);
        deleteUserId(id);
    }

    public boolean exists(UUID id) {
        return userRepository.existsById(id);
    }

    public long size() {
        return userRepository.count();
    }

    public UUID getUserId(int index) {
        return userIDs.get(index);
    }

    public void addUserId(UUID id) {
        userIDs.add(id);
    }

    public boolean deleteUserId(UUID id) {
        return userIDs.remove(id);
    }
}
