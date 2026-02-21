package com.example.home_task_01.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.example.home_task_01.Student;
import com.example.home_task_01.Teacher;
import com.example.home_task_01.repository.TeacherRepository;
import com.example.home_task_01.repository.StudentRepository;

@RestController
@RequestMapping("/teacher")
public class TeacherController {

    @Autowired
    private TeacherRepository teacherRepo;
    @Autowired
    private StudentRepository studentRepo;

    @GetMapping("/profile/{id}")
    public Teacher getProfile(@PathVariable Long id) {
        return teacherRepo.findById(id).orElseThrow();
    }

    @PutMapping("/profile/{id}")
    public Teacher updateProfile(@PathVariable Long id, @RequestBody Teacher updated) {
        Teacher teacher = teacherRepo.findById(id).orElseThrow();
        teacher.setFullName(updated.getFullName());
        return teacherRepo.save(teacher);
    }

    // Teacher can modify student info
    @PutMapping("/student/{id}")
    public Student updateStudent(@PathVariable Long id, @RequestBody Student updated) {
        Student student = studentRepo.findById(id).orElseThrow();
        student.setFullName(updated.getFullName());
        return studentRepo.save(student);
    }
}

