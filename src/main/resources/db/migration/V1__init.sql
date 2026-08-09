CREATE TABLE app_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    college_name VARCHAR(255) NOT NULL,
    username VARCHAR(120) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(40) NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT,
    CONSTRAINT uk_user_email UNIQUE (email),
    CONSTRAINT uk_user_username UNIQUE (username)
);

CREATE TABLE buildings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    building_name VARCHAR(255) NOT NULL,
    max_hall_count INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT,
    CONSTRAINT fk_building_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    CONSTRAINT uk_building_user_name UNIQUE (user_id, building_name)
);

CREATE TABLE halls (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    building_id BIGINT NOT NULL,
    hall_name VARCHAR(255) NOT NULL,
    capacity INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT,
    CONSTRAINT fk_hall_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_hall_building FOREIGN KEY (building_id) REFERENCES buildings(id) ON DELETE CASCADE,
    CONSTRAINT uk_hall_user_name UNIQUE (user_id, hall_name),
    CONSTRAINT ck_hall_capacity CHECK (capacity > 0)
);

CREATE TABLE students (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    hall_ticket_number VARCHAR(120) NOT NULL,
    student_name VARCHAR(255) NOT NULL,
    branch VARCHAR(120) NOT NULL,
    student_year VARCHAR(40) NOT NULL,
    semester VARCHAR(40) NOT NULL,
    section VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT,
    CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    CONSTRAINT uk_student_user_ticket UNIQUE (user_id, hall_ticket_number)
);

CREATE TABLE invigilators (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    invigilator_id VARCHAR(120) NOT NULL,
    invigilator_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT,
    CONSTRAINT fk_invigilator_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    CONSTRAINT uk_invigilator_user_code UNIQUE (user_id, invigilator_id)
);

CREATE TABLE exams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    exam_name VARCHAR(255) NOT NULL,
    academic_year VARCHAR(80) NOT NULL,
    semester VARCHAR(40) NOT NULL,
    exam_type VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT,
    CONSTRAINT fk_exam_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    CONSTRAINT uk_exam_user_scope UNIQUE (user_id, exam_name, semester, exam_type)
);

CREATE TABLE allocations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    building_id BIGINT NOT NULL,
    hall_id BIGINT NOT NULL,
    seat_number VARCHAR(20) NOT NULL,
    allocated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_allocation_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_allocation_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
    CONSTRAINT fk_allocation_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_allocation_building FOREIGN KEY (building_id) REFERENCES buildings(id) ON DELETE CASCADE,
    CONSTRAINT fk_allocation_hall FOREIGN KEY (hall_id) REFERENCES halls(id) ON DELETE CASCADE,
    CONSTRAINT uk_allocation_student_exam UNIQUE (user_id, exam_id, student_id),
    CONSTRAINT uk_allocation_seat_exam UNIQUE (user_id, exam_id, hall_id, seat_number)
);

CREATE TABLE invigilator_allocations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    invigilator_id_ref BIGINT NOT NULL,
    building_id BIGINT NOT NULL,
    hall_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_invig_allocation_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_invig_allocation_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
    CONSTRAINT fk_invig_allocation_invigilator FOREIGN KEY (invigilator_id_ref) REFERENCES invigilators(id) ON DELETE CASCADE,
    CONSTRAINT fk_invig_allocation_building FOREIGN KEY (building_id) REFERENCES buildings(id) ON DELETE CASCADE,
    CONSTRAINT fk_invig_allocation_hall FOREIGN KEY (hall_id) REFERENCES halls(id) ON DELETE CASCADE,
    CONSTRAINT uk_invig_hall_exam UNIQUE (user_id, exam_id, hall_id)
);

CREATE TABLE complaints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(3000) NOT NULL,
    category VARCHAR(120) NOT NULL,
    email VARCHAR(255) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT,
    CONSTRAINT fk_complaint_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
);

CREATE TABLE audit_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    action VARCHAR(120) NOT NULL,
    module VARCHAR(120) NOT NULL,
    description VARCHAR(1500) NOT NULL,
    performed_by VARCHAR(120) NOT NULL,
    performed_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
);

CREATE INDEX idx_buildings_user ON buildings(user_id);
CREATE INDEX idx_halls_user ON halls(user_id);
CREATE INDEX idx_students_user ON students(user_id);
CREATE INDEX idx_invigilators_user ON invigilators(user_id);
CREATE INDEX idx_exams_user ON exams(user_id);
CREATE INDEX idx_allocations_exam ON allocations(exam_id);
CREATE INDEX idx_allocations_student ON allocations(student_id);
CREATE INDEX idx_audit_user ON audit_records(user_id);
