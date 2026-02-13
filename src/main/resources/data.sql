-- ============================================================
-- Sample Data for N+1 Problem Demonstration
-- ============================================================

-- Departments (5 departments to clearly show the N+1 effect)
INSERT INTO departments (id, name, description) VALUES (1, 'Computer Science', 'Study of computation and information');
INSERT INTO departments (id, name, description) VALUES (2, 'Mathematics', 'Study of numbers, quantities, and shapes');
INSERT INTO departments (id, name, description) VALUES (3, 'Physics', 'Study of matter, energy, and fundamental forces');
INSERT INTO departments (id, name, description) VALUES (4, 'Biology', 'Study of living organisms');
INSERT INTO departments (id, name, description) VALUES (5, 'Chemistry', 'Study of substances and their interactions');

-- Department Details (OneToOne with shared PK)
INSERT INTO department_details (department_id, building, budget, head_of_department) VALUES (1, 'Turing Hall', 5000000.00, 'Dr. Alan Smith');
INSERT INTO department_details (department_id, building, budget, head_of_department) VALUES (2, 'Euler Building', 3000000.00, 'Dr. Maria Garcia');
INSERT INTO department_details (department_id, building, budget, head_of_department) VALUES (3, 'Newton Center', 4500000.00, 'Dr. James Wilson');
INSERT INTO department_details (department_id, building, budget, head_of_department) VALUES (4, 'Darwin Lab', 4000000.00, 'Dr. Sarah Chen');
INSERT INTO department_details (department_id, building, budget, head_of_department) VALUES (5, 'Curie Institute', 3500000.00, 'Dr. Robert Kim');

-- Courses (3-4 per department = ~17 courses)
-- Computer Science
INSERT INTO courses (id, title, description, credits, department_id) VALUES (1, 'Intro to Programming', 'Learn Java fundamentals', 4, 1);
INSERT INTO courses (id, title, description, credits, department_id) VALUES (2, 'Data Structures', 'Arrays, trees, graphs, and hash maps', 4, 1);
INSERT INTO courses (id, title, description, credits, department_id) VALUES (3, 'Database Systems', 'SQL, normalization, and query optimization', 3, 1);
INSERT INTO courses (id, title, description, credits, department_id) VALUES (4, 'Machine Learning', 'Supervised and unsupervised learning algorithms', 4, 1);
-- Mathematics
INSERT INTO courses (id, title, description, credits, department_id) VALUES (5, 'Calculus I', 'Limits, derivatives, and integrals', 4, 2);
INSERT INTO courses (id, title, description, credits, department_id) VALUES (6, 'Linear Algebra', 'Vectors, matrices, and transformations', 3, 2);
INSERT INTO courses (id, title, description, credits, department_id) VALUES (7, 'Statistics', 'Probability and statistical inference', 3, 2);
-- Physics
INSERT INTO courses (id, title, description, credits, department_id) VALUES (8, 'Classical Mechanics', 'Newtonian mechanics and dynamics', 4, 3);
INSERT INTO courses (id, title, description, credits, department_id) VALUES (9, 'Electromagnetism', 'Electric and magnetic fields', 4, 3);
INSERT INTO courses (id, title, description, credits, department_id) VALUES (10, 'Quantum Physics', 'Introduction to quantum mechanics', 3, 3);
-- Biology
INSERT INTO courses (id, title, description, credits, department_id) VALUES (11, 'Cell Biology', 'Structure and function of cells', 4, 4);
INSERT INTO courses (id, title, description, credits, department_id) VALUES (12, 'Genetics', 'Heredity and gene expression', 4, 4);
INSERT INTO courses (id, title, description, credits, department_id) VALUES (13, 'Ecology', 'Ecosystems and environmental biology', 3, 4);
-- Chemistry
INSERT INTO courses (id, title, description, credits, department_id) VALUES (14, 'General Chemistry', 'Atomic structure and chemical bonding', 4, 5);
INSERT INTO courses (id, title, description, credits, department_id) VALUES (15, 'Organic Chemistry', 'Carbon-based compounds', 4, 5);
INSERT INTO courses (id, title, description, credits, department_id) VALUES (16, 'Biochemistry', 'Chemistry of biological systems', 3, 5);

