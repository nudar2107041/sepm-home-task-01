package com.example.home_task_01.controller;

import com.example.home_task_01.Department;
import com.example.home_task_01.Student;
import com.example.home_task_01.Teacher;
import com.example.home_task_01.Course;
import com.example.home_task_01.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DashboardControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired StudentRepository studentRepo;
    @Autowired TeacherRepository teacherRepo;
    @Autowired CourseRepository courseRepo;
    @Autowired DepartmentRepository deptRepo;

    @BeforeEach
    void setUp() {
        // Wipe any data seeded by DataInitializer so our counts are predictable
        courseRepo.deleteAll();
        studentRepo.deleteAll();
        teacherRepo.deleteAll();
        deptRepo.deleteAll();

        Department dept = new Department();
        dept.setName("Engineering");
        dept = deptRepo.save(dept);

        Student s = new Student();
        s.setFullName("Alice Student");
        s.setUsername("alice@test.com");
        s.setPassword("pass");
        s.setDepartment(dept);
        studentRepo.save(s);

        Teacher t = new Teacher();
        t.setFullName("Bob Teacher");
        t.setUsername("bob@test.com");
        t.setPassword("pass");
        t.setDepartment(dept);
        teacherRepo.save(t);

        Course c = new Course();
        c.setName("Algorithms");
        c.setDepartment(dept);
        c.setTeacher(t);
        courseRepo.save(c);
    }

    // ── /dashboard ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /dashboard redirects unauthenticated users to login")
    void dashboard_unauthenticated_redirects() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("GET /dashboard returns 200 for authenticated teacher")
    void dashboard_asTeacher_returnsOk() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("GET /dashboard returns 200 for authenticated student")
    void dashboard_asStudent_returnsOk() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    // ── /view/students ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("GET /view/students returns student list in model")
    void viewStudents_returnsStudentList() throws Exception {
        mockMvc.perform(get("/view/students"))
                .andExpect(status().isOk())
                .andExpect(view().name("students"))
                .andExpect(model().attributeExists("students"))
                .andExpect(model().attribute("students", hasSize(1)));
    }

    @Test
    @DisplayName("GET /view/students redirects unauthenticated users")
    void viewStudents_unauthenticated_redirects() throws Exception {
        mockMvc.perform(get("/view/students"))
                .andExpect(status().is3xxRedirection());
    }

    // ── /view/teachers ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("GET /view/teachers returns teacher list in model")
    void viewTeachers_returnsTeacherList() throws Exception {
        mockMvc.perform(get("/view/teachers"))
                .andExpect(status().isOk())
                .andExpect(view().name("teachers"))
                .andExpect(model().attributeExists("teachers"))
                .andExpect(model().attribute("teachers", hasSize(1)));
    }

    // ── /view/courses ────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("GET /view/courses returns course list in model")
    void viewCourses_returnsCourseList() throws Exception {
        mockMvc.perform(get("/view/courses"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses"))
                .andExpect(model().attributeExists("courses"))
                .andExpect(model().attribute("courses", hasSize(1)));
    }

    // ── /view/departments ────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("GET /view/departments returns department list in model")
    void viewDepartments_returnsDepartmentList() throws Exception {
        mockMvc.perform(get("/view/departments"))
                .andExpect(status().isOk())
                .andExpect(view().name("departments"))
                .andExpect(model().attributeExists("departments"))
                .andExpect(model().attribute("departments", hasSize(1)));
    }
}