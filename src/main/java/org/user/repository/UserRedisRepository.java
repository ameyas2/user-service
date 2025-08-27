package org.user.repository;

import org.posts.model.UserRedis;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface UserRedisRepository extends CrudRepository<UserRedis, UUID> {
}
