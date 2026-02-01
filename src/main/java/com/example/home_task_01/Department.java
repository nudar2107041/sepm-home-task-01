@Entity
public class Department {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @OneToMany(mappedBy = "department")
    private List<Student> students;

    @OneToMany(mappedBy = "department")
    private List<Teacher> teachers;

    @OneToMany(mappedBy = "department")
    private List<Course> courses;
}
