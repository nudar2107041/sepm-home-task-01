package com.example.home_task_01.controller;

import com.example.home_task_01.Course;
import com.example.home_task_01.Department;
import com.example.home_task_01.Student;
import com.example.home_task_01.Teacher;
import com.example.home_task_01.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StudentWebControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired StudentRepository studentRepo;
    @Autowired TeacherRepository teacherRepo;
    @Autowired CourseRepository courseRepo;
    @Autowired DepartmentRepository deptRepo;
    @Autowired PasswordEncoder passwordEncoder;

    private Student student;
    private Course course;
    private Department dept;

    @BeforeEach
    void setUp() {
        dept = new Department();
        dept.setName("Engineering");
        dept = deptRepo.save(dept);

        Teacher teacher = new Teacher();
        teacher.setFullName("Prof. Test");
        teacher.setUsername("prof@uni.com");
        teacher.setPassword(passwordEncoder.encode("pass"));
        teacherRepo.save(teacher);

        student = new Student();
        student.setFullName("Alice Student");
        student.setUsername("alice@uni.com");
        student.setPassword(passwordEncoder.encode("pass"));
        student.setDepartment(dept);
        student = studentRepo.save(student);

        course = new Course();
        course.setName("Algorithms");
        course.setDepartment(dept);
        course = courseRepo.save(course);
    }

    // ── GET /student/profile ─────────────────────────────────────────────────

    @Test
    @DisplayName("GET /student/profile redirects unauthenticated user to login")
    void studentProfile_unauthenticated_redirects() throws Exception {
        mockMvc.perform(get("/student/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("GET /student/profile returns own profile for logged-in student")
    void studentProfile_authenticated_returnsOwnProfile() throws Exception {
        mockMvc.perform(get("/student/profile")
                        .with(user(student.getUsername()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("student_view"))
                .andExpect(model().attributeExists("student"))
                .andExpect(model().attributeExists("allCourses"));
    }

    @Test
    @DisplayName("Teacher cannot access /student/profile (403) — it is a STUDENT-only route")
    void studentProfile_asTeacher_isForbidden() throws Exception {
        mockMvc.perform(get("/student/profile")
                        .with(user("prof@uni.com").roles("TEACHER")))
                .andExpect(status().isForbidden());
    }

    // ── GET /student/profile/view/{id} ───────────────────────────────────────

    @Test
    @DisplayName("GET /student/profile/view/{id} allows a student to view their own profile by ID")
    void viewProfileById_ownProfile_returnsOk() throws Exception {
        mockMvc.perform(get("/student/profile/view/" + student.getId())
                        .with(user(student.getUsername()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("student_view"))
                .andExpect(model().attributeExists("student"));
    }

    // ── POST /student/enroll ─────────────────────────────────────────────────

    @Test
    @DisplayName("POST /student/enroll enrolls student in a course")
    void enroll_addsCourseToCourseList() throws Exception {
        mockMvc.perform(post("/student/enroll")
                        .with(user(student.getUsername()).roles("STUDENT"))
                        .param("studentId", student.getId().toString())
                        .param("courseId", course.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/profile/view/" + student.getId()));

        Student updated = studentRepo.findById(student.getId()).orElseThrow();
        assertThat(updated.getCourses()).extracting("id").contains(course.getId());
    }

    @Test
    @DisplayName("POST /student/enroll does not double-enroll the same course")
    void enroll_noDuplicate() throws Exception {
        // Enroll twice
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/student/enroll")
                    .with(user(student.getUsername()).roles("STUDENT"))
                    .param("studentId", student.getId().toString())
                    .param("courseId", course.getId().toString()));
        }

        Student updated = studentRepo.findById(student.getId()).orElseThrow();
        long count = updated.getCourses().stream()
                .filter(c -> c.getId().equals(course.getId())).count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /student/enroll blocks a different student from enrolling on behalf of another")
    void enroll_differentStudent_redirectsToLogin() throws Exception {
        Student other = new Student();
        other.setFullName("Eve Hacker");
        other.setUsername("eve@uni.com");
        other.setPassword(passwordEncoder.encode("pass"));
        other = studentRepo.save(other);

        mockMvc.perform(post("/student/enroll")
                        .with(user("eve@uni.com").roles("STUDENT"))
                        .param("studentId", student.getId().toString()) // alice's ID
                        .param("courseId", course.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        // Alice should still not be enrolled
        Student alice = studentRepo.findById(student.getId()).orElseThrow();
        assertThat(alice.getCourses() == null || alice.getCourses().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("POST /student/enroll blocks teachers (ROLE_TEACHER cannot access /student/** routes)")
    void enroll_asTeacher_blocked() throws Exception {
        mockMvc.perform(post("/student/enroll")
                        .with(user("prof@uni.com").roles("TEACHER"))
                        .param("studentId", student.getId().toString())
                        .param("courseId", course.getId().toString()))
                .andExpect(status().isForbidden());

        // Student should NOT be enrolled since teacher was blocked
        Student notEnrolled = studentRepo.findById(student.getId()).orElseThrow();
        assertThat(notEnrolled.getCourses() == null || notEnrolled.getCourses().isEmpty()).isTrue();
    }
}