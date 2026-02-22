package com.example.home_task_01.repository;

import com.example.home_task_01.Department;
import com.example.home_task_01.Student;
import com.example.home_task_01.Teacher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.assertj.core.api.Assertions;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lightweight repository tests using @DataJpaTest — only loads the JPA layer
 * (no web context, no security). Uses H2 in-memory database.
 */
@DataJpaTest
@ActiveProfiles("test")
class RepositoryIntegrationTest {

    @Autowired
    StudentRepository studentRepo;
    @Autowired
    TeacherRepository teacherRepo;
    @Autowired
    DepartmentRepository deptRepo;
    @Autowired
    CourseRepository courseRepo;

    // ── StudentRepository ────────────────────────────────────────────────────

    @Test
    @DisplayName("findByUsername returns student when username matches")
    void student_findByUsername_found() {
        Student s = new Student();
        s.setFullName("Alice");
        s.setUsername("alice@test.com");
        s.setPassword("hash");
        studentRepo.save(s);

        Optional<Student> result = studentRepo.findByUsername("alice@test.com");
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(result.get().getFullName()).isEqualTo("Alice");
    }

    @Test
    @DisplayName("findByUsername returns empty when username does not exist")
    void student_findByUsername_notFound() {
        Optional<Student> result = studentRepo.findByUsername("nobody@test.com");
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Student is persisted with department relationship")
    void student_persistsWithDepartment() {
        Department dept = new Department();
        dept.setName("CS");
        dept = deptRepo.save(dept);

        Student s = new Student();
        s.setFullName("Bob");
        s.setUsername("bob@test.com");
        s.setPassword("hash");
        s.setDepartment(dept);
        s = studentRepo.save(s);

        Student found = studentRepo.findById(s.getId()).orElseThrow();
        Assertions.assertThat(found.getDepartment()).isNotNull();
        Assertions.assertThat(found.getDepartment().getName()).isEqualTo("CS");
    }

    // ── TeacherRepository ────────────────────────────────────────────────────

    @Test
    @DisplayName("findByUsername returns teacher when username matches")
    void teacher_findByUsername_found() {
        Teacher t = new Teacher();
        t.setFullName("Dr. Smith");
        t.setUsername("smith@uni.com");
        t.setPassword("hash");
        teacherRepo.save(t);

        Optional<Teacher> result = teacherRepo.findByUsername("smith@uni.com");
        Assertions.assertThat(result).isPresent();
        Assertions.assertThat(result.get().getFullName()).isEqualTo("Dr. Smith");
    }

    @Test
    @DisplayName("findByUsername returns empty when teacher does not exist")
    void teacher_findByUsername_notFound() {
        Optional<Teacher> result = teacherRepo.findByUsername("ghost@uni.com");
        Assertions.assertThat(result).isEmpty();
    }

    // ── DepartmentRepository ─────────────────────────────────────────────────

    @Test
    @DisplayName("Department is saved and retrieved by ID")
    void department_savedAndRetrieved() {
        Department dept = new Department();
        dept.setName("Mathematics");
        dept = deptRepo.save(dept);

        Optional<Department> found = deptRepo.findById(dept.getId());
        Assertions.assertThat(found).isPresent();
        Assertions.assertThat(found.get().getName()).isEqualTo("Mathematics");
    }

    @Test
    @DisplayName("Multiple departments can be saved and findAll returns all")
    void department_findAll_returnsAll() {
        deptRepo.deleteAll(); // clean slate for count assertion
        for (String name : new String[]{"CS", "EEE", "ME"}) {
            Department d = new Department();
            d.setName(name);
            deptRepo.save(d);
        }
        Assertions.assertThat(deptRepo.findAll()).hasSize(3);
    }

    // ── CourseRepository ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Course is saved with teacher and department relationships")
    void course_persistsWithRelationships() {
        Department dept = new Department();
        dept.setName("Engineering");
        dept = deptRepo.save(dept);

        Teacher t = new Teacher();
        t.setFullName("Prof. Lee");
        t.setUsername("lee@uni.com");
        t.setPassword("hash");
        t.setDepartment(dept);
        t = teacherRepo.save(t);

        com.example.home_task_01.Course c = new com.example.home_task_01.Course();
        c.setName("Networks");
        c.setDepartment(dept);
        c.setTeacher(t);
        c = courseRepo.save(c);

        com.example.home_task_01.Course found = courseRepo.findById(c.getId()).orElseThrow();
        Assertions.assertThat(found.getDepartment().getName()).isEqualTo("Engineering");
        Assertions.assertThat(found.getTeacher().getUsername()).isEqualTo("lee@uni.com");
    }

    @Test
    @DisplayName("Deleting a course removes it from the repository")
    void course_delete_removesFromDb() {
        com.example.home_task_01.Course c = new com.example.home_task_01.Course();
        c.setName("Temp Course");
        c = courseRepo.save(c);
        Long id = c.getId();

        courseRepo.deleteById(id);

        Assertions.assertThat(courseRepo.findById(id)).isEmpty();
    }
}
