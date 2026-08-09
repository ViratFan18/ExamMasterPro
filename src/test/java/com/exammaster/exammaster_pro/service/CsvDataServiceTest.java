package com.exammaster.exammaster_pro.service;

import com.exammaster.exammaster_pro.entity.Allocation;
import com.exammaster.exammaster_pro.entity.AppUser;
import com.exammaster.exammaster_pro.entity.Building;
import com.exammaster.exammaster_pro.entity.Exam;
import com.exammaster.exammaster_pro.entity.Hall;
import com.exammaster.exammaster_pro.entity.Student;
import com.exammaster.exammaster_pro.dto.Responses.ImportResultResponse;
import com.exammaster.exammaster_pro.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CsvDataServiceTest {

    @Mock
    BuildingRepository buildings;
    @Mock
    HallRepository halls;
    @Mock
    StudentRepository students;
    @Mock
    AllocationRepository allocations;
    @Mock
    InvigilatorAllocationRepository invAllocations;
    @Mock
    ComplaintRepository complaints;
    @Mock
    InvigilatorRepository invigilators;
    @Mock
    ExamRepository exams;
    @Mock
    AuditService audits;

    @InjectMocks
    CsvDataService csv;
    @InjectMocks
    ExamMasterService ems;

    private AppUser user() {
        AppUser u = new AppUser();
        u.setId(1L);
        u.setCollegeName("Test College");
        u.setUsername("tester");
        return u;
    }

    @Test
    void headerValidationUnexpectedColumn() throws Exception {
        when(halls.countByUser(any())).thenReturn(1L);
        String csvText = "hallTicketNumber,studentName,branch,year,semester,section,extra\nHT001,John,CSE,3,6,A\n";
        MockMultipartFile file = new MockMultipartFile("file", "students.csv", "text/csv", csvText.getBytes(StandardCharsets.UTF_8));
        ImportResultResponse res = csv.importStudents(user(), file);
        assertThat(res.accepted()).isFalse();
        assertThat(res.errors()).anyMatch(e -> e.field().equals("header") && e.message().contains("Unexpected column"));
    }

    @Test
    void studentImportInvalidTicketChars() throws Exception {
        when(halls.countByUser(any())).thenReturn(1L);
        String csvText = "hallTicketNumber,studentName,branch,year,semester,section\nHT#001,John,CSE,3,6,A\n";
        MockMultipartFile file = new MockMultipartFile("file", "students.csv", "text/csv", csvText.getBytes(StandardCharsets.UTF_8));
        ImportResultResponse res = csv.importStudents(user(), file);
        assertThat(res.accepted()).isFalse();
        assertThat(res.errors()).anyMatch(e -> e.field().equals("hallTicketNumber") && e.message().contains("uppercase letters or digits"));
    }

    @Test
    void invigilatorImportRejectsDuplicateIdsInFile() throws Exception {
        String csvText = "invigilatorId,invigilatorName\nINV001,John Doe\nINV001,Jane Smith\n";
        MockMultipartFile file = new MockMultipartFile("file", "invigilators.csv", "text/csv", csvText.getBytes(StandardCharsets.UTF_8));
        when(invigilators.existsByUserAndInvigilatorIdIgnoreCase(any(), any())).thenReturn(false);

        ImportResultResponse res = csv.importInvigilators(user(), file);

        assertThat(res.accepted()).isFalse();
        assertThat(res.errors()).anyMatch(e -> e.field().equals("invigilatorId") && e.message().contains("Duplicate invigilator ID in file"));
    }

    @Test
    void allocationCsvUsesStudentTicketBuildingHallSeatFormat() {
        AppUser u = user();
        Exam exam = new Exam();
        exam.setUser(u);
        exam.setExamName("Mid Semester");

        Student student = new Student();
        student.setStudentName("Helen Lee");
        student.setHallTicketNumber("HT1050");
        student.setBranch("CSE");
        student.setSection("A");

        Building building = new Building();
        building.setBuildingName("CSE");

        Hall hall = new Hall();
        hall.setHallName("Hall-A");

        Allocation allocation = new Allocation();
        allocation.setStudent(student);
        allocation.setBuilding(building);
        allocation.setHall(hall);
        allocation.setSeatNumber("B01-S1");

        when(exams.findByIdAndUser(1L, u)).thenReturn(java.util.Optional.of(exam));
        when(allocations.findByUserAndExamOrderByHallHallNameAscSeatNumberAsc(u, exam)).thenReturn(List.of(allocation));

        String csv = ems.allocationCsv(u, 1L, null, null);

        assertThat(csv).contains("# ExamMaster Pro");
        assertThat(csv).contains("# College: Test College");
        assertThat(csv).contains("# Exam: Mid Semester");
        assertThat(csv).contains("Student,Ticket,Building,Hall,Seat");
        assertThat(csv).contains("Helen Lee,HT1050,CSE,Hall-A,B01-S1");
        assertThat(csv).doesNotContain("Hall Ticket,Student");
    }

    @Test
    void dryRunStrictModeInfeasible() {
        // ExamMasterService is injected with mocks by Mockito via @InjectMocks
        AppUser u = user();
        // students: 7 of BR1, 3 of BR2
        List<Student> studs = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            Student s = new Student(); s.setUser(u); s.setBranch("BR1"); s.setHallTicketNumber("HT" + i); studs.add(s);
        }
        for (int i = 0; i < 3; i++) {
            Student s = new Student(); s.setUser(u); s.setBranch("BR2"); s.setHallTicketNumber("HTB" + i); studs.add(s);
        }
        when(students.countByUser(u)).thenReturn((long) studs.size());
        when(students.findByUserOrderByHallTicketNumber(u)).thenReturn(studs);
        // halls: one building with 5 benches, studentsPerBench=2 => totalMixBenches = 5
        Building b = new Building(); b.setUser(u); b.setBuildingName("Main");
        Hall h = new Hall(); h.setUser(u); h.setBuilding(b); h.setBenchCount(5); h.setStudentsPerBench(2);
        when(buildings.countByUser(u)).thenReturn(1L);
        when(halls.countByUser(u)).thenReturn(1L);
        when(halls.findByUserOrderByHallName(u)).thenReturn(List.of(h));
        when(invigilators.countByUser(u)).thenReturn(1L);
        // mock exam presence required by dryRun
        com.exammaster.exammaster_pro.entity.Exam examObj = new com.exammaster.exammaster_pro.entity.Exam();
        when(exams.findByIdAndUser(1L, u)).thenReturn(java.util.Optional.of(examObj));

        var resp = ems.dryRun(u, 1L, com.exammaster.exammaster_pro.entity.AllocationMode.STRICT);
        assertThat(resp.ready()).isFalse();
        assertThat(resp.messages()).anyMatch(m -> m.contains("Strict mode not possible"));
    }
}
