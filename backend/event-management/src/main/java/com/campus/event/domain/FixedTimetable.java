package com.campus.event.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "fixed_timetable")
public class FixedTimetable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @NotBlank
    private String courseName;

    private String courseCode;

    private String section;

    private String semester;

    private String batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id")
    private User faculty;

    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    private LocalTime startTime;

    private LocalTime endTime;

    private String academicYear;

    private boolean isActive = true;

    // Explicit default constructor for Hibernate
    public FixedTimetable() {
    }

    public FixedTimetable(Room room, String courseName, String courseCode, String section, 
                        String semester, String batch, User faculty, DayOfWeek dayOfWeek, 
                        LocalTime startTime, LocalTime endTime, String academicYear) {
        this.room = room;
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.section = section;
        this.semester = semester;
        this.batch = batch;
        this.faculty = faculty;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.academicYear = academicYear;
    }
    
    // Explicit getters and setters to ensure they exist
    public Long getId() { return id; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }
    public User getFaculty() { return faculty; }
    public void setFaculty(User faculty) { this.faculty = faculty; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
