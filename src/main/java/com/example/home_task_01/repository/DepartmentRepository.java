package com.example.home_task_01.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.home_task_01.Department;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

}
