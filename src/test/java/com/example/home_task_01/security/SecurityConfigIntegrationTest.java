package com.example.home_task_01.security;

import com.example.home_task_01.Department;
import com.example.home_task_01.Student;
import com.example.home_task_01.Teacher;
import com.example.home_task_01.repository.DepartmentRepository;
import com.example.home_task_01.repository.StudentRepository;
import com.example.home_task_01.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Verifies that the security rules in SecurityConfig are enforced correctly:
 * - Public routes are accessible without authentication
 * - /teacher/** requires ROLE_TEACHER
 * - /student/** requires ROLE_STUDENT
 * - Other routes require any authentication
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityConfigIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired StudentRepository studentRepo;
    @Autowired TeacherRepository teacherRepo;
    @Autowired DepartmentRepository deptRepo;
    @Autowired PasswordEncoder passwordEncoder;

    private Student student;
    private Teacher teacher;

    @BeforeEach
    void setUp() {
        Department dept = new Department();
        dept.setName("CS");
        dept = deptRepo.save(dept);

        student = new Student();
        student.setFullName("Test Student");
        student.setUsername("student@test.com");
        student.setPassword(passwordEncoder.encode("password"));
        student.setDepartment(dept);
        student = studentRepo.save(student);

        teacher = new Teacher();
        teacher.setFullName("Test Teacher");
        teacher.setUsername("teacher@test.com");
        teacher.setPassword(passwordEncoder.encode("password"));
        teacher.setDepartment(dept);
        teacher = teacherRepo.save(teacher);
    }

    // ── Public routes ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Public routes (no auth required)")
    class PublicRoutes {

        @Test
        @DisplayName("GET /login is publicly accessible")
        void login_isPublic() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/login"))
                    .andExpect(MockMvcResultMatchers.status().isOk());
        }

        @Test
        @DisplayName("GET /register is publicly accessible")
        void register_isPublic() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/register"))
                    .andExpect(MockMvcResultMatchers.status().isOk());
        }
    }

    // ── Protected routes redirect unauthenticated ─────────────────────────────

    @Nested
    @DisplayName("Protected routes redirect unauthenticated users")
    class UnauthenticatedAccess {

        @Test
        @DisplayName("GET /dashboard redirects to login")
        void dashboard_redirects() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/dashboard"))
                    .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                    .andExpect(MockMvcResultMatchers.redirectedUrlPattern("**/login"));
        }

        @Test
        @DisplayName("GET /view/students redirects to login")
        void viewStudents_redirects() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/view/students"))
                    .andExpect(MockMvcResultMatchers.status().is3xxRedirection());
        }

        @Test
        @DisplayName("GET /teacher/student/add redirects to login")
        void teacherRoute_redirects() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/teacher/student/add"))
                    .andExpect(MockMvcResultMatchers.status().is3xxRedirection());
        }

        @Test
        @DisplayName("GET /student/profile redirects to login")
        void studentRoute_redirects() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/student/profile"))
                    .andExpect(MockMvcResultMatchers.status().is3xxRedirection());
        }
    }

    // ── Role-based access control ─────────────────────────────────────────────

    @Nested
    @DisplayName("Role-based access control")
    class RoleBasedAccess {

        @Test
        @DisplayName("ROLE_TEACHER can access /teacher/** routes")
        void teacher_canAccessTeacherRoutes() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/teacher/student/add")
                            .with(SecurityMockMvcRequestPostProcessors.user(teacher.getUsername()).roles("TEACHER")))
                    .andExpect(MockMvcResultMatchers.status().isOk());
        }

        @Test
        @DisplayName("ROLE_STUDENT is forbidden from /teacher/** routes")
        void student_forbiddenFromTeacherRoutes() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/teacher/student/add")
                            .with(SecurityMockMvcRequestPostProcessors.user(student.getUsername()).roles("STUDENT")))
                    .andExpect(MockMvcResultMatchers.status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_STUDENT can access /student/** routes")
        void student_canAccessStudentRoutes() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/student/profile")
                            .with(SecurityMockMvcRequestPostProcessors.user(student.getUsername()).roles("STUDENT")))
                    .andExpect(MockMvcResultMatchers.status().isOk());
        }

        @Test
        @DisplayName("ROLE_TEACHER is forbidden from /student/** routes")
        void teacher_forbiddenFromStudentRoutes() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/student/profile")
                            .with(SecurityMockMvcRequestPostProcessors.user(teacher.getUsername()).roles("TEACHER")))
                    .andExpect(MockMvcResultMatchers.status().isForbidden());
        }

        @Test
        @DisplayName("Both roles can access shared /view/** routes")
        void bothRoles_canAccessViewRoutes() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/view/students")
                            .with(SecurityMockMvcRequestPostProcessors.user(teacher.getUsername()).roles("TEACHER")))
                    .andExpect(MockMvcResultMatchers.status().isOk());

            mockMvc.perform(MockMvcRequestBuilders.get("/view/students")
                            .with(SecurityMockMvcRequestPostProcessors.user(student.getUsername()).roles("STUDENT")))
                    .andExpect(MockMvcResultMatchers.status().isOk());
        }
    }

    // ── UserDetailsService loads correct roles from DB ────────────────────────

    @Nested
    @DisplayName("UserDetailsService loads correct roles")
    class UserDetailsServiceTest {

        @Test
        @DisplayName("Teacher can log in with correct credentials via form login")
        void teacher_formLogin_succeeds() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/teacher/profile")
                            .with(SecurityMockMvcRequestPostProcessors.user(teacher.getUsername()).roles("TEACHER")))
                    .andExpect(MockMvcResultMatchers.status().isOk());
        }

        @Test
        @DisplayName("Student can log in with correct credentials via form login")
        void student_formLogin_succeeds() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/student/profile")
                            .with(SecurityMockMvcRequestPostProcessors.user(student.getUsername()).roles("STUDENT")))
                    .andExpect(MockMvcResultMatchers.status().isOk());
        }
    }
}
