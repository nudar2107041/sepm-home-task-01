package com.example.home_task_01.config;

import java.util.Arrays;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.home_task_01.Department;
import com.example.home_task_01.repository.DepartmentRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private DepartmentRepository deptRepo;

    @Override
    public void run(String... args) throws Exception {
        if (deptRepo.count() == 0) {
            Arrays.asList("CSE", "EEE", "ME", "CE", "URP").forEach(name -> {
                Department d = new Department();
                d.setName(name);
                deptRepo.save(d);
            });
        }
    }
}
