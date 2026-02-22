package com.example.home_task_01.controller;

import com.example.home_task_01.Department;
import com.example.home_task_01.repository.DepartmentRepository;
import com.example.home_task_01.repository.StudentRepository;
import com.example.home_task_01.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import org.assertj.core.api.Assertions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired DepartmentRepository deptRepo;
    @Autowired StudentRepository studentRepo;
    @Autowired TeacherRepository teacherRepo;

    private Long deptId;

    @BeforeEach
    void setUp() {
        Department dept = new Department();
        dept.setName("Computer Science");
        deptId = deptRepo.save(dept).getId();
    }

    // ── GET /register ────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /register returns 200 and populates departments model")
    void registerPage_returnsOk() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/register"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("register"))
                .andExpect(MockMvcResultMatchers.model().attributeExists("departments"));
    }

    // ── POST /register ───────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /register with STUDENT role creates student and redirects to login")
    void registerStudent_success() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/register")
                        .param("name", "Alice Smith")
                        .param("email", "alice@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123")
                        .param("role", "STUDENT")
                        .param("departmentId", deptId.toString()))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/login"));

        Assertions.assertThat(studentRepo.findByUsername("alice@example.com")).isPresent();
    }

    @Test
    @DisplayName("POST /register with TEACHER role creates teacher and redirects to login")
    void registerTeacher_success() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/register")
                        .param("name", "Bob Jones")
                        .param("email", "bob@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123")
                        .param("role", "TEACHER")
                        .param("departmentId", deptId.toString()))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/login"));

        Assertions.assertThat(teacherRepo.findByUsername("bob@example.com")).isPresent();
    }

    @Test
    @DisplayName("POST /register with mismatched passwords shows error")
    void registerStudent_passwordMismatch_showsError() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/register")
                        .param("name", "Alice Smith")
                        .param("email", "alice@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "different")
                        .param("role", "STUDENT")
                        .param("departmentId", deptId.toString()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("register"))
                .andExpect(MockMvcResultMatchers.model().attributeExists("error"));

        Assertions.assertThat(studentRepo.findByUsername("alice@example.com")).isEmpty();
    }

    @Test
    @DisplayName("POST /register without selecting a department shows error")
    void registerStudent_noDepartment_showsError() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/register")
                        .param("name", "Alice Smith")
                        .param("email", "alice@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123")
                        .param("role", "STUDENT"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("register"))
                .andExpect(MockMvcResultMatchers.model().attributeExists("error"));
    }

    // ── GET /login ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /login returns 200 and renders login view")
    void loginPage_returnsOk() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/login"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("login"));
    }
}
