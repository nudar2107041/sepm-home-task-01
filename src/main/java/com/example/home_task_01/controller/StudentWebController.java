package com.example.home_task_01.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.home_task_01.Student;
import com.example.home_task_01.Course;
import com.example.home_task_01.repository.StudentRepository;
import com.example.home_task_01.repository.CourseRepository;

@Controller
public class StudentWebController {

    @Autowired private StudentRepository studentRepo;
    @Autowired private CourseRepository courseRepo;

    /** Student views their own profile — resolved from the security context */
    @GetMapping("/student/profile")
    public String viewMyProfile(Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Student s = studentRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Student not found: " + username));
        model.addAttribute("student", s);
        model.addAttribute("allCourses", courseRepo.findAll());
        return "student_view";
    }

    /** Teacher or the student themselves can view by ID */
    @GetMapping("/student/profile/view/{id}")
    public String viewProfile(@PathVariable Long id, Model model) {
        Student s = studentRepo.findById(id).orElseThrow();
        model.addAttribute("student", s);
        model.addAttribute("allCourses", courseRepo.findAll());
        return "student_view";
    }

    @PostMapping("/student/enroll")
    public String enroll(@RequestParam Long studentId, @RequestParam Long courseId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Student s = studentRepo.findById(studentId).orElseThrow();

        boolean isTeacher = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));
        if (!s.getUsername().equals(username) && !isTeacher) {
            return "redirect:/login";
        }

        Course c = courseRepo.findById(courseId).orElseThrow();
        if (s.getCourses() == null) s.setCourses(new java.util.ArrayList<>());
        if (!s.getCourses().contains(c)) s.getCourses().add(c);
        studentRepo.save(s);
        return "redirect:/student/profile/view/" + studentId;
    }
}
