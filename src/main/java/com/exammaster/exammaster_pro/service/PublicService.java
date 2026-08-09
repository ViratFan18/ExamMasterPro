package com.exammaster.exammaster_pro.service;

import com.exammaster.exammaster_pro.dto.Responses.*;
import com.exammaster.exammaster_pro.entity.*;
import com.exammaster.exammaster_pro.exception.ResourceNotFoundException;
import com.exammaster.exammaster_pro.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicService {
    private final AppUserRepository users;
    private final ExamRepository exams;
    private final StudentRepository students;
    private final AllocationRepository allocations;

    public List<PublicCollegeResponse> colleges() {
        return users.findByRoleAndEnabledOrderByCollegeNameAsc(Role.ROLE_USER, true).stream()
                .map(u -> new PublicCollegeResponse(u.getCollegeName()))
                .distinct()
                .toList();
    }

    public List<PublicExamResponse> examsForCollege(String collegeName) {
        AppUser user = collegeUser(collegeName);
        return exams.findByUserOrderByCreatedAtDesc(user).stream()
                .map(e -> new PublicExamResponse(e.getId(), e.getExamName(), e.getSemester()))
                .toList();
    }

    public StudentSeatResponse lookupSeat(String collegeName, String hallTicket, Long examId) {
        AppUser user = collegeUser(collegeName);
        Exam exam = exams.findByIdAndUser(examId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found for this college."));
        Student student = students.findByUserAndHallTicketNumberIgnoreCase(user, hallTicket.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Hall ticket not found. Check your ticket number and college name."));
        Allocation allocation = allocations.findByUserAndExamAndStudent(user, exam, student)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not allocated yet for this exam. Please check back later."));
        return new StudentSeatResponse(
                user.getCollegeName(),
                exam.getExamName(),
                student.getStudentName(),
                student.getHallTicketNumber(),
                student.getBranch(),
                student.getSection(),
                allocation.getBuilding().getBuildingName(),
                allocation.getHall().getHallName(),
                allocation.getSeatNumber()
        );
    }

    private AppUser collegeUser(String collegeName) {
        if (collegeName == null || collegeName.isBlank()) {
            throw new ResourceNotFoundException("College name is required.");
        }
        return users.findByCollegeNameIgnoreCase(collegeName.trim())
                .filter(u -> u.getRole() == Role.ROLE_USER && u.isEnabled())
                .orElseThrow(() -> new ResourceNotFoundException("College not found. Select your college from the list."));
    }
}
