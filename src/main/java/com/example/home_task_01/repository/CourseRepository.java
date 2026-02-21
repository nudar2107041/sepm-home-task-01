package com.example.home_task_01.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.home_task_01.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {

}
