package com.example.home_task_01.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.home_task_01.repository.StudentRepository;
import com.example.home_task_01.repository.TeacherRepository;
import com.example.home_task_01.repository.CourseRepository;
import com.example.home_task_01.repository.DepartmentRepository;

@Controller
public class DashboardController {

    @Autowired
    private StudentRepository studentRepo;
    @Autowired
    private TeacherRepository teacherRepo;
    @Autowired
    private CourseRepository courseRepo;
    @Autowired
    private DepartmentRepository deptRepo;

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/view/students")
    public String viewStudents(Model model) {
        model.addAttribute("students", studentRepo.findAll());
        return "students";
    }

    @GetMapping("/view/teachers")
    public String viewTeachers(Model model) {
        model.addAttribute("teachers", teacherRepo.findAll());
        return "teachers";
    }

    @GetMapping("/view/courses")
    public String viewCourses(Model model) {
        model.addAttribute("courses", courseRepo.findAll());
        return "courses";
    }

    @GetMapping("/view/departments")
    public String viewDepartments(Model model) {
        model.addAttribute("departments", deptRepo.findAll());
        return "departments";
    }
}
