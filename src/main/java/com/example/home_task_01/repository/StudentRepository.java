package com.example.home_task_01.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.home_task_01.Student;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUsername(String username);
}
