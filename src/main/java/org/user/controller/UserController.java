package org.user.controller;

import lombok.extern.log4j.Log4j2;
import org.posts.dto.UserDTO;
import org.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Log4j2
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public ResponseEntity<Collection<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/{id}/posts")
    public ResponseEntity<UserDTO> getPostsByUserId(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(userService.getPostsByUserId(id));
    }

    @PostMapping("/")
    public ResponseEntity<UserDTO> addUser(@RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.addUser(userDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<UserDTO> deleteUser(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(userService.deleteUser(id));
    }

    @PutMapping("/")
    public ResponseEntity<UserDTO> updateUser(@RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.updateUser(userDTO));
    }

    @GetMapping("/load")
    public ResponseEntity<UserDTO> getRandomUser() {
        return ResponseEntity.ok(userService.getRandomUser());
    }

    @PostMapping("/load")
    public ResponseEntity<UserDTO> addRandomUser() {
        return ResponseEntity.ok(userService.addRandomUser());
    }
}
