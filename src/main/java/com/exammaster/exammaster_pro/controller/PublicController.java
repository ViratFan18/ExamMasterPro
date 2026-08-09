package com.exammaster.exammaster_pro.controller;

import com.exammaster.exammaster_pro.dto.ApiResponse;
import com.exammaster.exammaster_pro.service.PublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {
    private final PublicService portal;

    @GetMapping("/colleges")
    ApiResponse<?> colleges() {
        return ApiResponse.ok("Colleges loaded successfully.", portal.colleges());
    }

    @GetMapping("/exams")
    ApiResponse<?> exams(@RequestParam String collegeName) {
        return ApiResponse.ok("Exams loaded successfully.", portal.examsForCollege(collegeName));
    }

    @GetMapping("/seat")
    ApiResponse<?> seat(@RequestParam String collegeName, @RequestParam String hallTicket, @RequestParam Long examId) {
        return ApiResponse.ok("Seat details loaded successfully.", portal.lookupSeat(collegeName, hallTicket, examId));
    }
}
