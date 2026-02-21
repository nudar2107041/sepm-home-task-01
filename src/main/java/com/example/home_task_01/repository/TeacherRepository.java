package com.example.home_task_01.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.home_task_01.Teacher;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByUsername(String username);
}
