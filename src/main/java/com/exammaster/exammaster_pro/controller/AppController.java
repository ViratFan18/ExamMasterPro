package com.exammaster.exammaster_pro.controller;

import com.exammaster.exammaster_pro.dto.*;
import com.exammaster.exammaster_pro.dto.Requests.*;
import com.exammaster.exammaster_pro.entity.AllocationMode;
import com.exammaster.exammaster_pro.entity.AppUser;
import com.exammaster.exammaster_pro.exception.BusinessValidationException;
import com.exammaster.exammaster_pro.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AppController {
    private final CurrentUserService current;
    private final ExamMasterService app;
    private final CsvDataService csv;
    private final AuditService audits;

    private static final Logger log = LoggerFactory.getLogger(AppController.class);

    private AppUser user(Long workspaceUserId) {
        return current.workspaceUser(workspaceUserId);
    }

    private ApiResponse<?> handleImport(Supplier<com.exammaster.exammaster_pro.dto.Responses.ImportResultResponse> action) {
        try {
            var result = action.get();
            return result.accepted() ? ApiResponse.ok(result.message(), result) : ApiResponse.fail(result.message(), result);
        } catch (BusinessValidationException ex) {
            return ApiResponse.fail(ex.getMessage());
        } catch (DataAccessException ex) {
            return ApiResponse.fail("Database error during CSV import. Please verify your file and try again.");
        } catch (Exception ex) {
            return ApiResponse.fail("Import failed unexpectedly. Please fix your file and re-upload or contact the administrator.");
        }
    }

    @GetMapping("/dashboard")
    ApiResponse<?> dashboard(@RequestParam(required = false) Long workspaceUserId) {
        return ApiResponse.ok("Dashboard loaded successfully.", app.dashboard(user(workspaceUserId)));
    }

    @GetMapping("/buildings")
    ApiResponse<?> buildings(@RequestParam(required = false) Long workspaceUserId) {
        return ApiResponse.ok("Buildings loaded successfully.", app.buildings(user(workspaceUserId)));
    }

    @PostMapping("/buildings")
    ApiResponse<?> createBuilding(@RequestParam(required = false) Long workspaceUserId, @Valid @RequestBody BuildingRequest request) {
        return ApiResponse.ok("Building created successfully.", app.createBuilding(user(workspaceUserId), request));
    }

    @DeleteMapping("/buildings/{id}")
    ApiResponse<?> deleteBuilding(@RequestParam(required = false) Long workspaceUserId, @PathVariable Long id) {
        app.deleteBuilding(user(workspaceUserId), id);
        return ApiResponse.ok("Building deleted successfully.", null);
    }

    @GetMapping("/halls")
    ApiResponse<?> halls(@RequestParam(required = false) Long workspaceUserId) {
        return ApiResponse.ok("Halls loaded successfully.", app.halls(user(workspaceUserId)));
    }

    @PostMapping("/halls")
    ApiResponse<?> createHall(@RequestParam(required = false) Long workspaceUserId, @Valid @RequestBody HallRequest request) {
        return ApiResponse.ok("Hall created successfully.", app.createHall(user(workspaceUserId), request));
    }

    @DeleteMapping("/halls/{id}")
    ApiResponse<?> deleteHall(@RequestParam(required = false) Long workspaceUserId, @PathVariable Long id) {
        app.deleteHall(user(workspaceUserId), id);
        return ApiResponse.ok("Hall deleted successfully.", null);
    }

    @GetMapping("/students")
    ApiResponse<?> students(@RequestParam(required = false) Long workspaceUserId) {
        return ApiResponse.ok("Students loaded successfully.", app.students(user(workspaceUserId)));
    }

    @PostMapping("/students")
    ApiResponse<?> createStudent(@RequestParam(required = false) Long workspaceUserId, @Valid @RequestBody StudentRequest request) {
        return ApiResponse.ok("Student added successfully.", app.createStudent(user(workspaceUserId), request));
    }

    @DeleteMapping("/students/{id}")
    ApiResponse<?> deleteStudent(@RequestParam(required = false) Long workspaceUserId, @PathVariable Long id) {
        app.deleteStudent(user(workspaceUserId), id);
        return ApiResponse.ok("Student deleted successfully.", null);
    }

    @GetMapping("/invigilators")
    ApiResponse<?> invigilators(@RequestParam(required = false) Long workspaceUserId) {
        return ApiResponse.ok("Invigilators loaded successfully.", app.invigilators(user(workspaceUserId)));
    }

    @PostMapping("/invigilators")
    ApiResponse<?> createInvigilator(@RequestParam(required = false) Long workspaceUserId, @Valid @RequestBody InvigilatorRequest request) {
        return ApiResponse.ok("Invigilator added successfully.", app.createInvigilator(user(workspaceUserId), request));
    }

    @DeleteMapping("/invigilators/{id}")
    ApiResponse<?> deleteInvigilator(@RequestParam(required = false) Long workspaceUserId, @PathVariable Long id) {
        app.deleteInvigilator(user(workspaceUserId), id);
        return ApiResponse.ok("Invigilator deleted successfully.", null);
    }

    @GetMapping("/exams")
    ApiResponse<?> exams(@RequestParam(required = false) Long workspaceUserId) {
        return ApiResponse.ok("Exams loaded successfully.", app.exams(user(workspaceUserId)));
    }

    @PostMapping("/exams")
    ApiResponse<?> createExam(@RequestParam(required = false) Long workspaceUserId, @Valid @RequestBody ExamRequest request) {
        return ApiResponse.ok("Exam created successfully.", app.createExam(user(workspaceUserId), request));
    }

    @DeleteMapping("/exams/{id}")
    ApiResponse<?> deleteExam(@RequestParam(required = false) Long workspaceUserId, @PathVariable Long id) {
        app.deleteExam(user(workspaceUserId), id);
        return ApiResponse.ok("Exam deleted successfully.", null);
    }

    // ── Delete All Endpoints ──

    @PostMapping("/halls/delete-all")
    ApiResponse<?> deleteAllHalls(@RequestParam(required = false) Long workspaceUserId) {
        app.deleteAllHalls(user(workspaceUserId));
        return ApiResponse.ok("All halls deleted successfully.", null);
    }

    @PostMapping("/students/delete-all")
    ApiResponse<?> deleteAllStudents(@RequestParam(required = false) Long workspaceUserId) {
        app.deleteAllStudents(user(workspaceUserId));
        return ApiResponse.ok("All students deleted successfully.", null);
    }

    @PostMapping("/invigilators/delete-all")
    ApiResponse<?> deleteAllInvigilators(@RequestParam(required = false) Long workspaceUserId) {
        app.deleteAllInvigilators(user(workspaceUserId));
        return ApiResponse.ok("All invigilators deleted successfully.", null);
    }

    @PostMapping("/exams/delete-all")
    ApiResponse<?> deleteAllExams(@RequestParam(required = false) Long workspaceUserId) {
        app.deleteAllExams(user(workspaceUserId));
        return ApiResponse.ok("All exams deleted successfully.", null);
    }

    @PostMapping("/buildings/delete-all")
    ApiResponse<?> deleteAllBuildings(@RequestParam(required = false) Long workspaceUserId) {
        app.deleteAllBuildings(user(workspaceUserId));
        return ApiResponse.ok("All buildings deleted successfully.", null);
    }

    @GetMapping("/allocation/{examId}/dry-run")
    ApiResponse<?> dryRun(@RequestParam(required = false) Long workspaceUserId, @PathVariable Long examId,
                          @RequestParam(defaultValue = "STRICT") AllocationMode mode) {
        return ApiResponse.ok("Dry run completed successfully.", app.dryRun(user(workspaceUserId), examId, mode));
    }

    @GetMapping("/allocation/{examId}/status")
    ApiResponse<?> allocationStatus(@RequestParam(required = false) Long workspaceUserId, @PathVariable Long examId) {
        return ApiResponse.ok("Allocation status loaded successfully.", app.allocationStatus(user(workspaceUserId), examId));
    }

    @DeleteMapping("/allocation/{examId}")
    ApiResponse<?> deleteAllocation(@RequestParam(required = false) Long workspaceUserId, @PathVariable Long examId) {
        app.deleteAllocation(user(workspaceUserId), examId);
        return ApiResponse.ok("Allocation deleted successfully. You can now generate a fresh allocation.", null);
    }

    @PostMapping("/allocation/{examId}/generate")
    ApiResponse<?> generate(@RequestParam(required = false) Long workspaceUserId, @PathVariable Long examId,
                            @RequestParam(defaultValue = "false") boolean replaceExisting,
                            @RequestParam(defaultValue = "STRICT") AllocationMode mode) {
        return ApiResponse.ok("Allocation generated successfully.", app.generateAllocation(user(workspaceUserId), examId, replaceExisting, mode));
    }

    @GetMapping("/allocation/{examId}/students")
    ApiResponse<?> allocationReport(@RequestParam(required = false) Long workspaceUserId, @PathVariable Long examId,
                                    @RequestParam(required = false) Long buildingId,
                                    @RequestParam(required = false) Long hallId) {
        return ApiResponse.ok("Allocation report loaded successfully.",
                app.allocationReport(user(workspaceUserId), examId, buildingId, hallId));
    }

    @GetMapping(value = "/allocation/{examId}/students.csv", produces = "text/csv")
    ResponseEntity<String> allocationCsv(@RequestParam(required = false) Long workspaceUserId, @PathVariable Long examId,
                                         @RequestParam(required = false) Long buildingId,
                                         @RequestParam(required = false) Long hallId) {
        String filename = app.allocationCsvFilename(user(workspaceUserId), examId, buildingId, hallId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .body(app.allocationCsv(user(workspaceUserId), examId, buildingId, hallId));
    }

    @GetMapping("/allocation/{examId}/invigilators")
    ApiResponse<?> invigilatorReport(@RequestParam(required = false) Long workspaceUserId, @PathVariable Long examId) {
        return ApiResponse.ok("Invigilator report loaded successfully.", app.invigilatorReport(user(workspaceUserId), examId));
    }

    @GetMapping("/seat")
    ApiResponse<?> seat(@RequestParam(required = false) Long workspaceUserId, @RequestParam Long examId, @RequestParam String hallTicket) {
        return ApiResponse.ok("Seat loaded successfully.", app.lookupSeat(user(workspaceUserId), examId, hallTicket));
    }

    @GetMapping("/analytics/buildings")
    ApiResponse<?> buildingAnalytics(@RequestParam(required = false) Long workspaceUserId) {
        return ApiResponse.ok("Building analytics loaded successfully.", app.buildingAnalytics(user(workspaceUserId)));
    }

    @GetMapping("/visualizer/halls/{hallId}")
    ApiResponse<?> hallVisualizer(@RequestParam(required = false) Long workspaceUserId, @PathVariable Long hallId) {
        return ApiResponse.ok("Hall visualizer loaded successfully.", app.seats(user(workspaceUserId), hallId));
    }

    @GetMapping("/complaints")
    ApiResponse<?> complaints(@RequestParam(required = false) Long workspaceUserId) {
        return ApiResponse.ok("Complaints loaded successfully.", app.complaints(user(workspaceUserId)));
    }

    @PostMapping("/complaints")
    ApiResponse<?> createComplaint(@RequestParam(required = false) Long workspaceUserId, @Valid @RequestBody ComplaintRequest request) {
        return ApiResponse.ok("Complaint created successfully.", app.createComplaint(user(workspaceUserId), request));
    }

    @DeleteMapping("/complaints/{id}")
    ApiResponse<?> deleteComplaint(@RequestParam(required = false) Long workspaceUserId, @PathVariable Long id) {
        app.deleteComplaint(user(workspaceUserId), id);
        return ApiResponse.ok("Complaint deleted successfully.", null);
    }

    @GetMapping("/audit")
    ApiResponse<?> audit(@RequestParam(required = false) Long workspaceUserId) {
        return ApiResponse.ok("Audit history loaded successfully.", audits.list(user(workspaceUserId)));
    }

    // ── Import (validate all rows first; reject entire file on any error) ──

    @PostMapping(value = "/import/buildings", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<?> importBuildings(@RequestParam(required = false) Long workspaceUserId, @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return handleImport(() -> csv.importBuildings(user(workspaceUserId), file));
    }

    @PostMapping(value = "/import/halls", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<?> importHalls(@RequestParam(required = false) Long workspaceUserId, @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return handleImport(() -> csv.importHalls(user(workspaceUserId), file));
    }

    @PostMapping(value = "/import/students", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<?> importStudents(@RequestParam(required = false) Long workspaceUserId, @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return handleImport(() -> csv.importStudents(user(workspaceUserId), file));
    }

    @PostMapping(value = "/import/invigilators", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<?> importInvigilators(@RequestParam(required = false) Long workspaceUserId, @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return handleImport(() -> csv.importInvigilators(user(workspaceUserId), file));
    }

    @GetMapping(value = "/import/templates/buildings.csv", produces = "text/csv")
    ResponseEntity<String> templateBuildings() {
        return csvAttachment("buildings-template.csv", csv.templateBuildings());
    }

    @GetMapping(value = "/import/templates/halls.csv", produces = "text/csv")
    ResponseEntity<String> templateHalls() {
        return csvAttachment("halls-template.csv", csv.templateHalls());
    }

    @GetMapping(value = "/import/templates/students.csv", produces = "text/csv")
    ResponseEntity<String> templateStudents() {
        return csvAttachment("students-template.csv", csv.templateStudents());
    }

    @GetMapping(value = "/import/templates/invigilators.csv", produces = "text/csv")
    ResponseEntity<String> templateInvigilators() {
        return csvAttachment("invigilators-template.csv", csv.templateInvigilators());
    }

    // ── Export ──

    @GetMapping(value = "/export/buildings.csv", produces = "text/csv")
    ResponseEntity<String> exportBuildings(@RequestParam(required = false) Long workspaceUserId) {
        AppUser u = user(workspaceUserId);
        log.info("User '{}' requested buildings export.", u == null ? "anonymous" : u.getUsername());
        String body = csv.exportBuildings(u);
        log.info("Returning buildings export ({} bytes) to user '{}'.", body == null ? 0 : body.length(), u == null ? "anonymous" : u.getUsername());
        return csvAttachment("buildings-export.csv", body);
    }

    @GetMapping(value = "/export/halls.csv", produces = "text/csv")
    ResponseEntity<String> exportHalls(@RequestParam(required = false) Long workspaceUserId) {
        AppUser u = user(workspaceUserId);
        log.info("User '{}' requested halls export.", u == null ? "anonymous" : u.getUsername());
        String body = csv.exportHalls(u);
        log.info("Returning halls export ({} bytes) to user '{}'.", body == null ? 0 : body.length(), u == null ? "anonymous" : u.getUsername());
        return csvAttachment("halls-export.csv", body);
    }

    @GetMapping(value = "/export/students.csv", produces = "text/csv")
    ResponseEntity<String> exportStudents(@RequestParam(required = false) Long workspaceUserId) {
        AppUser u = user(workspaceUserId);
        log.info("User '{}' requested students export.", u == null ? "anonymous" : u.getUsername());
        String body = csv.exportStudents(u);
        log.info("Returning students export ({} bytes) to user '{}'.", body == null ? 0 : body.length(), u == null ? "anonymous" : u.getUsername());
        return csvAttachment("students-export.csv", body);
    }

    @GetMapping(value = "/export/overall.csv", produces = "text/csv")
    ResponseEntity<String> exportOverall(@RequestParam(required = false) Long workspaceUserId) {
        AppUser u = user(workspaceUserId);
        log.info("User '{}' requested overall export.", u == null ? "anonymous" : u.getUsername());
        String body = csv.exportOverall(u);
        log.info("Returning overall export ({} bytes) to user '{}'.", body == null ? 0 : body.length(), u == null ? "anonymous" : u.getUsername());
        return csvAttachment("exammaster-overall-export.csv", body);
    }

    @GetMapping("/export/summary")
    ApiResponse<?> exportSummary(@RequestParam(required = false) Long workspaceUserId) {
        return ApiResponse.ok("Export summary loaded successfully.", csv.exportSummary(user(workspaceUserId)));
    }

    private ResponseEntity<String> csvAttachment(String filename, String body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(body);
    }
}
