package com.exammaster.exammaster_pro.dto;

import com.exammaster.exammaster_pro.entity.*;

import java.time.Instant;
import java.util.List;

public final class Responses {
    private Responses() {}

    public record AuthResponse(String token, String username, String role, String message) {}
    public record UserResponse(Long id, String collegeName, String username, String email, Role role, boolean enabled) {}
    public record BuildingResponse(Long id, String buildingName, int maxHallCount, long currentHalls, long availableHallSlots, double occupancyPercent) {}
    public record HallResponse(Long id, Long buildingId, String buildingName, String hallName, int benchCount,
                               int studentsPerBench, int capacity, long allocatedStudents, double occupancyPercent) {}
    public record StudentResponse(Long id, String hallTicketNumber, String studentName, String branch, String year, String semester, String section) {}
    public record InvigilatorResponse(Long id, String invigilatorId, String invigilatorName) {}
    public record ExamResponse(Long id, String examName, String academicYear, String semester, ExamType examType) {}
    public record ComplaintResponse(Long id, String title, String description, String category, String email, ComplaintStatus status, Instant createdAt) {}
    public record AdminComplaintResponse(Long id, Long userId, String collegeName, String title, String description, String category, String email, ComplaintStatus status, Instant createdAt) {}
    public record AuditResponse(Long id, String action, String module, String description, String performedBy, Instant performedAt) {}
    public record DashboardResponse(long buildings, long halls, long students, long invigilators, long exams, long allocations,
                                    List<SetupStepResponse> setupSteps, List<AuditResponse> recentActivities) {}
    public record DryRunResponse(long students, long buildings, long halls, long invigilators, int totalCapacity, long remainingCapacity,
                                 boolean ready, AllocationMode mode, List<String> messages, 
                                 List<BranchAnalyticsResponse> branchAnalytics, List<SectionDistributionResponse> sectionDistribution) {}
    public record AllocationGenerateResponse(List<AllocationResponse> allocations, List<String> warnings, int unplacedStudents) {}
    public record AllocationResponse(Long id, Long examId, String examName, Long buildingId, Long hallId,
                                     String studentName, String hallTicketNumber, String buildingName, String hallName,
                                     String branch, String section, String seatNumber) {}
    public record InvigilatorAllocationResponse(Long id, String examName, String invigilatorId, String invigilatorName,
                                                String buildingName, String hallName) {}
    public record SeatResponse(int benchIndex, int seatInBench, String seatNumber, boolean allocated, String studentName,
                               String hallTicketNumber, String branch, String section) {}
    public record BenchResponse(int benchIndex, String benchLabel, List<SeatResponse> seats) {}
    public record BuildingAnalytics(String buildingName, int hallLimit, long currentHalls, double occupancyPercent) {}
    public record StorageAnalytics(Long userId, String collegeName, long buildings, long halls, long students, long invigilators, long exams, long allocations, long estimatedRecords) {}

    public record ImportRowError(int row, String field, String value, String message) {}
    public record ImportResultResponse(boolean accepted, String message, int totalRows, int validRows, int importedCount,
                                       List<ImportRowError> errors) {}
    public record ExportSummaryResponse(long buildings, long halls, long students, long invigilators, long exams,
                                        String whatsappSummary) {}

    public record AllocationStatusResponse(boolean exists, long allocatedCount, String examName) {}
    public record SetupStepResponse(int step, String title, String description, String page, boolean completed, long count) {}
    public record PublicCollegeResponse(String collegeName) {}
    public record PublicExamResponse(Long id, String examName, String semester) {}
    public record StudentSeatResponse(String collegeName, String examName, String studentName, String hallTicketNumber,
                                      String branch, String section, String buildingName, String hallName, String seatNumber) {}
    
    // ── Branch & Section Analytics ──
    public record BranchAnalyticsResponse(String branch, long studentCount, double percentage, int requiredSeats, 
                                         int allocatedSeats, String status, String recommendation) {}
    public record SectionDistributionResponse(String section, long studentCount, String branches, String distributionStrategy) {}
}
