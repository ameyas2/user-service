package org.user.service;

import com.hazelcast.core.HazelcastInstance;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;

import org.posts.dto.PostDTO;
import org.posts.dto.UserDTO;
import org.posts.mapper.PostMapper;
import org.posts.mapper.UserMapper;
import org.posts.model.Post;
import org.posts.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.user.dao.UserDAO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
public class UserService {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostMapper postMapper;

    private List<String> names;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @PostConstruct
    private void init() throws IOException {
        names = hazelcastInstance.getList("user_names");
        log.info("Loaded names fetched with size : {}", names.size());
        if(names.isEmpty()) {
            log.info("Names is empty");
            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("names.csv");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            names.addAll(bufferedReader.lines().toList());
        }
        log.info("Total usernames loaded : {}", names.size());
    }

    public Collection<UserDTO> getAllUsers() {
        log.info("Get all users");
        return userDTOS(userDAO.getAllUsers());
    }

    public UserDTO getUserById(UUID id) {
        log.info("Get user: {}", id);
        Optional<User> user = userDAO.getUserById(id);
        if(user.isEmpty()) {
            log.info("No user exists for id : {}", id);
            return UserDTO.builder().message("No user exists for id : " + id).build();
        }
        return userMapper.toUserDTO(user.get());
    }

    public UserDTO getPostsByUserId(UUID id) {
        log.info("Get all posts for user id : {}", id);
        Optional<User> user = userDAO.getUserById(id);
        if(user.isEmpty()) {
            log.info("No user exists for id : {}", id);
            return UserDTO.builder().message("No user exists for id : " + id).build();
        }
        Set<Post> posts = user.get().getPosts();
        Set<PostDTO> postDTOs = posts.stream().map(post -> postMapper.toPostDTO(post)).collect(Collectors.toSet());
        UserDTO userDTO = userMapper.toUserDTO(user.get());
        userDTO.setPosts(postDTOs);
        return userDTO;
    }


    public UserDTO addUser(UserDTO userDTO) {
        User user = userMapper.toUser(userDTO);
        userDAO.saveUser(user);
        log.info("Added new user with id: {}", user.getId());
        return userMapper.toUserDTO(user);
    }

    public UserDTO deleteUser(UUID id) {
        log.info("Deleting user with id: {}", id);

        if(userDAO.exists(id)) {
            userDAO.deleteUser(id);
            return UserDTO.builder().message("User deleted with id " + id).build();
        } else {
            return UserDTO.builder().message("User not exists with id " + id).build();
        }
    }

    public UserDTO updateUser(UserDTO userDTO) {
        Optional<User> oldUserEntry = userDAO.getUserById(userDTO.getId());
        if(oldUserEntry.isEmpty()) {
            log.info("No user available for the id : {}", userDTO.getId());
            return null;
        }
        User oldUser = oldUserEntry.get();
        update(oldUser, userDTO);
        userDAO.saveUser(oldUser);
        return userMapper.toUserDTO(oldUser);
    }

    public UserDTO getRandomUser() {
        Random random = new Random();
        int index = (int)random.nextLong(userDAO.size());
        UUID userId = userDAO.getUserId(index);
        log.info("Get random user for id: {}", userId);
        return userMapper.toUserDTO(userDAO.getUserById(userId).get());
    }

    public UserDTO addRandomUser() {
        String name[] = names.stream().skip(names.isEmpty() ? 0 : new Random().nextInt(names.size()))
                .findFirst().get().split(",");
        String firstName = name[0];
        String lastName = name[1];
        String username = firstName.toLowerCase().charAt(0) + lastName.toLowerCase();
        User user = User.of(firstName, lastName, username);
        log.info("Adding new user with id: {}", user.getId());
        userDAO.saveUser(user);
        return userMapper.toUserDTO(user);
    }

    private Collection<UserDTO> userDTOS(Collection<User> users) {
        return users.stream().map(post -> userMapper.toUserDTO(post)).toList();
    }

    private void update(User oldUser, UserDTO newUser) {
        oldUser.setLastname(newUser.getLastname());
        oldUser.setFirstName(newUser.getFirstName());
        //existingUser.setUsername(newUser.getUsername());
    }
}
