package com.exammaster.exammaster_pro.service;

import com.exammaster.exammaster_pro.dto.Responses.*;
import com.exammaster.exammaster_pro.entity.*;
import com.exammaster.exammaster_pro.exception.BusinessValidationException;
import com.exammaster.exammaster_pro.repository.*;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CsvDataService {
    private static final Logger log = LoggerFactory.getLogger(CsvDataService.class);
    private static final String[] BUILDING_HEADERS = {"buildingName", "maxHallCount"};
    private static final String[] HALL_HEADERS = {"buildingName", "hallName", "benchCount", "studentsPerBench"};
    private static final String[] STUDENT_HEADERS = {"hallTicketNumber", "studentName", "branch", "year", "semester", "section"};
    private static final String[] INVIGILATOR_HEADERS = {"invigilatorId", "invigilatorName"};

    private final BuildingRepository buildings;
    private final HallRepository halls;
    private final StudentRepository students;
    private final InvigilatorRepository invigilators;
    private final ExamRepository exams;
    private final AllocationRepository allocations;
    private final AuditService audits;

    public ImportResultResponse importBuildings(AppUser user, MultipartFile file) {
        ParsedCsv parsed = parse(file, BUILDING_HEADERS);
        if (parsed.errors() != null) return parsed.errors();

        List<ImportRowError> errors = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();
        List<BuildingRequestRow> validRows = new ArrayList<>();

        for (int i = 0; i < parsed.rows().size(); i++) {
            int rowNum = i + 2;
            Map<String, String> row = parsed.rows().get(i);
            String name = clean(row.get("buildingName"));
            String maxRaw = clean(row.get("maxHallCount"));

            if (name.isEmpty()) {
                errors.add(err(rowNum, "buildingName", maxRaw, "Building name is required."));
                continue;
            }
            if (name.length() < 2 || name.length() > 100) {
                errors.add(err(rowNum, "buildingName", name, "Building name must be 2-100 characters."));
                continue;
            }
            String nameKey = name.toUpperCase(Locale.ROOT);
            if (!seenNames.add(nameKey)) {
                errors.add(err(rowNum, "buildingName", name, "Duplicate building name in file."));
                continue;
            }
            if (buildings.existsByUserAndBuildingNameIgnoreCase(user, name)) {
                errors.add(err(rowNum, "buildingName", name, "Building already exists in your workspace."));
                continue;
            }
            Integer maxHallCount = parsePositiveInt(maxRaw);
            if (maxHallCount == null) {
                errors.add(err(rowNum, "maxHallCount", maxRaw, "Maximum halls must be a positive number."));
                continue;
            }
            validRows.add(new BuildingRequestRow(name, maxHallCount));
        }

        if (!errors.isEmpty()) {
            return reject(parsed.rows().size(), errors);
        }
        if (validRows.isEmpty()) {
            return reject(0, List.of(err(1, "file", "", "No data rows found. Add at least one building.")));
        }

        for (BuildingRequestRow row : validRows) {
            Building b = new Building();
            b.setUser(user);
            b.setBuildingName(row.name());
            b.setMaxHallCount(row.maxHallCount());
            buildings.save(b);
        }
        audits.log(user, "Buildings Imported", "Import", validRows.size() + " buildings imported from CSV.", user.getUsername());
        return accept(validRows.size(), validRows.size(), "All buildings imported successfully.");
    }

    public ImportResultResponse importHalls(AppUser user, MultipartFile file) {
        if (buildings.countByUser(user) == 0) {
            return reject(0, List.of(err(1, "file", "", "Import buildings first. No buildings found in your workspace.")));
        }
        ParsedCsv parsed = parse(file, HALL_HEADERS);
        if (parsed.errors() != null) return parsed.errors();

        List<ImportRowError> errors = new ArrayList<>();
        Set<String> seenHallNames = new HashSet<>();
        Map<String, Integer> newHallsPerBuilding = new HashMap<>();
        List<HallRequestRow> validRows = new ArrayList<>();

        for (int i = 0; i < parsed.rows().size(); i++) {
            int rowNum = i + 2;
            Map<String, String> row = parsed.rows().get(i);
            String buildingName = clean(row.get("buildingName"));
            String hallName = clean(row.get("hallName"));
            String benchRaw = clean(row.get("benchCount"));
            String perBenchRaw = clean(row.get("studentsPerBench"));

            if (buildingName.isEmpty()) {
                errors.add(err(rowNum, "buildingName", buildingName, "Building name is required."));
                continue;
            }
            if (hallName.isEmpty()) {
                errors.add(err(rowNum, "hallName", hallName, "Hall name is required."));
                continue;
            }
            if (!hallName.matches("^[A-Za-z0-9._-]{1,50}$")) {
                errors.add(err(rowNum, "hallName", hallName,
                        "Hall name must be 1-50 characters using letters, numbers, dot, underscore, or hyphen; spaces are not allowed."));
                continue;
            }
            Optional<Building> buildingOpt = buildings.findByUserAndBuildingNameIgnoreCase(user, buildingName);
            if (buildingOpt.isEmpty()) {
                errors.add(err(rowNum, "buildingName", buildingName, "Building not found. Import buildings first or fix the name."));
                continue;
            }
            Building building = buildingOpt.get();
            String hallKey = building.getBuildingName().toUpperCase(Locale.ROOT) + "|" + hallName.toUpperCase(Locale.ROOT);
            if (!seenHallNames.add(hallKey)) {
                errors.add(err(rowNum, "hallName", hallName, "Duplicate hall name in file for this building."));
                continue;
            }

            if (halls.existsByUserAndBuildingAndHallNameIgnoreCase(user, building, hallName)) {
                errors.add(err(rowNum, "hallName", hallName, "Hall already exists in the specified building in your workspace."));
                continue;
            }

            Integer benchCount = parsePositiveInt(benchRaw);
            if (benchCount == null) {
                errors.add(err(rowNum, "benchCount", benchRaw, "Bench count must be a positive whole number."));
                continue;
            }
            if (benchCount > 500) {
                errors.add(err(rowNum, "benchCount", benchRaw, "Bench count cannot exceed 500."));
                continue;
            }
            Integer studentsPerBench = parseStudentsPerBench(perBenchRaw);
            if (studentsPerBench == null) {
                errors.add(err(rowNum, "studentsPerBench", perBenchRaw, "Students per bench must be 1 or 2."));
                continue;
            }

            String buildingKey = building.getBuildingName().toUpperCase(Locale.ROOT);
            int planned = newHallsPerBuilding.getOrDefault(buildingKey, 0) + 1;
            long existing = halls.countByUserAndBuilding(user, building);
            if (existing + planned > building.getMaxHallCount()) {
                errors.add(err(rowNum, "buildingName", buildingName,
                        "Hall limit reached for " + building.getBuildingName() + " (max " + building.getMaxHallCount() + ")."));
                continue;
            }
            newHallsPerBuilding.put(buildingKey, planned);
            validRows.add(new HallRequestRow(building, hallName, benchCount, studentsPerBench));
        }

        if (!errors.isEmpty()) {
            return reject(parsed.rows().size(), errors);
        }
        if (validRows.isEmpty()) {
            return reject(0, List.of(err(1, "file", "", "No data rows found. Add at least one hall.")));
        }

        for (HallRequestRow row : validRows) {
            Hall h = new Hall();
            h.setUser(user);
            h.setBuilding(row.building());
            h.setHallName(row.hallName());
            h.setBenchCount(row.benchCount());
            h.setStudentsPerBench(row.studentsPerBench());
            h.setCapacity(row.benchCount() * row.studentsPerBench());
            halls.save(h);
        }
        audits.log(user, "Halls Imported", "Import", validRows.size() + " halls imported from CSV.", user.getUsername());
        return accept(validRows.size(), validRows.size(), "All halls imported successfully.");
    }

    public ImportResultResponse importStudents(AppUser user, MultipartFile file) {
        if (halls.countByUser(user) == 0) {
            return reject(0, List.of(err(1, "file", "", "Import halls first. No halls found in your workspace.")));
        }

        ParsedCsv parsed = parse(file, STUDENT_HEADERS);
        if (parsed.errors() != null) return parsed.errors();

        List<ImportRowError> errors = new ArrayList<>();
        Set<String> seenTickets = new HashSet<>();
        List<StudentRequestRow> validRows = new ArrayList<>();

        for (int i = 0; i < parsed.rows().size(); i++) {
            int rowNum = i + 2;
            Map<String, String> row = parsed.rows().get(i);
            String ticket = clean(row.get("hallTicketNumber"));
            String name = clean(row.get("studentName"));
            String branch = clean(row.get("branch"));
            String year = clean(row.get("year"));
            String semester = clean(row.get("semester"));
            String section = clean(row.get("section"));

            if (ticket.isEmpty()) {
                errors.add(err(rowNum, "hallTicketNumber", ticket, "Hall ticket number is required."));
                continue;
            }
            if (!ticket.matches("^[A-Z0-9]{3,20}$")) {
                errors.add(err(rowNum, "hallTicketNumber", ticket,
                        "Hall ticket must be 3-20 uppercase letters or digits only."));
                continue;
            }
            String ticketKey = ticket.toUpperCase(Locale.ROOT);
            if (!seenTickets.add(ticketKey)) {
                errors.add(err(rowNum, "hallTicketNumber", ticket, "Duplicate hall ticket in file."));
                continue;
            }
            if (students.existsByUserAndHallTicketNumberIgnoreCase(user, ticket)) {
                errors.add(err(rowNum, "hallTicketNumber", ticket, "Hall ticket already exists in your workspace."));
                continue;
            }
            if (name.isEmpty()) {
                errors.add(err(rowNum, "studentName", name, "Student name is required."));
                continue;
            }
            if (name.length() < 2 || name.length() > 100) {
                errors.add(err(rowNum, "studentName", name, "Student name must be 2-100 characters."));
                continue;
            }
            if (!name.matches("^[a-zA-Z\\s.'-]+$")) {
                errors.add(err(rowNum, "studentName", name, "Student name can only contain letters, spaces, dots, hyphens, and apostrophes."));
                continue;
            }
            if (branch.isEmpty()) {
                errors.add(err(rowNum, "branch", branch, "Branch is required."));
                continue;
            }
            if (!branch.matches("^[A-Z0-9]{2,10}$")) {
                errors.add(err(rowNum, "branch", branch, "Branch must be 2-10 uppercase letters or digits."));
                continue;
            }
            if (year.isEmpty()) {
                errors.add(err(rowNum, "year", year, "Year is required."));
                continue;
            }
            if (!year.matches("^[1-4]$")) {
                errors.add(err(rowNum, "year", year, "Year must be 1, 2, 3, or 4."));
                continue;
            }
            if (semester.isEmpty()) {
                errors.add(err(rowNum, "semester", semester, "Semester is required."));
                continue;
            }
            if (!semester.matches("^[1-8]$")) {
                errors.add(err(rowNum, "semester", semester, "Semester must be 1-8."));
                continue;
            }
            if (section.isEmpty()) {
                errors.add(err(rowNum, "section", section, "Section is required."));
                continue;
            }
            if (!section.matches("^[A-Z]$")) {
                errors.add(err(rowNum, "section", section, "Section must be a single uppercase letter."));
                continue;
            }
            validRows.add(new StudentRequestRow(ticket, name, branch, year, semester, section));
        }

        if (!errors.isEmpty()) {
            return reject(parsed.rows().size(), errors);
        }
        if (validRows.isEmpty()) {
            return reject(0, List.of(err(1, "file", "", "No data rows found. Add at least one student.")));
        }

        for (StudentRequestRow row : validRows) {
            Student s = new Student();
            s.setUser(user);
            s.setHallTicketNumber(row.ticket());
            s.setStudentName(row.name());
            s.setBranch(row.branch());
            s.setYear(row.year());
            s.setSemester(row.semester());
            s.setSection(row.section());
            students.save(s);
        }
        audits.log(user, "Students Imported", "Import", validRows.size() + " students imported from CSV.", user.getUsername());
        return accept(validRows.size(), validRows.size(), "All students imported successfully.");
    }

    public ImportResultResponse importInvigilators(AppUser user, MultipartFile file) {
        ParsedCsv parsed = parse(file, INVIGILATOR_HEADERS);
        if (parsed.errors() != null) return parsed.errors();

        List<ImportRowError> errors = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        List<InvigilatorRequestRow> validRows = new ArrayList<>();

        for (int i = 0; i < parsed.rows().size(); i++) {
            int rowNum = i + 2;
            Map<String, String> row = parsed.rows().get(i);
            String invigilatorId = clean(row.get("invigilatorId"));
            String invigilatorName = clean(row.get("invigilatorName"));

            if (invigilatorId.isEmpty()) {
                errors.add(err(rowNum, "invigilatorId", invigilatorId, "Invigilator ID is required."));
                continue;
            }
            if (!invigilatorId.matches("^[A-Z0-9]{2,20}$")) {
                errors.add(err(rowNum, "invigilatorId", invigilatorId,
                        "Invigilator ID must be 2-20 uppercase letters or digits only."));
                continue;
            }
            String idKey = invigilatorId.toUpperCase(Locale.ROOT);
            if (!seenIds.add(idKey)) {
                errors.add(err(rowNum, "invigilatorId", invigilatorId, "Duplicate invigilator ID in file."));
                continue;
            }
            if (invigilators.existsByUserAndInvigilatorIdIgnoreCase(user, invigilatorId)) {
                errors.add(err(rowNum, "invigilatorId", invigilatorId, "Invigilator ID already exists in your workspace."));
                continue;
            }
            if (invigilatorName.isEmpty()) {
                errors.add(err(rowNum, "invigilatorName", invigilatorName, "Invigilator name is required."));
                continue;
            }
            if (invigilatorName.length() < 2 || invigilatorName.length() > 100) {
                errors.add(err(rowNum, "invigilatorName", invigilatorName, "Invigilator name must be 2-100 characters."));
                continue;
            }
            validRows.add(new InvigilatorRequestRow(invigilatorId, invigilatorName));
        }

        if (!errors.isEmpty()) {
            return reject(parsed.rows().size(), errors);
        }
        if (validRows.isEmpty()) {
            return reject(0, List.of(err(1, "file", "", "No data rows found. Add at least one invigilator.")));
        }

        for (InvigilatorRequestRow row : validRows) {
            Invigilator inv = new Invigilator();
            inv.setUser(user);
            inv.setInvigilatorId(row.invigilatorId());
            inv.setInvigilatorName(row.invigilatorName());
            invigilators.save(inv);
        }
        audits.log(user, "Invigilators Imported", "Import", validRows.size() + " invigilators imported from CSV.", user.getUsername());
        return accept(validRows.size(), validRows.size(), "All invigilators imported successfully.");
    }

    @Transactional(readOnly = true)
    public String exportBuildings(AppUser user) {
        return writeCsv(BUILDING_HEADERS, buildings.findByUserOrderByBuildingName(user).stream()
                .map(b -> new String[]{b.getBuildingName(), String.valueOf(b.getMaxHallCount())})
                .toList());
    }

    @Transactional(readOnly = true)
    public String exportHalls(AppUser user) {
        return writeCsv(HALL_HEADERS, halls.findByUserOrderByHallName(user).stream()
                .map(h -> new String[]{
                        h.getBuilding().getBuildingName(),
                        h.getHallName(),
                        String.valueOf(h.getBenchCount()),
                        String.valueOf(h.getStudentsPerBench())
                })
                .toList());
    }

    @Transactional(readOnly = true)
    public String exportStudents(AppUser user) {
        return writeCsv(STUDENT_HEADERS, students.findByUserOrderByHallTicketNumber(user).stream()
                .map(s -> new String[]{
                        s.getHallTicketNumber(), s.getStudentName(), s.getBranch(),
                        s.getYear(), s.getSemester(), s.getSection()
                })
                .toList());
    }

    @Transactional(readOnly = true)
    public String exportOverall(AppUser user) {
        log.info("Generating allocation-style export for user: {}", user == null ? "anonymous" : user.getUsername());
        StringBuilder sb = new StringBuilder();
        sb.append("ExamMaster Pro\n");
        sb.append("College: ").append(user.getCollegeName() == null ? "" : user.getCollegeName()).append("\n");

        // Pick the most recent exam for this user (if any)
        var examsList = exams.findByUserOrderByCreatedAtDesc(user);
        if (examsList == null || examsList.isEmpty()) {
            sb.append("Exam: \n");
            sb.append("Student\tTicket\tBuilding\tHall\tSeat\n");
            return sb.toString();
        }
        var exam = examsList.get(0);
        sb.append("Exam: ").append(exam.getExamName() == null ? "" : exam.getExamName()).append("\n\n");

        sb.append("Student\tTicket\tBuilding\tHall\tSeat\n");
        var rows = allocations.findByUserAndExamOrderByHallHallNameAscSeatNumberAsc(user, exam);
        if (rows == null || rows.isEmpty()) {
            log.info("No allocations found for exam '{}' (user={})", exam.getExamName(), user.getUsername());
            return sb.toString();
        }
        log.info("Found {} allocation rows for exam '{}' (user={})", rows.size(), exam.getExamName(), user.getUsername());
        for (var a : rows) {
            String studentName = a.getStudent() == null ? "" : (a.getStudent().getStudentName() == null ? "" : a.getStudent().getStudentName());
            String ticket = a.getStudent() == null ? "" : (a.getStudent().getHallTicketNumber() == null ? "" : a.getStudent().getHallTicketNumber());
            String buildingName = a.getBuilding() == null ? "" : (a.getBuilding().getBuildingName() == null ? "" : a.getBuilding().getBuildingName());
            String hallName = a.getHall() == null ? "" : (a.getHall().getHallName() == null ? "" : a.getHall().getHallName());
            String seat = a.getSeatNumber() == null ? "" : a.getSeatNumber();
            sb.append(studentName).append('\t')
                    .append(ticket).append('\t')
                    .append(buildingName).append('\t')
                    .append(hallName).append('\t')
                    .append(seat).append('\n');
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public ExportSummaryResponse exportSummary(AppUser user) {
        long b = buildings.countByUser(user);
        long h = halls.countByUser(user);
        long s = students.countByUser(user);
        long inv = invigilators.countByUser(user);
        long ex = exams.countByUser(user);
        int capacity = halls.findByUserOrderByHallName(user).stream().mapToInt(Hall::getCapacity).sum();
        String summary = "📋 *ExamMaster Pro Export*\n"
                + "🏫 College: " + user.getCollegeName() + "\n"
                + "🏢 Buildings: " + b + "\n"
                + "🚪 Halls: " + h + " (" + capacity + " seats)\n"
                + "👨‍🎓 Students: " + s + "\n"
                + "👮 Invigilators: " + inv + "\n"
                + "📝 Exams: " + ex + "\n"
                + "— Exported from ExamMaster Pro";
        return new ExportSummaryResponse(b, h, s, inv, ex, summary);
    }

    public String templateBuildings() {
        return writeCsv(BUILDING_HEADERS, List.of(
                new String[]{"Main Block", "10"},
                new String[]{"Science Block", "5"}
        ));
    }

    public String templateHalls() {
        return writeCsv(HALL_HEADERS, List.of(
                new String[]{"Main Block", "Hall A", "30", "2"},
                new String[]{"Main Block", "Hall B", "25", "1"},
                new String[]{"Science Block", "Hall C", "20", "2"}
        ));
    }

    public String templateStudents() {
        return writeCsv(STUDENT_HEADERS, List.of(
                new String[]{"HT001", "John Doe", "CSE", "3", "6", "A"},
                new String[]{"HT002", "Jane Smith", "ECE", "3", "6", "B"},
                new String[]{"HT003", "Alex Kumar", "CSE", "3", "6", "A"}
        ));
    }

    public String templateInvigilators() {
        return writeCsv(INVIGILATOR_HEADERS, List.of(
                new String[]{"INV001", "John Doe"},
                new String[]{"INV002", "Jane Smith"}
        ));
    }

    private ParsedCsv parse(MultipartFile file, String[] expectedHeaders) {
        if (file == null || file.isEmpty()) {
            return new ParsedCsv(null, reject(0, List.of(err(1, "file", "", "Please choose a CSV file to upload."))));
        }
        long maxBytes = 10L * 1024 * 1024; // 10MB
        if (file.getSize() > maxBytes) {
            return new ParsedCsv(null, reject(0, List.of(err(1, "file", file.getOriginalFilename() == null ? "" : file.getOriginalFilename(), "File too large. Maximum allowed size is 10MB."))));
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".csv")) {
            return new ParsedCsv(null, reject(0, List.of(err(1, "file", filename, "Only .csv files are accepted."))));
        }

        try {
            String raw = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (raw.isBlank()) {
                return new ParsedCsv(null, reject(0, List.of(err(1, "file", "", "CSV file is empty."))));
            }
            char separator = detectSeparator(raw);
            CSVParser parser = new CSVParserBuilder().withSeparator(separator).build();
            try (CSVReader reader = new CSVReaderBuilder(new StringReader(raw)).withCSVParser(parser).build()) {
                List<String[]> all = reader.readAll();
                if (all.isEmpty()) {
                    return new ParsedCsv(null, reject(0, List.of(err(1, "file", "", "CSV file is empty."))));
                }
                int maxRows = 50000;
                if (all.size() - 1 > maxRows) {
                    return new ParsedCsv(null, reject(maxRows, List.of(err(1, "file", "", "CSV file has too many rows. Maximum " + maxRows + " data rows allowed."))));
                }
                String[] headers = normalizeHeaders(all.get(0));
                List<ImportRowError> headerErrors = validateHeaders(headers, expectedHeaders);
                if (!headerErrors.isEmpty()) {
                    return new ParsedCsv(null, reject(Math.max(0, all.size() - 1), headerErrors));
                }

                Map<String, Integer> index = headerIndex(headers);
                List<Map<String, String>> rows = new ArrayList<>();
                for (int i = 1; i < all.size(); i++) {
                    String[] cells = all.get(i);
                    if (isBlankRow(cells)) continue;
                    Map<String, String> row = new LinkedHashMap<>();
                    for (String h : expectedHeaders) {
                        Integer idx = index.get(h.toLowerCase(Locale.ROOT));
                        row.put(h, idx != null && idx < cells.length ? cells[idx] : "");
                    }
                    rows.add(row);
                }
                // final safety: trim values
                for (Map<String, String> r : rows) {
                    for (Map.Entry<String, String> e : new ArrayList<>(r.entrySet())) {
                        r.put(e.getKey(), clean(e.getValue()));
                    }
                }
                return new ParsedCsv(rows, null);
            }
        } catch (IOException | CsvException ex) {
            throw new BusinessValidationException("Could not read CSV file. Please check the format and try again.");
        }
    }

    private List<ImportRowError> validateHeaders(String[] headers, String[] expected) {
        Set<String> expectedSet = Arrays.stream(expected).map(h -> h.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        Set<String> present = Arrays.stream(headers).map(h -> h.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        List<ImportRowError> errors = new ArrayList<>();
        for (String col : expected) {
            if (!present.contains(col.toLowerCase(Locale.ROOT))) {
                errors.add(err(1, "header", col, "Missing required column: " + col));
            }
        }
        for (String col : present) {
            if (!expectedSet.contains(col)) {
                errors.add(err(1, "header", col, "Unexpected column: " + col + ". Remove it or rename it to match the template."));
            }
        }
        return errors;
    }

    private String[] normalizeHeaders(String[] headers) {
        String[] result = new String[headers.length];
        for (int i = 0; i < headers.length; i++) {
            result[i] = clean(headers[i]).replace("\uFEFF", "");
        }
        return result;
    }

    private Map<String, Integer> headerIndex(String[] headers) {
        Map<String, Integer> result = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            result.put(headers[i].toLowerCase(Locale.ROOT), i);
        }
        return result;
    }

    private boolean isBlankRow(String[] cells) {
        if (cells == null || cells.length == 0) return true;
        for (String c : cells) {
            if (c != null && !c.trim().isEmpty()) return false;
        }
        return true;
    }

    private char detectSeparator(String raw) {
        String firstLine = raw.split("\r\n|\r|\n", -1)[0];
        if (firstLine.contains(";") && !firstLine.contains(",")) return ';';
        if (firstLine.contains("\t") && !firstLine.contains(",")) return '\t';
        return ',';
    }

    private String writeCsv(String[] headers, List<String[]> rows) {
        try (StringWriter sw = new StringWriter(); CSVWriter writer = new CSVWriter(sw)) {
            writer.writeNext(headers);
            for (String[] row : rows) writer.writeNext(row);
            return sw.toString();
        } catch (IOException ex) {
            throw new BusinessValidationException("Could not generate CSV export.");
        }
    }

    private ImportResultResponse accept(int total, int imported, String message) {
        return new ImportResultResponse(true, message, total, total, imported, List.of());
    }

    private ImportResultResponse reject(int totalRows, List<ImportRowError> errors) {
        return new ImportResultResponse(false,
                "Import rejected. Fix " + errors.size() + " issue(s) below and re-upload your file.",
                totalRows, Math.max(0, totalRows - errors.size()), 0, errors);
    }

    private ImportRowError err(int row, String field, String value, String message) {
        return new ImportRowError(row, field, value == null ? "" : value, message);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private Integer parsePositiveInt(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            int v = Integer.parseInt(raw.trim());
            return v > 0 ? v : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseStudentsPerBench(String raw) {
        Integer v = parsePositiveInt(raw);
        if (v == null || (v != 1 && v != 2)) return null;
        return v;
    }

    private record ParsedCsv(List<Map<String, String>> rows, ImportResultResponse errors) {}
    private record BuildingRequestRow(String name, int maxHallCount) {}
    private record HallRequestRow(Building building, String hallName, int benchCount, int studentsPerBench) {}
    private record StudentRequestRow(String ticket, String name, String branch, String year, String semester, String section) {}
    private record InvigilatorRequestRow(String invigilatorId, String invigilatorName) {}
}
