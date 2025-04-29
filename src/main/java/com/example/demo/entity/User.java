package com.example.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

/**
 * 用户实体类
 */
@Data
@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 用户名
     */
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    
    /**
     * 邮箱
     */
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    /**
     * 姓名
     */
    @Column(length = 100)
    private String name;
}
