package com.example.demo.service.impl;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    
    @Autowired
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * 根据ID查找用户 - 读操作，使用从库
     */
    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    /**
     * 根据用户名查找用户 - 读操作，使用从库
     */
    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * 获取所有用户 - 读操作，使用从库
     */
    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }
    
    /**
     * 保存用户 - 写操作，使用主库
     */
    @Override
    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }
    
    /**
     * 更新用户 - 写操作，使用主库
     */
    @Override
    @Transactional
    public User update(User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("User ID cannot be null for update operation");
        }
        return userRepository.save(user);
    }
    
    /**
     * 删除用户 - 写操作，使用主库
     */
    @Override
    @Transactional
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }
}
