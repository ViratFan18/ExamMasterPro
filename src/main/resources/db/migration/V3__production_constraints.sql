-- V3__production_constraints.sql
-- Production-grade database enhancements that are compatible with MySQL 8.

-- Add version fields for optimistic locking
SET @stmt = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'allocations' AND column_name = 'version') = 0, 'ALTER TABLE allocations ADD COLUMN version INT NOT NULL DEFAULT 0', 'SELECT 1'); PREPARE stmt FROM @stmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @stmt = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'students' AND column_name = 'version') = 0, 'ALTER TABLE students ADD COLUMN version INT NOT NULL DEFAULT 0', 'SELECT 1'); PREPARE stmt FROM @stmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @stmt = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'exams' AND column_name = 'version') = 0, 'ALTER TABLE exams ADD COLUMN version INT NOT NULL DEFAULT 0', 'SELECT 1'); PREPARE stmt FROM @stmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @stmt = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'halls' AND column_name = 'version') = 0, 'ALTER TABLE halls ADD COLUMN version INT NOT NULL DEFAULT 0', 'SELECT 1'); PREPARE stmt FROM @stmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Add indexes for query performance
SET @stmt = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'students' AND index_name = 'idx_student_user_ticket') = 0, 'CREATE INDEX idx_student_user_ticket ON students(user_id, hall_ticket_number)', 'SELECT 1'); PREPARE stmt FROM @stmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @stmt = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'students' AND index_name = 'idx_student_branch') = 0, 'CREATE INDEX idx_student_branch ON students(user_id, branch)', 'SELECT 1'); PREPARE stmt FROM @stmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @stmt = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'students' AND index_name = 'idx_student_section') = 0, 'CREATE INDEX idx_student_section ON students(user_id, section)', 'SELECT 1'); PREPARE stmt FROM @stmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @stmt = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'allocations' AND index_name = 'idx_allocation_user_exam') = 0, 'CREATE INDEX idx_allocation_user_exam ON allocations(user_id, exam_id)', 'SELECT 1'); PREPARE stmt FROM @stmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @stmt = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'allocations' AND index_name = 'idx_allocation_student') = 0, 'CREATE INDEX idx_allocation_student ON allocations(user_id, student_id)', 'SELECT 1'); PREPARE stmt FROM @stmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @stmt = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'exams' AND index_name = 'idx_exam_user') = 0, 'CREATE INDEX idx_exam_user ON exams(user_id, created_at)', 'SELECT 1'); PREPARE stmt FROM @stmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @stmt = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'halls' AND index_name = 'idx_hall_user_building') = 0, 'CREATE INDEX idx_hall_user_building ON halls(user_id, building_id)', 'SELECT 1'); PREPARE stmt FROM @stmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @stmt = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'complaints' AND index_name = 'idx_complaint_user_created') = 0, 'CREATE INDEX idx_complaint_user_created ON complaints(user_id, created_at)', 'SELECT 1'); PREPARE stmt FROM @stmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Add audit table
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    changes JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_user_entity (user_id, entity_type, created_at),
    FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Add allocation session table for tracking concurrent allocations
CREATE TABLE IF NOT EXISTS allocation_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    session_id VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    total_students INT NOT NULL DEFAULT 0,
    allocated_students INT NOT NULL DEFAULT 0,
    failed_students INT NOT NULL DEFAULT 0,
    error_message TEXT,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    INDEX idx_session_user_exam (user_id, exam_id),
    FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Add capacity planning table
CREATE TABLE IF NOT EXISTS capacity_analysis (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    total_students INT NOT NULL,
    total_capacity INT NOT NULL,
    utilization_percent DECIMAL(5,2),
    is_feasible BOOLEAN NOT NULL,
    analysis_json JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_analysis_user_exam (user_id, exam_id),
    FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
