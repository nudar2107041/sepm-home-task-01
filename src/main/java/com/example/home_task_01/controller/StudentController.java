package com.example.home_task_01.controller;

@RestController
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private StudentRepository studentRepo;

    @GetMapping("/profile/{id}")
    public Student getProfile(@PathVariable Long id) {
        return studentRepo.findById(id).orElseThrow();
    }

    @PutMapping("/profile/{id}")
    public Student updateProfile(@PathVariable Long id, @RequestBody Student updated) {
        Student student = studentRepo.findById(id).orElseThrow();
        student.setFullName(updated.getFullName());
        return studentRepo.save(student);
    }
}

