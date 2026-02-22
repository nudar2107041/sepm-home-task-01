package com.example.home_task_01.controller;

import com.example.home_task_01.Course;
import com.example.home_task_01.Department;
import com.example.home_task_01.Student;
import com.example.home_task_01.Teacher;
import com.example.home_task_01.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.transaction.annotation.Transactional;
import org.assertj.core.api.Assertions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TeacherWebControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired StudentRepository studentRepo;
    @Autowired TeacherRepository teacherRepo;
    @Autowired CourseRepository courseRepo;
    @Autowired DepartmentRepository deptRepo;
    @Autowired PasswordEncoder passwordEncoder;

    private Department dept;
    private Teacher teacher;
    private Student student;
    private Course course;

    @BeforeEach
    void setUp() {
        dept = new Department();
        dept.setName("Computer Science");
        dept = deptRepo.save(dept);

        teacher = new Teacher();
        teacher.setFullName("Dr. Smith");
        teacher.setUsername("smith@uni.com");
        teacher.setPassword(passwordEncoder.encode("pass"));
        teacher.setDepartment(dept);
        teacher = teacherRepo.save(teacher);

        student = new Student();
        student.setFullName("Alice Student");
        student.setUsername("alice@uni.com");
        student.setPassword(passwordEncoder.encode("pass"));
        student.setDepartment(dept);
        student = studentRepo.save(student);

        course = new Course();
        course.setName("Data Structures");
        course.setDepartment(dept);
        course.setTeacher(teacher);
        course = courseRepo.save(course);
    }

    // ── Security: non-teachers are blocked ───────────────────────────────────

    @Test
    @DisplayName("Student cannot access teacher-only routes (403)")
    @WithMockUser(roles = "STUDENT")
    void studentCannotAccessTeacherRoutes() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/teacher/student/add"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated user is redirected to login from teacher routes")
    void unauthenticated_redirectedToLogin() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/teacher/student/add"))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrlPattern("**/login"));
    }

    // ── Teacher profile ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Teacher profile")
    class TeacherProfile {

        @Test
        @DisplayName("GET /teacher/profile loads own profile from security context")
        void getProfile_loadsCorrectTeacher() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/teacher/profile")
                            .with(SecurityMockMvcRequestPostProcessors.user(teacher.getUsername()).roles("TEACHER")))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.view().name("teacher_profile"))
                    .andExpect(MockMvcResultMatchers.model().attributeExists("teacher"))
                    .andExpect(MockMvcResultMatchers.model().attributeExists("departments"));
        }

        @Test
        @DisplayName("POST /teacher/profile/update saves new name and username")
        void updateProfile_savesChanges() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post("/teacher/profile/update")
                            .with(SecurityMockMvcRequestPostProcessors.user(teacher.getUsername()).roles("TEACHER"))
                            .param("id", teacher.getId().toString())
                            .param("fullName", "Dr. John Smith")
                            .param("username", "smith@uni.com")
                            .param("departmentId", dept.getId().toString()))
                    .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                    .andExpect(MockMvcResultMatchers.redirectedUrl("/teacher/profile"));

            Teacher updated = teacherRepo.findById(teacher.getId()).orElseThrow();
            Assertions.assertThat(updated.getFullName()).isEqualTo("Dr. John Smith");
        }
    }

    // ── Student management ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Student management")
    class StudentManagement {

        @Test
        @DisplayName("GET /teacher/student/add renders form with departments")
        void addStudentForm_rendersWithDepartments() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/teacher/student/add")
                            .with(SecurityMockMvcRequestPostProcessors.user(teacher.getUsername()).roles("TEACHER")))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.view().name("add_student"))
                    .andExpect(MockMvcResultMatchers.model().attributeExists("departments"));
        }

        @Test
        @DisplayName("POST /teacher/student/create creates student and redirects")
        void createStudent_persistsAndRedirects() throws Exception {
            long countBefore = studentRepo.count();

            mockMvc.perform(MockMvcRequestBuilders.post("/teacher/student/create")
                            .with(SecurityMockMvcRequestPostProcessors.user(teacher.getUsername()).roles("TEACHER"))
                            .param("fullName", "Bob New")
                            .param("username", "bob@uni.com")
                            .param("password", "password123")
                            .param("departmentId", dept.getId().toString()))
                    .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                    .andExpect(MockMvcResultMatchers.redirectedUrl("/view/students"));

            Assertions.assertThat(studentRepo.count()).isEqualTo(countBefore + 1);
            Assertions.assertThat(studentRepo.findByUsername("bob@uni.com")).isPresent();
        }

        @Test
        @DisplayName("POST /teacher/student/create encodes the password (not stored in plain text)")
        void createStudent_passwordIsEncoded() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post("/teacher/student/create")
                            .with(SecurityMockMvcRequestPostProcessors.user(teacher.getUsername()).roles("TEACHER"))
                            .param("fullName", "Carol")
                            .param("username", "carol@uni.com")
                            .param("password", "plaintext")
                            .param("departmentId", dept.getId().toString()))
                    .andExpect(MockMvcResultMatchers.status().is3xxRedirection());

            Student saved = studentRepo.findByUsername("carol@uni.com").orElseThrow();
            Assertions.assertThat(saved.getPassword()).isNotEqualTo("plaintext");
            Assertions.assertThat(passwordEncoder.matches("plaintext", saved.getPassword())).isTrue();
        }

        @Test
        @DisplayName("GET /teacher/student/edit/{id} renders student edit form")
        void editStudentForm_rendersForm() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/teacher/student/edit/" + student.getId())
                            .with(SecurityMockMvcRequestPostProcessors.user(teacher.getUsername()).roles("TEACHER")))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.view().name("student_profile"))
                    .andExpect(MockMvcResultMatchers.model().attributeExists("student"))
                    .andExpect(MockMvcResultMatchers.model().attributeExists("departments"))
                    .andExpect(MockMvcResultMatchers.model().attributeExists("allCourses"));
        }

        @Test
        @DisplayName("POST /teacher/student/update saves new name and redirects")
        void updateStudent_savesChanges() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post("/teacher/student/update")
                            .with(SecurityMockMvcRequestPostProcessors.user(teacher.getUsername()).roles("TEACHER"))
                            .param("id", student.getId().toString())
                            .param("fullName", "Alice Updated")
                            .param("username", "alice@uni.com")
                            .param("departmentId", dept.getId().toString()))
                    .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                    .andExpect(MockMvcResultMatchers.redirectedUrl("/view/students"));

            Student updated = studentRepo.findById(student.getId()).orElseThrow();
            Assertions.assertThat(updated.getFullName()).isEqualTo("Alice Updated");
        }

        @Test
        @DisplayName("POST /teacher/student/delete/{id} removes student from DB")
        void deleteStudent_removesFromDb() throws Exception {
            Long id = student.getId();

            mockMvc.perform(MockMvcRequestBuilders.post("/teacher/student/delete/" + id)
                            .with(SecurityMockMvcRequestPostProcessors.user(teacher.getUsername()).roles("TEACHER")))
                    .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                    .andExpect(MockMvcResultMatchers.redirectedUrl("/view/students"));

            Assertions.assertThat(studentRepo.findById(id)).isEmpty();
        }

        @Test
        @DisplayName("POST /teacher/student/{id}/addCourse enrolls student in course")
        void addCourseToStudent_enrollsStudent() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post("/teacher/student/" + student.getId() + "/addCourse")
                            .with(SecurityMockMvcRequestPostProcessors.user(teacher.getUsername()).roles("TEACHER"))
                            .param("courseId", course.getId().toString()))
                    .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                    .andExpect(MockMvcResultMatchers.redirectedUrl("/teacher/student/edit/" + student.getId()));

            Student updated = studentRepo.findById(student.getId()).orElseThrow();
            Assertions.assertThat(updated.getCourses()).extracting("id").contains(course.getId());
        }

        @Test
        @DisplayName("POST /teacher/student/{id}/addCourse does not double-enroll")
        void addCourseToStudent_noDuplicate() throws Exception {
            // Enroll once
            mockMvc.perform(MockMvcRequestBuilders.post("/teacher/student/" + student.getId() + "/addCourse")
                    .with(SecurityMockMvcRequestPostProcessors.user(teacher.getUsername()).roles("TEACHER"))
                    .param("courseId", course.getId().toString()));

            // Enroll again
            mockMvc.perform(MockMvcRequestBuilders.post("/teacher/student/" + student.getId() + "/addCourse")
                    .with(SecurityMockMvcRequestPostProcessors.user(teacher.getUsername()).roles("TEACHER"))
                    .param("courseId", course.getId().toString()));

            Student updated = studentRepo.findById(student.getId()).orElseThrow();
            long occurrences = updated.getCourses().stream()
                    .filter(c -> c.getId().equals(course.getId())).count();
            Assertions.assertThat(occurrences).isEqualTo(1);
        }

        @Test
        @DisplayName("POST /teacher/student/{id}/removeCourse/{courseId} un-enrolls student")
        void removeCourseFromStudent_unenrollsStudent() throws Exception {
            // First enroll
            mockMvc.perform(MockMvcRequestBuilders.post("/teacher/student/" + student.getId() + "/addCourse")
                    .with(SecurityMockMvcRequestPostProcessors.user(teacher.getUsername()).roles("TEACHER"))
                    .param("courseId", course.getId().toString()));

            // Then remove
            mockMvc.perform(MockMvcRequestBuilders.post("/teacher/student/" + student.getId()
                            + "/removeCourse/" + course.getId())
                            .with(SecurityMockMvcRequestPostProcessors.user(teacher.getUsername()).roles("TEACHER")))
                    .andExpect(MockMvcResultMatchers.status().is3xxRedirection());

            Student updated = studentRepo.findById(student.getId()).orElseThrow();
            Assertions.assertThat(updated.getCourses()).extracting("id").doesNotContain(course.getId());
        }
    }

    // ── Course management ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Course management")
    class CourseManagement {

        @Test
        @DisplayName("GET /teacher/course/add renders form with departments")
        void addCourseForm_rendersWithDepartments() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/teacher/course/add")
                            .with(SecurityMockMvcRequestPostProcessors.user(teacher.getUsername()).roles("TEACHER")))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.view().name("add_course"))
                    .andExpect(MockMvcResultMatchers.model().attributeExists("departments"));
        }

        @Test
        @DisplayName("POST /teacher/course/create creates course and assigns teacher")
        void createCourse_persistsWithTeacher() throws Exception {
            long countBefore = courseRepo.count();

            mockMvc.perform(MockMvcRequestBuilders.post("/teacher/course/create")
                            .with(SecurityMockMvcRequestPostProcessors.user(teacher.getUsername()).roles("TEACHER"))
                            .param("name", "Operating Systems")
                            .param("departmentId", dept.getId().toString()))
                    .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                    .andExpect(MockMvcResultMatchers.redirectedUrl("/view/courses"));

            Assertions.assertThat(courseRepo.count()).isEqualTo(countBefore + 1);
            // Verify the logged-in teacher is auto-assigned as owner
            Course created = courseRepo.findAll().stream()
                    .filter(c -> c.getName().equals("Operating Systems"))
                    .findFirst().orElseThrow();
            Assertions.assertThat(created.getTeacher()).isNotNull();
            Assertions.assertThat(created.getTeacher().getUsername()).isEqualTo(teacher.getUsername());
        }

        @Test
        @DisplayName("POST /teacher/course/delete/{id} removes course from DB")
        void deleteCourse_removesFromDb() throws Exception {
            Long id = course.getId();

            mockMvc.perform(MockMvcRequestBuilders.post("/teacher/course/delete/" + id)
                            .with(SecurityMockMvcRequestPostProcessors.user(teacher.getUsername()).roles("TEACHER")))
                    .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                    .andExpect(MockMvcResultMatchers.redirectedUrl("/view/courses"));

            Assertions.assertThat(courseRepo.findById(id)).isEmpty();
        }
    }

    // ── Department management ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Department management")
    class DepartmentManagement {

        @Test
        @DisplayName("GET /teacher/department/add renders the add department form")
        void addDepartmentForm_renders() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/teacher/department/add")
                            .with(SecurityMockMvcRequestPostProcessors.user(teacher.getUsername()).roles("TEACHER")))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.view().name("add_department"));
        }

        @Test
        @DisplayName("POST /teacher/department/create creates department and redirects")
        void createDepartment_persistsAndRedirects() throws Exception {
            long countBefore = deptRepo.count();

            mockMvc.perform(MockMvcRequestBuilders.post("/teacher/department/create")
                            .with(SecurityMockMvcRequestPostProcessors.user(teacher.getUsername()).roles("TEACHER"))
                            .param("name", "Mathematics"))
                    .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                    .andExpect(MockMvcResultMatchers.redirectedUrl("/view/departments"));

            Assertions.assertThat(deptRepo.count()).isEqualTo(countBefore + 1);
        }

        @Test
        @DisplayName("POST /teacher/department/delete/{id} removes department")
        void deleteDepartment_removesFromDb() throws Exception {
            Department extra = new Department();
            extra.setName("Removable Dept");
            extra = deptRepo.save(extra);
            Long id = extra.getId();

            mockMvc.perform(MockMvcRequestBuilders.post("/teacher/department/delete/" + id)
                            .with(SecurityMockMvcRequestPostProcessors.user(teacher.getUsername()).roles("TEACHER")))
                    .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                    .andExpect(MockMvcResultMatchers.redirectedUrl("/view/departments"));

            Assertions.assertThat(deptRepo.findById(id)).isEmpty();
        }
    }
}
