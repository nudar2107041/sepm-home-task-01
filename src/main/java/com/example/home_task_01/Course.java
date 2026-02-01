@Entity
public class Course {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToMany(mappedBy = "courses")
    private List<Student> students;

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;
}
