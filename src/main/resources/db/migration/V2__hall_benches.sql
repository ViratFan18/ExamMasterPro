ALTER TABLE halls ADD COLUMN bench_count INT NOT NULL DEFAULT 1;
ALTER TABLE halls ADD COLUMN students_per_bench INT NOT NULL DEFAULT 1;

UPDATE halls SET bench_count = capacity, students_per_bench = 1;

ALTER TABLE halls ADD CONSTRAINT ck_hall_bench_count CHECK (bench_count > 0);
ALTER TABLE halls ADD CONSTRAINT ck_hall_students_per_bench CHECK (students_per_bench IN (1, 2));
