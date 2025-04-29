package com.example.demo.service;

import com.example.demo.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 根据ID查找用户
     * @param id 用户ID
     * @return 用户对象
     */
    Optional<User> findById(Long id);
    
    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户对象
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 获取所有用户
     * @return 用户列表
     */
    List<User> findAll();
    
    /**
     * 保存用户
     * @param user 用户对象
     * @return 保存后的用户对象
     */
    User save(User user);
    
    /**
     * 更新用户
     * @param user 用户对象
     * @return 更新后的用户对象
     */
    User update(User user);
    
    /**
     * 删除用户
     * @param id 用户ID
     */
    void deleteById(Long id);
}