-- Students (8 students)
INSERT INTO students (id, first_name, last_name, email) VALUES (1, 'Alice', 'Johnson', 'alice@university.edu');
INSERT INTO students (id, first_name, last_name, email) VALUES (2, 'Bob', 'Williams', 'bob@university.edu');
INSERT INTO students (id, first_name, last_name, email) VALUES (3, 'Charlie', 'Brown', 'charlie@university.edu');
INSERT INTO students (id, first_name, last_name, email) VALUES (4, 'Diana', 'Martinez', 'diana@university.edu');
INSERT INTO students (id, first_name, last_name, email) VALUES (5, 'Eve', 'Taylor', 'eve@university.edu');
INSERT INTO students (id, first_name, last_name, email) VALUES (6, 'Frank', 'Anderson', 'frank@university.edu');
INSERT INTO students (id, first_name, last_name, email) VALUES (7, 'Grace', 'Thomas', 'grace@university.edu');
INSERT INTO students (id, first_name, last_name, email) VALUES (8, 'Henry', 'Jackson', 'henry@university.edu');

-- Student Profiles (OneToOne with shared PK)
INSERT INTO student_profiles (student_id, bio, date_of_birth, phone_number, address) VALUES (1, 'CS major passionate about AI', '2000-03-15', '+1-555-0101', '123 Main St');
INSERT INTO student_profiles (student_id, bio, date_of_birth, phone_number, address) VALUES (2, 'Double major in CS and Math', '1999-07-22', '+1-555-0102', '456 Oak Ave');
INSERT INTO student_profiles (student_id, bio, date_of_birth, phone_number, address) VALUES (3, 'Physics enthusiast', '2001-01-10', '+1-555-0103', '789 Pine Rd');
INSERT INTO student_profiles (student_id, bio, date_of_birth, phone_number, address) VALUES (4, 'Biology research assistant', '2000-11-05', '+1-555-0104', '321 Elm St');
INSERT INTO student_profiles (student_id, bio, date_of_birth, phone_number, address) VALUES (5, 'Chemistry lab coordinator', '1999-09-18', '+1-555-0105', '654 Maple Dr');
INSERT INTO student_profiles (student_id, bio, date_of_birth, phone_number, address) VALUES (6, 'Math tutor and TA', '2000-06-30', '+1-555-0106', '987 Cedar Ln');
INSERT INTO student_profiles (student_id, bio, date_of_birth, phone_number, address) VALUES (7, 'Pre-med student', '2001-04-25', '+1-555-0107', '147 Birch Ct');
INSERT INTO student_profiles (student_id, bio, date_of_birth, phone_number, address) VALUES (8, 'Transfer student from MIT', '2000-12-12', '+1-555-0108', '258 Spruce Way');

-- Course Enrollments (ManyToMany join table)
-- Alice: CS courses + Math
INSERT INTO course_students (course_id, student_id) VALUES (1, 1);
INSERT INTO course_students (course_id, student_id) VALUES (2, 1);
INSERT INTO course_students (course_id, student_id) VALUES (4, 1);
INSERT INTO course_students (course_id, student_id) VALUES (5, 1);
-- Bob: CS + Math courses
INSERT INTO course_students (course_id, student_id) VALUES (1, 2);
INSERT INTO course_students (course_id, student_id) VALUES (3, 2);
INSERT INTO course_students (course_id, student_id) VALUES (5, 2);
INSERT INTO course_students (course_id, student_id) VALUES (6, 2);
-- Charlie: Physics + Math
INSERT INTO course_students (course_id, student_id) VALUES (8, 3);
INSERT INTO course_students (course_id, student_id) VALUES (9, 3);
INSERT INTO course_students (course_id, student_id) VALUES (5, 3);
INSERT INTO course_students (course_id, student_id) VALUES (7, 3);
-- Diana: Biology + Chemistry
INSERT INTO course_students (course_id, student_id) VALUES (11, 4);
INSERT INTO course_students (course_id, student_id) VALUES (12, 4);
INSERT INTO course_students (course_id, student_id) VALUES (14, 4);
INSERT INTO course_students (course_id, student_id) VALUES (16, 4);
-- Eve: Chemistry
INSERT INTO course_students (course_id, student_id) VALUES (14, 5);
INSERT INTO course_students (course_id, student_id) VALUES (15, 5);
INSERT INTO course_students (course_id, student_id) VALUES (16, 5);
-- Frank: Math + CS
INSERT INTO course_students (course_id, student_id) VALUES (5, 6);
INSERT INTO course_students (course_id, student_id) VALUES (6, 6);
INSERT INTO course_students (course_id, student_id) VALUES (7, 6);
INSERT INTO course_students (course_id, student_id) VALUES (2, 6);
-- Grace: Biology + Chemistry
INSERT INTO course_students (course_id, student_id) VALUES (11, 7);
INSERT INTO course_students (course_id, student_id) VALUES (13, 7);
INSERT INTO course_students (course_id, student_id) VALUES (14, 7);
-- Henry: CS + Physics
INSERT INTO course_students (course_id, student_id) VALUES (1, 8);
INSERT INTO course_students (course_id, student_id) VALUES (4, 8);
INSERT INTO course_students (course_id, student_id) VALUES (8, 8);
INSERT INTO course_students (course_id, student_id) VALUES (10, 8);

