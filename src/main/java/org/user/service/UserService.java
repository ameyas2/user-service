package org.user.service;

import com.hazelcast.core.HazelcastInstance;
import io.minio.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.instancio.Instancio;
import org.instancio.Select;
import org.posts.dto.PostDTO;
import org.posts.dto.UserDTO;
import org.posts.mapper.PostMapper;
import org.posts.mapper.UserMapper;
import org.posts.model.AbstractEntity;
import org.posts.model.Post;
import org.posts.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
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

    @Autowired
    private MinioClient minioClient;

    private final String BUCKET_NAME = "users";

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

    /**
     * 1. Get user by id
     * 2. Get the location and split it by '/' to get the filename
     * 3. Get the file from minio
     * 4. send it as response
     */
    public ResponseEntity<InputStreamResource> getProfileImage(UUID id) {
        try {
            log.info("Getting profile image for the id : {}", id);
            User user = userDAO.getUserById(id).orElse(null);
            String location = user.getProfileImageLocation();
            String filename = location.split("/")[1];
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(location)
                            .build());
            InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(location)
                            .build()
            );

            String contentDisposition = "attachment; filename=\"" + filename + "\"";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .contentType(MediaType.parseMediaType(stat.contentType()))
                    .contentLength(stat.size())
                    .body(new InputStreamResource(inputStream));
        } catch (Exception e) {
            log.error("Exception : {}", e.getMessage());
            log.error("Stack Trace : {}", ExceptionUtils.getStackTrace(e));
            return ResponseEntity.badRequest().body(null);
        }
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

    /**
     * 1. Store user -> get the id
     * 2. use the id as folder -> store the file
     * 3. update the location in the user table
    * */
    public UserDTO addUser(UserDTO userDTO, MultipartFile file) {
        try {
            User user = userMapper.toUser(userDTO);
            userDAO.saveUser(user);
            String filename = file.getOriginalFilename();
            String location = user.getId() + "/" + filename;
            PutObjectArgs putObjectArgs = PutObjectArgs
                    .builder()
                    .bucket(BUCKET_NAME)
                    .object(location)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();
            minioClient.putObject(putObjectArgs);
            int num = userDAO.updateImageLocation(location, user.getId());
            return userMapper.toUserDTO(user);
        } catch (Exception e) {
            log.error("Exception : {}", e.getMessage(), e);
            return UserDTO.builder().message("User creation failed").error(e.getMessage()).build();
        }
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
        Optional<User> userOptional = userDAO.getAnyUser();
        log.info("Get random user");
        return userMapper.toUserDTO(userOptional.orElse(null));
    }

    public UserDTO addRandomUser() {
        User user = Instancio.of(User.class)
                .ignore(Select.field("posts"))
                .ignore(Select.field(AbstractEntity.class, "id"))
                .ignore(Select.field(AbstractEntity.class, "createdAt"))
                .ignore(Select.field(AbstractEntity.class, "updatedAt"))
                .create();
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
