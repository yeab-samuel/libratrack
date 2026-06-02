-- Sample students (format: DEPT/SERIAL/YEAR)
INSERT INTO university_registry (university_id, full_name, role, department) VALUES
('ATE/9305/14', 'Yeabsira Samuel',   'STUDENT', 'Software Engineering'),
('ATE/8400/14', 'Kassahun Belachew', 'STUDENT', 'Software Engineering'),
('ATE/7495/14', 'Natnael Nigatu',    'STUDENT', 'Software Engineering'),
('ATE/8814/14', 'Tsegaab Alemu',     'STUDENT', 'Software Engineering'),
('CSE/1001/16', 'Abebe Tadesse',     'STUDENT', 'Computer Science'),
('CSE/1002/16', 'Tigist Hailu',      'STUDENT', 'Computer Science'),
('MED/2001/17', 'Dawit Bekele',      'STUDENT', 'Medicine'),
('ENG/3001/15', 'Sara Girma',        'STUDENT', 'Engineering'),
-- Sample faculty (format: FAC/SERIAL/YEAR)
('FAC/0101/05', 'Dr. Abebe Girma',    'FACULTY', 'Computer Science'),
('FAC/0202/08', 'Dr. Tigist Haile',   'FACULTY', 'Engineering'),
('FAC/0303/10', 'Prof. Dawit Bekele', 'FACULTY', 'Mathematics'),
('FAC/0404/06', 'Dr. Sara Yohannes',  'FACULTY', 'Literature'),
('FAC/0505/12', 'Prof. Hana Tesfaye', 'FACULTY', 'Medicine')
ON CONFLICT (university_id) DO NOTHING;
