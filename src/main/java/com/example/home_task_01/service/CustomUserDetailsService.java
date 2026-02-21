package com.example.home_task_01.service;

import com.example.home_task_01.repository.StudentRepository;
import com.example.home_task_01.repository.TeacherRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private StudentRepository studentRepo;

    @Autowired
    private TeacherRepository teacherRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // Check teachers first
        var teacher = teacherRepo.findByUsername(username);
        if (teacher.isPresent()) {
            return new User(
                teacher.get().getUsername(),
                teacher.get().getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
            );
        }

        // Then check students
        var student = studentRepo.findByUsername(username);
        if (student.isPresent()) {
            return new User(
                student.get().getUsername(),
                student.get().getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
            );
        }

        throw new UsernameNotFoundException("No user found with email: " + username);
    }
}
