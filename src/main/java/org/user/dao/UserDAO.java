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

    private Map<UUID, User> users;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @PostConstruct
    public void init() {
        try {
            users = hazelcastInstance.getMap("users");
            log.info("Total users loaded : {}", users.size());
        } catch (Exception e) {
            log.error("Exception : {}", e.getMessage(), e);
            e.printStackTrace();
        }
    }

    public Collection<User> getAllUsers() {
        log.debug("Getting all users");
        return userRepository.findAll();
    }

    public Optional<User> getUserById(UUID id) {
        log.debug("Get user: {}", id);
        return userRepository.findById(id);
    }

    public Optional<User> getAnyUser() {
        return users.values().stream().findAny();
    }

    public User saveUser(User user) {
        log.info("Adding  user to db : {}", user);
        userRepository.save(user);
        log.info("Adding user to hazelcast with id: {}", user.getId());
        users.put(user.getId(), user);
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

    public int updateImageLocation(String location, UUID id) {
        return userRepository.updateImageLocation(location, id);
    }

    public User deleteUserId(UUID id) {
        return users.remove(id);
    }
}
