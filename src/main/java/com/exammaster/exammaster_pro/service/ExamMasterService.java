package com.exammaster.exammaster_pro.service;

import com.exammaster.exammaster_pro.dto.Requests.*;
import com.exammaster.exammaster_pro.dto.Responses.*;
import com.exammaster.exammaster_pro.entity.*;
import com.exammaster.exammaster_pro.exception.BusinessValidationException;
import com.exammaster.exammaster_pro.exception.ResourceNotFoundException;
import com.exammaster.exammaster_pro.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ExamMasterService {
    private static final Logger log = LoggerFactory.getLogger(ExamMasterService.class);
    private final BuildingRepository buildings;
    private final HallRepository halls;
    private final StudentRepository students;
    private final InvigilatorRepository invigilators;
    private final ExamRepository exams;
    private final AllocationRepository allocations;
    private final InvigilatorAllocationRepository invigilatorAllocations;
    private final ComplaintRepository complaints;
    private final AuditService audits;

    public DashboardResponse dashboard(AppUser user) {
        long b = buildings.countByUser(user);
        long h = halls.countByUser(user);
        long s = students.countByUser(user);
        long inv = invigilators.countByUser(user);
        long ex = exams.countByUser(user);
        long alloc = allocations.countByUser(user);
        List<SetupStepResponse> steps = List.of(
                new SetupStepResponse(1, "Buildings", "Add at least one building and set the maximum halls allowed per building.", "buildings", b > 0, b),
                new SetupStepResponse(2, "Halls", "Add halls inside each building with bench count and students per bench (1 or 2).", "halls", h > 0, h),
                new SetupStepResponse(3, "Students", "Add students with unique hall ticket numbers, branch, year, semester, and section.", "students", s > 0, s),
                new SetupStepResponse(4, "Invigilators", "Add one invigilator per hall before generating seat allocation.", "invigilators", inv > 0, inv),
                new SetupStepResponse(5, "Exams", "Create the exam session you want to allocate seats for.", "exams", ex > 0, ex)
        );
        return new DashboardResponse(b, h, s, inv, ex, alloc, steps, audits.recent(user));
    }

    public List<BuildingResponse> buildings(AppUser user) {
        return buildings.findByUserOrderByBuildingName(user).stream().map(this::buildingResponse).toList();
    }

    public BuildingResponse createBuilding(AppUser user, BuildingRequest request) {
        log.info("Creating building '{}' for user '{}'", request.buildingName(), user == null ? "anonymous" : user.getUsername());
        if (buildings.existsByUserAndBuildingNameIgnoreCase(user, request.buildingName())) {
            throw new BusinessValidationException("Building name already exists. Please enter a different building name.");
        }
        Building b = new Building();
        b.setUser(user);
        b.setBuildingName(request.buildingName());
        b.setMaxHallCount(request.maxHallCount());
        buildings.save(b);
        audits.log(user, "Building Created", "Buildings", "Building " + b.getBuildingName() + " created.", user.getUsername());
        return buildingResponse(b);
    }

    public void deleteBuilding(AppUser user, Long id) {
        Building b = building(user, id);
        long count = allocations.countByUser(user);
        if (count > 0) {
            throw new BusinessValidationException("Cannot delete building - " + count + " allocation(s) exist. Delete allocations first.");
        }
        List<Hall> affected = halls.findByUserAndBuildingOrderByHallName(user, b);
        if (!affected.isEmpty()) {
            affected.forEach(halls::delete);
        }
        buildings.delete(b);
        audits.log(user, "Building Deleted", "Buildings", "Building " + b.getBuildingName() + " and " + affected.size() + " halls deleted.", user.getUsername());
    }

    public List<HallResponse> halls(AppUser user) {
        return halls.findByUserOrderByHallName(user).stream().map(this::hallResponse).toList();
    }

    public HallResponse createHall(AppUser user, HallRequest request) {
        log.info("Creating hall '{}' in buildingId={} for user '{}'", request.hallName(), request.buildingId(), user == null ? "anonymous" : user.getUsername());
        if (request.studentsPerBench() != 1 && request.studentsPerBench() != 2) {
            throw new BusinessValidationException("Students per bench must be 1 or 2.");
        }
        if (halls.existsByUserAndHallNameIgnoreCase(user, request.hallName())) {
            throw new BusinessValidationException("Hall name already exists. Please enter a different hall name.");
        }
        Building building = building(user, request.buildingId());
        if (halls.countByUserAndBuilding(user, building) >= building.getMaxHallCount()) {
            throw new BusinessValidationException("Maximum hall limit reached.");
        }
        Hall h = new Hall();
        h.setUser(user);
        h.setBuilding(building);
        h.setHallName(request.hallName());
        h.setBenchCount(request.benchCount());
        h.setStudentsPerBench(request.studentsPerBench());
        h.setCapacity(request.benchCount() * request.studentsPerBench());
        halls.save(h);
        audits.log(user, "Hall Created", "Halls", "Hall " + h.getHallName() + " created with " + h.getBenchCount() + " benches.", user.getUsername());
        return hallResponse(h);
    }

    public void deleteHall(AppUser user, Long id) {
        Hall hall = hall(user, id);
        long count = allocations.countByUser(user);
        if (count > 0) {
            throw new BusinessValidationException("Cannot delete hall - " + count + " allocation(s) exist. Delete allocations first.");
        }
        halls.delete(hall);
        audits.log(user, "Hall Deleted", "Halls", "Hall " + hall.getHallName() + " deleted.", user.getUsername());
    }

    public List<StudentResponse> students(AppUser user) {
        return students.findByUserOrderByHallTicketNumber(user).stream().map(this::studentResponse).toList();
    }

    public StudentResponse createStudent(AppUser user, StudentRequest request) {
        log.info("Adding student '{}' (ticket={}) for user '{}'", request.studentName(), request.hallTicketNumber(), user == null ? "anonymous" : user.getUsername());
        if (students.existsByUserAndHallTicketNumberIgnoreCase(user, request.hallTicketNumber())) {
            throw new BusinessValidationException("Hall ticket number already exists. Please enter a different hall ticket number.");
        }
        Student s = new Student();
        s.setUser(user);
        s.setHallTicketNumber(request.hallTicketNumber());
        s.setStudentName(request.studentName());
        s.setBranch(request.branch());
        s.setYear(request.year());
        s.setSemester(request.semester());
        s.setSection(request.section());
        students.save(s);
        audits.log(user, "Student Created", "Students", "Student " + s.getStudentName() + " added.", user.getUsername());
        return studentResponse(s);
    }

    public void deleteStudent(AppUser user, Long id) {
        Student s = students.findByIdAndUser(id, user).orElseThrow(() -> new ResourceNotFoundException("Student was not found."));
        long count = allocations.countByUser(user);
        if (count > 0) {
            throw new BusinessValidationException("Cannot delete student - " + count + " allocation(s) exist. Delete allocations first.");
        }
        students.delete(s);
        audits.log(user, "Student Deleted", "Students", "Student " + s.getStudentName() + " deleted.", user.getUsername());
    }

    public void deleteAllAllocations(AppUser user) {
        for (Exam exam : exams.findByUserOrderByCreatedAtDesc(user)) {
            invigilatorAllocations.deleteByUserAndExam(user, exam);
            allocations.deleteByUserAndExam(user, exam);
        }
        audits.log(user, "Allocations Cleared", "Seat Allocation", "All allocation data deleted for user workspace.", user.getUsername());
    }

    public void deleteAllBuildings(AppUser user) {
        long count = allocations.countByUser(user);
        if (count > 0) {
            throw new BusinessValidationException("Cannot delete all buildings - " + count + " allocation(s) exist. Delete allocations first.");
        }
        List<Building> list = buildings.findByUserOrderByBuildingName(user);
        for (Building b : list) {
            List<Hall> affected = halls.findByUserAndBuildingOrderByHallName(user, b);
            if (!affected.isEmpty()) {
                halls.deleteAll(affected);
            }
        }
        buildings.deleteAll(list);
        audits.log(user, "Buildings Cleared", "Buildings", "All buildings and related halls deleted for user workspace.", user.getUsername());
    }

    public void deleteAllHalls(AppUser user) {
        long count = allocations.countByUser(user);
        if (count > 0) {
            throw new BusinessValidationException("Cannot delete all halls - " + count + " allocation(s) exist. Delete allocations first.");
        }
        List<Hall> list = halls.findByUserOrderByHallName(user);
        halls.deleteAll(list);
        audits.log(user, "Halls Cleared", "Halls", "All halls deleted for user workspace.", user.getUsername());
    }

    public void deleteAllStudents(AppUser user) {
        long count = allocations.countByUser(user);
        if (count > 0) {
            throw new BusinessValidationException("Cannot delete all students - " + count + " allocation(s) exist. Delete allocations first.");
        }
        List<Student> list = students.findByUserOrderByHallTicketNumber(user);
        students.deleteAll(list);
        audits.log(user, "Students Cleared", "Students", "All students deleted for user workspace.", user.getUsername());
    }

    public void deleteAllInvigilators(AppUser user) {
        long count = allocations.countByUser(user);
        if (count > 0) {
            throw new BusinessValidationException("Cannot delete all invigilators - " + count + " allocation(s) exist. Delete allocations first.");
        }
        List<Invigilator> list = invigilators.findByUserOrderByInvigilatorId(user);
        invigilators.deleteAll(list);
        audits.log(user, "Invigilators Cleared", "Invigilators", "All invigilators deleted for user workspace.", user.getUsername());
    }

    public void deleteAllExams(AppUser user) {
        long count = allocations.countByUser(user);
        if (count > 0) {
            throw new BusinessValidationException("Cannot delete all exams - " + count + " allocation(s) exist. Delete allocations first.");
        }
        List<Exam> list = exams.findByUserOrderByCreatedAtDesc(user);
        exams.deleteAll(list);
        audits.log(user, "Exams Cleared", "Exams", "All exams deleted for user workspace.", user.getUsername());
    }

    public List<InvigilatorResponse> invigilators(AppUser user) {
        return invigilators.findByUserOrderByInvigilatorId(user).stream().map(this::invigilatorResponse).toList();
    }

    public InvigilatorResponse createInvigilator(AppUser user, InvigilatorRequest request) {
        if (invigilators.existsByUserAndInvigilatorIdIgnoreCase(user, request.invigilatorId())) {
            throw new BusinessValidationException("Invigilator ID already exists. Please enter a different invigilator ID.");
        }
        Invigilator inv = new Invigilator();
        inv.setUser(user);
        inv.setInvigilatorId(request.invigilatorId());
        inv.setInvigilatorName(request.invigilatorName());
        invigilators.save(inv);
        audits.log(user, "Invigilator Created", "Invigilators", "Invigilator " + inv.getInvigilatorName() + " added.", user.getUsername());
        return invigilatorResponse(inv);
    }

    public void deleteInvigilator(AppUser user, Long id) {
        Invigilator inv = invigilators.findByIdAndUser(id, user).orElseThrow(() -> new ResourceNotFoundException("Invigilator was not found."));
        long count = allocations.countByUser(user);
        if (count > 0) {
            throw new BusinessValidationException("Cannot delete invigilator - " + count + " allocation(s) exist. Delete allocations first.");
        }
        invigilators.delete(inv);
        audits.log(user, "Invigilator Deleted", "Invigilators", "Invigilator " + inv.getInvigilatorName() + " deleted.", user.getUsername());
    }

    public List<ExamResponse> exams(AppUser user) {
        return exams.findByUserOrderByCreatedAtDesc(user).stream().map(this::examResponse).toList();
    }

    public ExamResponse createExam(AppUser user, ExamRequest request) {
        log.info("Creating exam '{}' (semester={}) for user '{}'", request.examName(), request.semester(), user == null ? "anonymous" : user.getUsername());
        if (exams.existsByUserAndExamNameIgnoreCaseAndSemesterIgnoreCaseAndExamType(user, request.examName(), request.semester(), request.examType())) {
            throw new BusinessValidationException("No duplicate exam is allowed.");
        }
        Exam exam = new Exam();
        exam.setUser(user);
        exam.setExamName(request.examName());
        exam.setAcademicYear(request.academicYear());
        exam.setSemester(request.semester());
        exam.setExamType(request.examType());
        exams.save(exam);
        audits.log(user, "Exam Created", "Exams", "Exam " + exam.getExamName() + " created.", user.getUsername());
        return examResponse(exam);
    }

    public void deleteExam(AppUser user, Long id) {
        Exam exam = exam(user, id);
        long count = allocations.countByUserAndExam(user, exam);
        if (count > 0) {
            throw new BusinessValidationException("Cannot delete exam - " + count + " allocation(s) exist. Delete allocations first.");
        }
        exams.delete(exam);
        audits.log(user, "Exam Deleted", "Exams", "Exam " + exam.getExamName() + " deleted.", user.getUsername());
    }

    @Transactional(readOnly = true)
    public DryRunResponse dryRun(AppUser user, Long examId, AllocationMode mode) {
        exam(user, examId);
        long studentCount = students.countByUser(user);
        long buildingCount = buildings.countByUser(user);
        long hallCount = halls.countByUser(user);
        long invigilatorCount = invigilators.countByUser(user);
        int capacity = totalEffectiveCapacity(user, mode);
        List<String> messages = new ArrayList<>();
        if (studentCount == 0) messages.add("Students are required before allocation.");
        if (buildingCount == 0) messages.add("Buildings are required before allocation.");
        if (hallCount == 0) messages.add("Halls are required before allocation.");
        if (invigilatorCount == 0) messages.add("Invigilators are required before allocation.");
        if (capacity < studentCount) {
            if (capacity == 0) {
                messages.add("No seating capacity available. Add halls before allocation.");
            } else {
                messages.add((studentCount - capacity) + " additional seats are required. Please add more halls or increase hall capacity.");
            }
        }
        if (invigilatorCount < hallCount) messages.add((hallCount - invigilatorCount) + " additional invigilators are required.");
        
        // Get branch analytics
        List<BranchAnalyticsResponse> branchAnalytics = computeBranchAnalytics(user, mode, capacity);
        
        // Get section distribution
        List<SectionDistributionResponse> sectionDistribution = computeSectionDistribution(user);
        
        if (mode == AllocationMode.STRICT) {
            // Check branch distribution feasibility for strict mode (mixed benches)
            int totalMixBenches = orderedHalls(user).stream().mapToInt(h -> h.getStudentsPerBench() > 1 ? h.getBenchCount() : 0).sum();
            if (totalMixBenches == 0) {
                messages.add("Strict mode requires benches with 2 students; none found.");
            } else {
                Map<String, Long> branchCounts = students.findByUserOrderByHallTicketNumber(user).stream()
                        .collect(java.util.stream.Collectors.groupingBy(s -> normalizeBranch(s.getBranch()), java.util.stream.Collectors.counting()));
                long maxBranch = branchCounts.values().stream().mapToLong(Long::longValue).max().orElse(0L);
                if (maxBranch > totalMixBenches) {
                    messages.add("Strict mode not possible: largest branch has " + maxBranch + " students but only " + totalMixBenches + " mixed benches available.");
                }
            }
        } else if (mode == AllocationMode.FLEXIBLE) {
            messages.add("Flexible mode enabled: Smart auto-balancing based on branch distribution.");
            // Warn about highly imbalanced branches
            long maxBranchCount = branchAnalytics.stream().mapToLong(BranchAnalyticsResponse::studentCount).max().orElse(0L);
            long minBranchCount = branchAnalytics.stream().mapToLong(BranchAnalyticsResponse::studentCount).min().orElse(0L);
            double imbalanceRatio = minBranchCount > 0 ? (double) maxBranchCount / minBranchCount : Double.MAX_VALUE;
            if (imbalanceRatio > 3.0) {
                messages.add("⚠️ High branch imbalance detected (ratio: " + String.format("%.1f", imbalanceRatio) + "):1. Consider adjusting to dedicated halls for major branches.");
            }
        }
        
        boolean ready = messages.isEmpty();
        if (ready) {
            String modeLabel = mode == AllocationMode.FREE ? "Free (1 student per bench)" 
                             : mode == AllocationMode.STRICT ? "Strict (branch-mixed benches)"
                             : "Flexible (smart auto-balancing)";
            messages.add("Allocation readiness check passed for " + modeLabel + " mode.");
        }
        return new DryRunResponse(studentCount, buildingCount, hallCount, invigilatorCount, capacity, capacity - studentCount, ready, mode, messages, branchAnalytics, sectionDistribution);
    }

    public AllocationGenerateResponse generateAllocation(AppUser user, Long examId, boolean replaceExisting, AllocationMode mode) {
        log.info("Starting allocation generation for examId={} user={} replaceExisting={} mode={}", examId, user == null ? "anonymous" : user.getUsername(), replaceExisting, mode);
        Exam exam = exam(user, examId);
        DryRunResponse dryRun = dryRun(user, examId, mode);
        if (!dryRun.ready()) {
            throw new BusinessValidationException("Allocation cannot be generated. " + String.join(" ", dryRun.messages()));
        }
        if (allocations.existsByUserAndExam(user, exam)) {
            if (!replaceExisting) {
                throw new BusinessValidationException("Previous allocation found. We recommend exporting a backup before continuing.");
            }
            invigilatorAllocations.deleteByUserAndExam(user, exam);
            allocations.deleteByUserAndExam(user, exam);
        }
        List<Student> studentList = new ArrayList<>(students.findByUserOrderByHallTicketNumber(user));
        
        // Apply section-based ordering for better distribution
        studentList.sort(Comparator
            .comparing((Student s) -> s.getSection() == null ? "" : s.getSection())
            .thenComparing(s -> normalizeBranch(s.getBranch()))
            .thenComparing(Student::getHallTicketNumber)
        );
        
        Collections.shuffle(studentList); // Shuffle within sections to randomize order
        List<Hall> hallList = orderedHalls(user);
        List<Invigilator> invList = invigilators.findByUserOrderByInvigilatorId(user);

        PlacementResult placement = placeStudents(user, exam, studentList, hallList, mode);
        placement.allocations.forEach(allocations::save);

        for (int i = 0; i < hallList.size(); i++) {
            Hall hall = hallList.get(i);
            InvigilatorAllocation ia = new InvigilatorAllocation();
            ia.setUser(user);
            ia.setExam(exam);
            ia.setInvigilator(invList.get(i));
            ia.setBuilding(hall.getBuilding());
            ia.setHall(hall);
            invigilatorAllocations.save(ia);
        }

        String modeLabel = mode == AllocationMode.FREE ? "Free" 
                         : mode == AllocationMode.STRICT ? "Strict"
                         : "Flexible";
        audits.log(user, "Allocation Generated", "Seat Allocation",
                modeLabel + " allocation generated for " + exam.getExamName() + " with " + placement.allocations.size() + " seats filled.",
                user.getUsername());
        log.info("Allocation generation completed for exam '{}' with {} seats filled.", exam.getExamName(), placement.allocations.size());

        List<AllocationResponse> report = allocationReport(user, examId, null, null);
        return new AllocationGenerateResponse(report, placement.warnings, placement.unplaced);
    }

    public AllocationStatusResponse allocationStatus(AppUser user, Long examId) {
        Exam exam = exam(user, examId);
        long count = allocations.countByUserAndExam(user, exam);
        return new AllocationStatusResponse(count > 0, count, exam.getExamName());
    }

    public void deleteAllocation(AppUser user, Long examId) {
        Exam exam = exam(user, examId);
        if (!allocations.existsByUserAndExam(user, exam)) {
            throw new BusinessValidationException("No allocation exists for " + exam.getExamName() + ".");
        }
        invigilatorAllocations.deleteByUserAndExam(user, exam);
        allocations.deleteByUserAndExam(user, exam);
        audits.log(user, "Allocation Deleted", "Seat Allocation",
                "Allocation deleted for " + exam.getExamName() + ".", user.getUsername());
    }

    public List<AllocationResponse> allocationReport(AppUser user, Long examId, Long buildingId, Long hallId) {
        Exam exam = exam(user, examId);
        List<Allocation> list;
        if (hallId != null) {
            list = allocations.findByUserAndExamAndHallOrderBySeatNumberAsc(user, exam, hall(user, hallId));
        } else if (buildingId != null) {
            list = allocations.findByUserAndExamAndBuildingOrderByHallHallNameAscSeatNumberAsc(user, exam, building(user, buildingId));
        } else {
            list = allocations.findByUserAndExamOrderByHallHallNameAscSeatNumberAsc(user, exam);
        }
        return list.stream().map(this::allocationResponse).toList();
    }

    public List<InvigilatorAllocationResponse> invigilatorReport(AppUser user, Long examId) {
        Exam exam = exam(user, examId);
        return invigilatorAllocations.findByUserAndExamOrderByHallHallName(user, exam).stream().map(ia ->
                new InvigilatorAllocationResponse(ia.getId(), ia.getExam().getExamName(), ia.getInvigilator().getInvigilatorId(),
                        ia.getInvigilator().getInvigilatorName(), ia.getBuilding().getBuildingName(), ia.getHall().getHallName())).toList();
    }

    public List<BenchResponse> seats(AppUser user, Long hallId) {
        Hall hall = hall(user, hallId);
        Map<String, Allocation> bySeat = new HashMap<>();
        for (Allocation a : allocations.findByUserAndHallOrderBySeatNumberAsc(user, hall)) {
            bySeat.put(a.getSeatNumber(), a);
        }
        List<BenchResponse> benches = new ArrayList<>();
        for (int bench = 1; bench <= hall.getBenchCount(); bench++) {
            String benchLabel = "B" + String.format("%02d", bench);
            List<SeatResponse> seatResponses = new ArrayList<>();
            int seatsOnBench = hall.getStudentsPerBench();
            for (int seat = 1; seat <= seatsOnBench; seat++) {
                String seatNumber = benchLabel + "-S" + seat;
                Allocation a = bySeat.get(seatNumber);
                seatResponses.add(new SeatResponse(bench, seat, seatNumber, a != null,
                        a == null ? null : a.getStudent().getStudentName(),
                        a == null ? null : a.getStudent().getHallTicketNumber(),
                        a == null ? null : a.getStudent().getBranch(),
                        a == null ? null : a.getStudent().getSection()));
            }
            benches.add(new BenchResponse(bench, benchLabel, seatResponses));
        }
        return benches;
    }

    public List<BuildingAnalytics> buildingAnalytics(AppUser user) {
        return buildings.findByUserOrderByBuildingName(user).stream()
                .map(b -> new BuildingAnalytics(b.getBuildingName(), b.getMaxHallCount(), halls.countByUserAndBuilding(user, b), percent(halls.countByUserAndBuilding(user, b), b.getMaxHallCount())))
                .toList();
    }

    public List<ComplaintResponse> complaints(AppUser user) {
        return complaints.findByUserOrderByCreatedAtDesc(user).stream().map(this::complaintResponse).toList();
    }

    public ComplaintResponse createComplaint(AppUser user, ComplaintRequest request) {
        Complaint complaint = new Complaint();
        complaint.setUser(user);
        complaint.setTitle(request.title());
        complaint.setDescription(request.description());
        complaint.setCategory(request.category());
        complaint.setEmail(request.email());
        complaints.save(complaint);
        audits.log(user, "Complaint Created", "Complaints", "Complaint " + complaint.getTitle() + " created.", user.getUsername());
        return complaintResponse(complaint);
    }

    public void deleteComplaint(AppUser user, Long id) {
        Complaint complaint = complaints.findByIdAndUser(id, user).orElseThrow(() -> new ResourceNotFoundException("Complaint was not found."));
        if (complaint.getStatus() != ComplaintStatus.RESOLVED && complaint.getStatus() != ComplaintStatus.CLOSED) {
            throw new BusinessValidationException("Only resolved or closed complaints can be deleted.");
        }
        complaints.delete(complaint);
        audits.log(user, "Complaint Deleted", "Complaints", "Complaint " + complaint.getTitle() + " deleted.", user.getUsername());
    }

    public ComplaintResponse updateComplaint(Long id, ComplaintStatus status, String adminName) {
        Complaint complaint = complaints.findById(id).orElseThrow(() -> new ResourceNotFoundException("Complaint was not found."));
        complaint.setStatus(status);
        audits.log(complaint.getUser(), "Complaint Updated", "Complaints", "Complaint " + complaint.getTitle() + " set to " + status + ".", adminName);
        return complaintResponse(complaint);
    }

        @Transactional(readOnly = true)
        public StudentSeatResponse lookupSeat(AppUser user, Long examId, String hallTicket) {
        Exam exam = exams.findByIdAndUser(examId, user).orElseThrow(() -> new ResourceNotFoundException("Exam was not found."));
        Student student = students.findByUserAndHallTicketNumberIgnoreCase(user, hallTicket == null ? "" : hallTicket.trim())
            .orElseThrow(() -> new ResourceNotFoundException("Hall ticket not found. Check your ticket number."));
        Allocation allocation = allocations.findByUserAndExamAndStudent(user, exam, student)
            .orElseThrow(() -> new ResourceNotFoundException("Seat not allocated yet for this exam."));
        return new StudentSeatResponse(user.getCollegeName(), exam.getExamName(), student.getStudentName(), student.getHallTicketNumber(), student.getBranch(), student.getSection(), allocation.getBuilding().getBuildingName(), allocation.getHall().getHallName(), allocation.getSeatNumber());
        }

    public String allocationCsv(AppUser user, Long examId, Long buildingId, Long hallId) {
        List<Allocation> list;
        Exam exam = exam(user, examId);
        String buildingName = null;
        String hallName = null;
        if (hallId != null) {
            Hall hall = hall(user, hallId);
            buildingName = hall.getBuilding().getBuildingName();
            hallName = hall.getHallName();
            list = allocations.findByUserAndExamAndHallOrderBySeatNumberAsc(user, exam, hall);
        } else if (buildingId != null) {
            Building building = building(user, buildingId);
            buildingName = building.getBuildingName();
            list = allocations.findByUserAndExamAndBuildingOrderByHallHallNameAscSeatNumberAsc(user, exam, building);
        } else {
            list = allocations.findByUserAndExamOrderByHallHallNameAscSeatNumberAsc(user, exam);
        }
        return buildAllocationCsv(user, exam.getExamName(), buildingName, hallName, list);
    }

    public String allocationCsvFilename(AppUser user, Long examId, Long buildingId, Long hallId) {
        Exam exam = exam(user, examId);
        String base = sanitizeFilename(exam.getExamName());
        if (hallId != null) {
            Hall hall = hall(user, hallId);
            return base + "-" + sanitizeFilename(hall.getBuilding().getBuildingName()) + "-"
                    + sanitizeFilename(hall.getHallName()) + "-allocation.csv";
        }
        if (buildingId != null) {
            return base + "-" + sanitizeFilename(building(user, buildingId).getBuildingName()) + "-allocation.csv";
        }
        return base + "-allocation.csv";
    }

    private String buildAllocationCsv(AppUser user, String examName, String buildingName, String hallName, List<Allocation> list) {
        StringBuilder csv = new StringBuilder();
        csv.append("ExamMaster Pro\n");
        csv.append("College: ").append(user.getCollegeName()).append("\n");
        csv.append("Exam: ").append(examName).append("\n");
        if (buildingName != null) {
            csv.append("Building: ").append(buildingName).append("\n");
        }
        if (hallName != null) {
            csv.append("Hall: ").append(hallName).append("\n");
        }
        csv.append("\nStudent\tTicket\tBuilding\tHall\tSeat\n");
        for (Allocation a : list) {
            csv.append(a.getStudent().getStudentName()).append("\t")
                    .append(a.getStudent().getHallTicketNumber()).append("\t")
                    .append(a.getBuilding().getBuildingName()).append("\t")
                    .append(a.getHall().getHallName()).append("\t")
                    .append(a.getSeatNumber()).append("\n");
        }
        return csv.toString();
    }

    private String sanitizeFilename(String value) {
        return value == null ? "export" : value.trim().replaceAll("[^a-zA-Z0-9._-]+", "-").replaceAll("-+", "-");
    }

    private PlacementResult placeStudents(AppUser user, Exam exam, List<Student> remaining, List<Hall> hallList, AllocationMode mode) {
        PlacementResult result = new PlacementResult();
        
        if (mode == AllocationMode.FLEXIBLE) {
            return placeStudentsFlexible(user, exam, new ArrayList<>(remaining), hallList, result);
        }
        
        for (Hall hall : hallList) {
            for (int bench = 1; bench <= hall.getBenchCount() && !remaining.isEmpty(); bench++) {
                int seatsNeeded = mode == AllocationMode.FREE ? 1 : hall.getStudentsPerBench();
                String benchLabel = "B" + String.format("%02d", bench);
                if (seatsNeeded == 1) {
                    Student student = remaining.remove(0);
                    result.allocations.add(buildAllocation(user, exam, hall, student, benchLabel + "-S1"));
                } else {
                    Student first = remaining.remove(0);
                    result.allocations.add(buildAllocation(user, exam, hall, first, benchLabel + "-S1"));
                    if (remaining.isEmpty()) break;
                    String firstBranch = normalizeBranch(first.getBranch());
                    int mixedIndex = findDifferentBranchIndex(remaining, firstBranch);
                    if (mixedIndex >= 0) {
                        Student second = remaining.remove(mixedIndex);
                        result.allocations.add(buildAllocation(user, exam, hall, second, benchLabel + "-S2"));
                    } else {
                        Student second = remaining.remove(0);
                        result.allocations.add(buildAllocation(user, exam, hall, second, benchLabel + "-S2"));
                        result.warnings.add("Branch conflict on " + benchLabel + " in " + hall.getHallName()
                                + ": both students are from " + firstBranch + ".");
                    }
                }
            }
        }
        result.unplaced = remaining.size();
        if (result.unplaced > 0) {
            result.warnings.add(result.unplaced + " students could not be placed due to insufficient bench capacity.");
        }
        return result;
    }
    
    // ── FLEXIBLE MODE: Smart auto-balancing based on branch distribution ──
    private PlacementResult placeStudentsFlexible(AppUser user, Exam exam, List<Student> remaining, List<Hall> hallList, PlacementResult result) {
        Map<String, List<Student>> branchGroups = new HashMap<>();
        for (Student s : remaining) {
            String branch = normalizeBranch(s.getBranch());
            branchGroups.computeIfAbsent(branch, k -> new ArrayList<>()).add(s);
        }
        
        // Sort branches by count (largest first)
        List<String> sortedBranches = branchGroups.keySet().stream()
            .sorted((b1, b2) -> Long.compare(branchGroups.get(b2).size(), branchGroups.get(b1).size()))
            .toList();
        
        // For major branch, allocate dedicated halls; for others, use shared halls
        int hallIndex = 0;
        for (String branch : sortedBranches) {
            List<Student> branchStudents = branchGroups.get(branch);
            double branchPercentage = (double) branchStudents.size() / remaining.size() * 100;
            
            // Allocate proportional halls to branch
            int hallsNeeded = Math.max(1, (int) Math.ceil(branchPercentage / 100.0 * hallList.size()));
            
            for (int h = 0; h < hallsNeeded && hallIndex < hallList.size() && !branchStudents.isEmpty(); h++) {
                Hall hall = hallList.get(hallIndex++);
                for (int bench = 1; bench <= hall.getBenchCount() && !branchStudents.isEmpty(); bench++) {
                    String benchLabel = "B" + String.format("%02d", bench);
                    // Fill first seat
                    Student first = branchStudents.remove(0);
                    result.allocations.add(buildAllocation(user, exam, hall, first, benchLabel + "-S1"));
                    
                    // Fill second seat if available
                    if (hall.getStudentsPerBench() > 1) {
                        if (!branchStudents.isEmpty()) {
                            Student second = branchStudents.remove(0);
                            result.allocations.add(buildAllocation(user, exam, hall, second, benchLabel + "-S2"));
                        } else if (!remaining.isEmpty()) {
                            // Use student from different branch if available
                            Student second = remaining.remove(0);
                            result.allocations.add(buildAllocation(user, exam, hall, second, benchLabel + "-S2"));
                            branchGroups.get(normalizeBranch(second.getBranch())).remove(second);
                        }
                    }
                }
            }
        }
        
        // Fill remaining halls with leftover students
        while (hallIndex < hallList.size() && !remaining.isEmpty()) {
            Hall hall = hallList.get(hallIndex++);
            for (int bench = 1; bench <= hall.getBenchCount() && !remaining.isEmpty(); bench++) {
                String benchLabel = "B" + String.format("%02d", bench);
                Student first = remaining.remove(0);
                result.allocations.add(buildAllocation(user, exam, hall, first, benchLabel + "-S1"));
                
                if (hall.getStudentsPerBench() > 1 && !remaining.isEmpty()) {
                    Student second = remaining.remove(0);
                    result.allocations.add(buildAllocation(user, exam, hall, second, benchLabel + "-S2"));
                }
            }
        }
        
        result.unplaced = remaining.size();
        if (result.unplaced > 0) {
            result.warnings.add("Flexible: " + result.unplaced + " students could not be placed due to insufficient bench capacity.");
        } else {
            result.warnings.add("Flexible mode: Allocation completed with smart branch distribution. Major branches allocated to dedicated halls.");
        }
        return result;
    }

    private Allocation buildAllocation(AppUser user, Exam exam, Hall hall, Student student, String seatNumber) {
        Allocation allocation = new Allocation();
        allocation.setUser(user);
        allocation.setExam(exam);
        allocation.setStudent(student);
        allocation.setBuilding(hall.getBuilding());
        allocation.setHall(hall);
        allocation.setSeatNumber(seatNumber);
        return allocation;
    }

    private int findDifferentBranchIndex(List<Student> remaining, String branch) {
        for (int i = 0; i < remaining.size(); i++) {
            if (!normalizeBranch(remaining.get(i).getBranch()).equals(branch)) {
                return i;
            }
        }
        return -1;
    }

    // ── Branch Analytics ──
    private List<BranchAnalyticsResponse> computeBranchAnalytics(AppUser user, AllocationMode mode, int totalCapacity) {
        List<Student> allStudents = students.findByUserOrderByHallTicketNumber(user);
        Map<String, List<Student>> branchGroups = allStudents.stream()
            .collect(java.util.stream.Collectors.groupingBy(s -> normalizeBranch(s.getBranch())));
        
        List<BranchAnalyticsResponse> analytics = new ArrayList<>();
        long totalStudents = allStudents.size();
        
        for (Map.Entry<String, List<Student>> entry : branchGroups.entrySet()) {
            String branch = entry.getKey();
            long count = entry.getValue().size();
            double percentage = totalStudents > 0 ? (double) count / totalStudents * 100 : 0;
            int requiredSeats = (int) Math.ceil((double) count / (mode == AllocationMode.FREE ? 1 : 2));
            int allocatedSeats = (int) count;
            
            String status = count <= requiredSeats ? "✓ OK" : "⚠️ May need more benches";
            String recommendation = percentage > 40 ? "Consider dedicated halls for this branch"
                                  : percentage > 20 ? "Mix with smaller branches"
                                  : "Can be combined with other branches";
            
            analytics.add(new BranchAnalyticsResponse(branch, count, percentage, requiredSeats, allocatedSeats, status, recommendation));
        }
        
        return analytics;
    }
    
    // ── Section Distribution ──
    private List<SectionDistributionResponse> computeSectionDistribution(AppUser user) {
        List<Student> allStudents = students.findByUserOrderByHallTicketNumber(user);
        Map<String, List<Student>> sectionGroups = allStudents.stream()
            .collect(java.util.stream.Collectors.groupingBy(s -> s.getSection() == null ? "NONE" : s.getSection()));
        
        List<SectionDistributionResponse> distribution = new ArrayList<>();
        
        for (Map.Entry<String, List<Student>> entry : sectionGroups.entrySet()) {
            String section = entry.getKey();
            List<Student> students = entry.getValue();
            long count = students.size();
            
            // Get unique branches in this section
            Set<String> branches = students.stream()
                .map(s -> normalizeBranch(s.getBranch()))
                .collect(java.util.stream.Collectors.toSet());
            String branchesStr = String.join(", ", branches);
            
            String strategy = branches.size() > 1 ? "Mixed branches in section" : "Single branch section";
            
            distribution.add(new SectionDistributionResponse(section, count, branchesStr, strategy));
        }
        
        return distribution;
    }

    private String normalizeBranch(String branch) {
        return branch == null ? "" : branch.trim().toUpperCase(Locale.ROOT);
    }

    private List<Hall> orderedHalls(AppUser user) {
        return halls.findByUserOrderByHallName(user).stream()
                .sorted(Comparator.comparing((Hall h) -> h.getBuilding().getBuildingName(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Hall::getHallName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private int totalEffectiveCapacity(AppUser user, AllocationMode mode) {
        return orderedHalls(user).stream().mapToInt(h -> effectiveCapacity(h, mode)).sum();
    }

    private int effectiveCapacity(Hall hall, AllocationMode mode) {
        return mode == AllocationMode.FREE ? hall.getBenchCount() : hall.getBenchCount() * hall.getStudentsPerBench();
    }

    private String q(String value) {
        return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\"";
    }

    private Building building(AppUser user, Long id) {
        return buildings.findByIdAndUser(id, user).orElseThrow(() -> new ResourceNotFoundException("Building was not found."));
    }

    private Hall hall(AppUser user, Long id) {
        return halls.findByIdAndUser(id, user).orElseThrow(() -> new ResourceNotFoundException("Hall was not found."));
    }

    private Exam exam(AppUser user, Long id) {
        return exams.findByIdAndUser(id, user).orElseThrow(() -> new ResourceNotFoundException("Exam was not found."));
    }

    private BuildingResponse buildingResponse(Building b) {
        long current = halls.countByUserAndBuilding(b.getUser(), b);
        return new BuildingResponse(b.getId(), b.getBuildingName(), b.getMaxHallCount(), current, Math.max(0, b.getMaxHallCount() - current), percent(current, b.getMaxHallCount()));
    }

    private HallResponse hallResponse(Hall h) {
        long allocated = allocations.countByUserAndHall(h.getUser(), h);
        return new HallResponse(h.getId(), h.getBuilding().getId(), h.getBuilding().getBuildingName(), h.getHallName(),
                h.getBenchCount(), h.getStudentsPerBench(), h.getCapacity(), allocated, percent(allocated, h.getCapacity()));
    }

    private StudentResponse studentResponse(Student s) {
        return new StudentResponse(s.getId(), s.getHallTicketNumber(), s.getStudentName(), s.getBranch(), s.getYear(), s.getSemester(), s.getSection());
    }

    private InvigilatorResponse invigilatorResponse(Invigilator inv) {
        return new InvigilatorResponse(inv.getId(), inv.getInvigilatorId(), inv.getInvigilatorName());
    }

    private ExamResponse examResponse(Exam e) {
        return new ExamResponse(e.getId(), e.getExamName(), e.getAcademicYear(), e.getSemester(), e.getExamType());
    }

    private AllocationResponse allocationResponse(Allocation a) {
        return new AllocationResponse(a.getId(), a.getExam().getId(), a.getExam().getExamName(), a.getBuilding().getId(),
                a.getHall().getId(), a.getStudent().getStudentName(), a.getStudent().getHallTicketNumber(),
                a.getBuilding().getBuildingName(), a.getHall().getHallName(), a.getStudent().getBranch(),
                a.getStudent().getSection(), a.getSeatNumber());
    }

    private ComplaintResponse complaintResponse(Complaint c) {
        return new ComplaintResponse(c.getId(), c.getTitle(), c.getDescription(), c.getCategory(), c.getEmail(), c.getStatus(), c.getCreatedAt());
    }

    private double percent(double current, double max) {
        return max <= 0 ? 0 : Math.round((current / max) * 10000.0) / 100.0;
    }

    private static class PlacementResult {
        private final List<Allocation> allocations = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private int unplaced;
    }
}