-- Reviews (multiple per course to amplify the N+1 effect)
INSERT INTO reviews (id, content, rating, created_at, course_id, student_id) VALUES (1, 'Great introduction to programming concepts', 5, '2024-01-15 10:00:00', 1, 1);
INSERT INTO reviews (id, content, rating, created_at, course_id, student_id) VALUES (2, 'Very well structured course', 4, '2024-01-16 14:30:00', 1, 2);
INSERT INTO reviews (id, content, rating, created_at, course_id, student_id) VALUES (3, 'Good but could use more examples', 4, '2024-01-17 09:00:00', 1, 8);
INSERT INTO reviews (id, content, rating, created_at, course_id, student_id) VALUES (4, 'Challenging but rewarding', 5, '2024-02-01 11:00:00', 2, 1);
INSERT INTO reviews (id, content, rating, created_at, course_id, student_id) VALUES (5, 'Excellent data structures coverage', 5, '2024-02-02 16:00:00', 2, 6);
INSERT INTO reviews (id, content, rating, created_at, course_id, student_id) VALUES (6, 'SQL section was the best part', 4, '2024-02-10 10:30:00', 3, 2);
INSERT INTO reviews (id, content, rating, created_at, course_id, student_id) VALUES (7, 'Fascinating ML algorithms', 5, '2024-03-01 13:00:00', 4, 1);
INSERT INTO reviews (id, content, rating, created_at, course_id, student_id) VALUES (8, 'Very math heavy but worth it', 4, '2024-03-02 15:00:00', 4, 8);
INSERT INTO reviews (id, content, rating, created_at, course_id, student_id) VALUES (9, 'Solid calculus foundation', 4, '2024-01-20 08:00:00', 5, 1);
INSERT INTO reviews (id, content, rating, created_at, course_id, student_id) VALUES (10, 'Difficult but excellent professor', 5, '2024-01-22 12:00:00', 5, 3);
INSERT INTO reviews (id, content, rating, created_at, course_id, student_id) VALUES (11, 'Linear algebra is everywhere!', 5, '2024-02-15 10:00:00', 6, 2);
INSERT INTO reviews (id, content, rating, created_at, course_id, student_id) VALUES (12, 'Great practical applications', 4, '2024-02-20 14:00:00', 7, 3);
INSERT INTO reviews (id, content, rating, created_at, course_id, student_id) VALUES (13, 'Classical mechanics made clear', 5, '2024-01-25 09:30:00', 8, 3);
INSERT INTO reviews (id, content, rating, created_at, course_id, student_id) VALUES (14, 'Complex but fascinating topic', 4, '2024-03-10 11:00:00', 9, 3);
INSERT INTO reviews (id, content, rating, created_at, course_id, student_id) VALUES (15, 'Mind-bending quantum concepts', 5, '2024-03-15 16:30:00', 10, 8);
INSERT INTO reviews (id, content, rating, created_at, course_id, student_id) VALUES (16, 'Great lab work', 4, '2024-02-05 10:00:00', 11, 4);
INSERT INTO reviews (id, content, rating, created_at, course_id, student_id) VALUES (17, 'Genetics is my passion!', 5, '2024-02-12 13:00:00', 12, 4);
INSERT INTO reviews (id, content, rating, created_at, course_id, student_id) VALUES (18, 'Loved the field trips', 5, '2024-03-05 09:00:00', 13, 7);
INSERT INTO reviews (id, content, rating, created_at, course_id, student_id) VALUES (19, 'Solid chemistry fundamentals', 4, '2024-01-28 11:30:00', 14, 4);
INSERT INTO reviews (id, content, rating, created_at, course_id, student_id) VALUES (20, 'Organic chem is tough!', 3, '2024-02-25 15:00:00', 15, 5);
