package com.example.home_task_01.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.home_task_01.Student;
import com.example.home_task_01.Course;
import com.example.home_task_01.Department;
import com.example.home_task_01.repository.StudentRepository;
import com.example.home_task_01.repository.CourseRepository;
import com.example.home_task_01.repository.DepartmentRepository;
import com.example.home_task_01.repository.TeacherRepository;

@Controller
public class TeacherWebController {

    @Autowired private StudentRepository studentRepo;
    @Autowired private CourseRepository courseRepo;
    @Autowired private DepartmentRepository deptRepo;
    @Autowired private TeacherRepository teacherRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    // ── Teacher profile ──────────────────────────────────────────────────────

    @GetMapping("/teacher/profile")
    public String myProfile(Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        var t = teacherRepo.findByUsername(username).orElseThrow();
        model.addAttribute("teacher", t);
        model.addAttribute("departments", deptRepo.findAll());
        return "teacher_profile";
    }

    @PostMapping("/teacher/profile/update")
    public String updateTeacherProfile(@RequestParam Long id,
                                       @RequestParam String fullName,
                                       @RequestParam String username,
                                       @RequestParam(required = false) Long departmentId) {
        var t = teacherRepo.findById(id).orElseThrow();
        t.setFullName(fullName);
        t.setUsername(username);
        if (departmentId != null) deptRepo.findById(departmentId).ifPresent(t::setDepartment);
        teacherRepo.save(t);
        return "redirect:/teacher/profile";
    }

    // ── Student management ────────────────────────────────────────────────────

    @GetMapping("/teacher/student/add")
    public String addStudentForm(Model model) {
        model.addAttribute("departments", deptRepo.findAll());
        return "add_student";
    }

    @PostMapping("/teacher/student/create")
    public String createStudent(@RequestParam String fullName,
                                @RequestParam String username,
                                @RequestParam String password,
                                @RequestParam Long departmentId) {
        Student s = new Student();
        s.setFullName(fullName);
        s.setUsername(username);
        s.setPassword(passwordEncoder.encode(password));
        deptRepo.findById(departmentId).ifPresent(s::setDepartment);
        studentRepo.save(s);
        return "redirect:/view/students";
    }

    @GetMapping("/teacher/student/edit/{id}")
    public String editStudentForm(@PathVariable Long id, Model model) {
        Student s = studentRepo.findById(id).orElseThrow();
        model.addAttribute("student", s);
        model.addAttribute("departments", deptRepo.findAll());
        model.addAttribute("allCourses", courseRepo.findAll());
        return "student_profile";
    }

    @PostMapping("/teacher/student/update")
    public String updateStudent(@RequestParam Long id,
                                @RequestParam String fullName,
                                @RequestParam String username,
                                @RequestParam(required = false) Long departmentId) {
        Student s = studentRepo.findById(id).orElseThrow();
        s.setFullName(fullName);
        s.setUsername(username);
        if (departmentId != null) deptRepo.findById(departmentId).ifPresent(s::setDepartment);
        studentRepo.save(s);
        return "redirect:/view/students";
    }

    @PostMapping("/teacher/student/delete/{id}")
    public String deleteStudent(@PathVariable Long id) {
        studentRepo.deleteById(id);
        return "redirect:/view/students";
    }

    @PostMapping("/teacher/student/{id}/addCourse")
    public String addCourseToStudent(@PathVariable Long id, @RequestParam Long courseId) {
        Student s = studentRepo.findById(id).orElseThrow();
        Course c = courseRepo.findById(courseId).orElseThrow();
        if (s.getCourses() == null) s.setCourses(new java.util.ArrayList<>());
        if (!s.getCourses().contains(c)) s.getCourses().add(c);
        studentRepo.save(s);
        return "redirect:/teacher/student/edit/" + id;
    }

    @PostMapping("/teacher/student/{id}/removeCourse/{courseId}")
    public String removeCourseFromStudent(@PathVariable Long id, @PathVariable Long courseId) {
        Student s = studentRepo.findById(id).orElseThrow();
        if (s.getCourses() != null) {
            s.getCourses().removeIf(x -> x.getId().equals(courseId));
            studentRepo.save(s);
        }
        return "redirect:/teacher/student/edit/" + id;
    }

    // ── Course management ─────────────────────────────────────────────────────

    @GetMapping("/teacher/course/add")
    public String addCourseForm(Model model) {
        model.addAttribute("departments", deptRepo.findAll());
        return "add_course";
    }

    @PostMapping("/teacher/course/create")
    public String createCourse(@RequestParam String name,
                               @RequestParam Long departmentId) {
        Course c = new Course();
        c.setName(name);
        deptRepo.findById(departmentId).ifPresent(c::setDepartment);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        teacherRepo.findByUsername(username).ifPresent(c::setTeacher);
        courseRepo.save(c);
        return "redirect:/view/courses";
    }

    @PostMapping("/teacher/course/delete/{id}")
    public String deleteCourse(@PathVariable Long id) {
        courseRepo.deleteById(id);
        return "redirect:/view/courses";
    }

    // ── Department management ─────────────────────────────────────────────────

    @GetMapping("/teacher/department/add")
    public String addDepartmentForm() {
        return "add_department";
    }

    @PostMapping("/teacher/department/create")
    public String createDepartment(@RequestParam String name) {
        Department d = new Department();
        d.setName(name);
        deptRepo.save(d);
        return "redirect:/view/departments";
    }

    @PostMapping("/teacher/department/delete/{id}")
    public String deleteDepartment(@PathVariable Long id) {
        deptRepo.deleteById(id);
        return "redirect:/view/departments";
    }
}
