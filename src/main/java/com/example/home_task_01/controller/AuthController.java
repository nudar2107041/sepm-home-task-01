package com.example.home_task_01.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.home_task_01.Student;
import com.example.home_task_01.Teacher;
import com.example.home_task_01.repository.StudentRepository;
import com.example.home_task_01.repository.TeacherRepository;
import com.example.home_task_01.repository.DepartmentRepository;

@Controller
public class AuthController {

    @Autowired
    private StudentRepository studentRepo;

    @Autowired
    private TeacherRepository teacherRepo;

    @Autowired
    private DepartmentRepository deptRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("departments", deptRepo.findAll());
        return "register";
    }

    @PostMapping("/register")
    public String registerSubmit(@RequestParam String name,
                                 @RequestParam String email,
                                 @RequestParam String password,
                                 @RequestParam String confirmPassword,
                                 @RequestParam String role,
                                 @RequestParam(required = false) Long departmentId,
                                 Model model) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match");
            model.addAttribute("departments", deptRepo.findAll());
            return "register";
        }

        if (departmentId == null) {
            model.addAttribute("error", "Please select a department");
            model.addAttribute("departments", deptRepo.findAll());
            return "register";
        }

        String encoded = passwordEncoder.encode(password);

        if ("TEACHER".equalsIgnoreCase(role)) {
            Teacher t = new Teacher();
            t.setFullName(name);
            t.setUsername(email);
            t.setPassword(encoded);
            if (departmentId != null) {
                deptRepo.findById(departmentId).ifPresent(t::setDepartment);
            }
            teacherRepo.save(t);
        } else {
            Student s = new Student();
            s.setFullName(name);
            s.setUsername(email);
            s.setPassword(encoded);
            if (departmentId != null) {
                deptRepo.findById(departmentId).ifPresent(s::setDepartment);
            }
            studentRepo.save(s);
        }

        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
