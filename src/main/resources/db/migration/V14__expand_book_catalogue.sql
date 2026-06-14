-- =============================================================================
-- V14: Expanded book catalogue with physical copies
-- 41 titles across all 6 categories · 117 physical copies
-- Covers Science, Engineering, Humanities, Law, Medicine, Other
-- ON CONFLICT clauses make this migration safe to re-run on any environment.
-- =============================================================================

-- ═══════════════════════════════════════════════════════════════
--  BOOKS
-- ═══════════════════════════════════════════════════════════════

INSERT INTO books (isbn, title, author, category, publisher, published_year, total_copies, description, created_at) VALUES

-- ── SCIENCE (8 titles) ────────────────────────────────────────
('9780553380163', 'A Brief History of Time',
 'Stephen Hawking', 'SCIENCE', 'Bantam Books', 1988, 3,
 'A landmark exploration of cosmology covering the Big Bang, black holes, light cones, and the nature of time, written for general readers.', NOW()),

('9780198788621', 'The Selfish Gene',
 'Richard Dawkins', 'SCIENCE', 'Oxford University Press', 1976, 3,
 'Dawkins presents a gene-centred view of evolution, introducing the concept of the meme and explaining natural selection from the gene''s perspective.', NOW()),

('9780345539434', 'Cosmos',
 'Carl Sagan', 'SCIENCE', 'Random House', 1980, 3,
 'A sweeping journey through space and time, tracing the origins of the universe, the rise of science, and humanity''s place in the cosmos.', NOW()),

('9780465023820', 'The Feynman Lectures on Physics, Vol. 1',
 'Richard P. Feynman', 'SCIENCE', 'Basic Books', 1964, 2,
 'The legendary lecture series covering mechanics, radiation, and heat. Considered the finest physics teaching text ever written.', NOW()),

('9780226458120', 'The Structure of Scientific Revolutions',
 'Thomas S. Kuhn', 'SCIENCE', 'University of Chicago Press', 1962, 2,
 'A landmark work on the history and philosophy of science introducing the concept of paradigm shifts and how scientific knowledge evolves.', NOW()),

('9780140432053', 'On the Origin of Species',
 'Charles Darwin', 'SCIENCE', 'Penguin Classics', 1859, 2,
 'Darwin''s foundational work presenting evidence for evolution by natural selection — arguably the most important science book ever written.', NOW()),

('9781305270336', 'Calculus: Early Transcendentals',
 'James Stewart', 'SCIENCE', 'Cengage Learning', 2015, 4,
 'A comprehensive and rigorous introduction to single and multivariable calculus widely adopted across universities worldwide.', NOW()),

('9780135159552', 'University Physics with Modern Physics',
 'Young & Freedman', 'SCIENCE', 'Pearson', 2019, 4,
 'A thorough undergraduate physics text covering mechanics, thermodynamics, electromagnetism, optics, and modern physics with worked examples.', NOW()),

-- ── ENGINEERING (6 titles) ────────────────────────────────────
('9780262033848', 'Introduction to Algorithms',
 'Cormen, Leiserson, Rivest & Stein', 'ENGINEERING', 'MIT Press', 2009, 4,
 'The definitive reference for algorithms and data structures used in computer science education worldwide. Covers sorting, graph algorithms, dynamic programming, and complexity.', NOW()),

('9780201835953', 'The Mythical Man-Month',
 'Frederick P. Brooks Jr.', 'ENGINEERING', 'Addison-Wesley', 1975, 2,
 'Essays on software engineering addressing why adding people to a late project makes it later, based on IBM System/360 development experience.', NOW()),

('9780133918922', 'Engineering Mechanics: Statics',
 'Russell C. Hibbeler', 'ENGINEERING', 'Pearson', 2016, 4,
 'Comprehensive coverage of statics fundamentals including force systems, equilibrium, structural analysis, friction, and moments of inertia.', NOW()),

('9780134610672', 'Structural Analysis',
 'Russell C. Hibbeler', 'ENGINEERING', 'Pearson', 2018, 3,
 'A clear and thorough presentation of structural analysis methods for beams, frames, and trusses with real-world engineering applications.', NOW()),

('9780078028229', 'Fundamentals of Electric Circuits',
 'Alexander & Sadiku', 'ENGINEERING', 'McGraw-Hill', 2020, 4,
 'A student-friendly introduction to circuit analysis covering DC/AC circuits, operational amplifiers, capacitors, inductors, and frequency response.', NOW()),

-- ── HUMANITIES (7 titles) ─────────────────────────────────────
('9780451524935', '1984',
 'George Orwell', 'HUMANITIES', 'Signet Classic', 1949, 3,
 'A dystopian vision of a totalitarian surveillance state where language is weaponised and individual thought is a crime. Essential reading for understanding political power.', NOW()),

('9780061743528', 'To Kill a Mockingbird',
 'Harper Lee', 'HUMANITIES', 'Harper Perennial', 1960, 3,
 'Through a child''s eyes in Depression-era Alabama, this Pulitzer Prize-winning novel addresses racial injustice and moral courage with compassion and clarity.', NOW()),

('9780140455113', 'The Republic',
 'Plato', 'HUMANITIES', 'Penguin Classics', 2000, 2,
 'Plato''s foundational dialogue on justice, the ideal state, the nature of the soul, and the role of philosophy in political life. Translated by Desmond Lee.', NOW()),

('9780140449334', 'Meditations',
 'Marcus Aurelius', 'HUMANITIES', 'Penguin Classics', 2002, 2,
 'Personal writings of the Roman Emperor and Stoic philosopher — a guide to resilience, self-discipline, and finding meaning amid uncertainty.', NOW()),

('9780393317558', 'Guns, Germs, and Steel',
 'Jared Diamond', 'HUMANITIES', 'W. W. Norton', 1997, 3,
 'A Pulitzer Prize-winning explanation of why some societies came to dominate others through geography, agriculture, and the spread of disease rather than racial superiority.', NOW()),

('9780073407333', 'A History of the Modern World',
 'R.R. Palmer & Joel Colton', 'HUMANITIES', 'McGraw-Hill', 2013, 2,
 'A comprehensive survey of world history from 1500 to the present covering revolutions, world wars, colonialism, and the emergence of the modern global order.', NOW()),

('9780415693431', 'Philosophy: The Basics',
 'Nigel Warburton', 'HUMANITIES', 'Routledge', 2012, 3,
 'An accessible and engaging introduction to five core areas of philosophy: appearance and reality, right and wrong, political power, God''s existence, and the mind.', NOW()),

-- ── LAW (7 titles) ────────────────────────────────────────────
('9780141976402', 'The Rule of Law',
 'Tom Bingham', 'LAW', 'Penguin Books', 2010, 3,
 'A former Lord Chief Justice of England explains the meaning of the rule of law, its historical development, and why it is indispensable to a just society.', NOW()),

('9781454873204', 'Constitutional Law',
 'Erwin Chemerinsky', 'LAW', 'Wolters Kluwer', 2019, 3,
 'A widely used constitutional law casebook covering the structure of government, judicial review, individual rights, equal protection, and due process.', NOW()),

('9781628102154', 'Criminal Law: Cases and Materials',
 'Joshua Dressler', 'LAW', 'West Academic', 2018, 3,
 'Comprehensive coverage of criminal law principles including actus reus, mens rea, homicide, theft, defences, and constitutional limitations on criminal punishment.', NOW()),

('9781107618459', 'International Law',
 'Malcolm N. Shaw', 'LAW', 'Cambridge University Press', 2017, 2,
 'The leading textbook on public international law covering sources, subjects, jurisdiction, state responsibility, human rights, and the law of the sea.', NOW()),

('9780314158956', 'Black''s Law Dictionary',
 'Bryan A. Garner', 'LAW', 'West Publishing', 2019, 2,
 'The most widely cited law book in the world. Defines thousands of legal terms with precise meanings, historical context, and citation to authoritative sources.', NOW()),

('9780199556328', 'Jurisprudence: Theory and Context',
 'Brian Bix', 'LAW', 'Westview Press', 2018, 3,
 'A clear introduction to legal theory covering natural law, legal positivism, legal realism, critical legal studies, feminist jurisprudence, and law and economics.', NOW()),

('9780314290342', 'Legal Writing in Plain English',
 'Bryan A. Garner', 'LAW', 'University of Chicago Press', 2013, 3,
 'Practical guidance for lawyers and law students on producing clear, readable legal documents — covering structure, style, grammar, and document design.', NOW()),

-- ── MEDICINE (7 titles) ───────────────────────────────────────
('9780702077050', 'Gray''s Anatomy',
 'Henry Gray', 'MEDICINE', 'Elsevier', 2015, 2,
 'The definitive reference work on human anatomy, now in its 41st edition. Covers every body system with clinical commentary and superb anatomical illustrations.', NOW()),

('9781259644030', 'Harrison''s Principles of Internal Medicine',
 'Kasper, Fauci & Longo', 'MEDICINE', 'McGraw-Hill', 2018, 2,
 'The foremost reference for clinical medicine. Provides authoritative coverage of the pathophysiology, diagnosis, and treatment of all major diseases.', NOW()),

('9781439107943', 'The Emperor of All Maladies: A Biography of Cancer',
 'Siddhartha Mukherjee', 'MEDICINE', 'Scribner', 2010, 3,
 'A Pulitzer Prize-winning history of cancer — its biology, the development of treatments, and the human stories behind centuries of struggle to understand the disease.', NOW()),

('9780071371599', 'Pharmacology: An Illustrated Review',
 'Richard Finkel & Luigi Cubeddu', 'MEDICINE', 'McGraw-Hill', 2020, 3,
 'A concise, visually rich pharmacology review covering drug classes, mechanisms of action, therapeutic uses, and adverse effects for medical students.', NOW()),

('9780323394390', 'Robbins Basic Pathology',
 'Kumar, Abbas & Aster', 'MEDICINE', 'Elsevier', 2017, 3,
 'A clearly written, well-illustrated introduction to the mechanisms of disease. Covers cell injury, inflammation, neoplasia, and disease of all major organ systems.', NOW()),

('9780323280396', 'Medical Physiology',
 'Boron & Boulpaep', 'MEDICINE', 'Elsevier', 2016, 2,
 'A quantitative, molecular approach to physiology covering the cellular basis of organ function across the nervous system, cardiovascular, renal, respiratory, and endocrine systems.', NOW()),

('9780702066979', 'Clinical Medicine',
 'Parveen Kumar & Michael Clark', 'MEDICINE', 'Elsevier', 2020, 3,
 'A trusted clinical companion covering the presentation, investigation, and management of medical conditions encountered in practice and clinical examinations.', NOW()),

-- ── OTHER (7 titles) ──────────────────────────────────────────
('9781603580557', 'Thinking in Systems: A Primer',
 'Donella H. Meadows', 'OTHER', 'Chelsea Green Publishing', 2008, 3,
 'An accessible introduction to systems thinking — how to understand feedback loops, stocks, flows, and delays that drive behaviour in complex systems.', NOW()),

('9781285165776', 'Principles of Economics',
 'N. Gregory Mankiw', 'OTHER', 'Cengage Learning', 2020, 4,
 'The world''s most popular economics textbook covering supply and demand, markets, firm behaviour, macroeconomics, monetary policy, and international trade.', NOW()),

('9780857197689', 'The Psychology of Money',
 'Morgan Housel', 'OTHER', 'Harriman House', 2020, 3,
 'Nineteen short stories exploring the strange ways people think about money — on greed, fear, risk, wealth, and why financial success depends more on behaviour than knowledge.', NOW()),

('9780060731328', 'Freakonomics',
 'Steven D. Levitt & Stephen J. Dubner', 'OTHER', 'Harper Perennial', 2005, 3,
 'An economist and journalist explore the hidden side of everything — using data and incentives to find surprising answers to unconventional questions.', NOW()),

('9780553804911', 'Emotional Intelligence',
 'Daniel Goleman', 'OTHER', 'Bantam Books', 1995, 3,
 'The groundbreaking work arguing that emotional intelligence — self-awareness, empathy, and self-regulation — matters more than IQ for success in life and work.', NOW()),

('9780307887894', 'The Lean Startup',
 'Eric Ries', 'OTHER', 'Crown Business', 2011, 3,
 'A methodology for building successful companies through validated learning, rapid experimentation, and iterative product releases to reduce market risk.', NOW()),

('9780374533557', 'Thinking, Fast and Slow',
 'Daniel Kahneman', 'OTHER', 'Farrar, Straus and Giroux', 2011, 3,
 'Nobel laureate Kahneman reveals two systems of thought: fast intuitive thinking and slow deliberate reasoning, and how their interplay shapes our decisions and judgements.', NOW())

ON CONFLICT (isbn) DO NOTHING;


-- ═══════════════════════════════════════════════════════════════
--  BOOK COPIES  (131 total)
--  Conditions reflect book age: NEW = recent, GOOD = standard,
--  WORN = heavily used older copy.
--  Most copies AVAILABLE; a few ON_LOAN to simulate active lending.
-- ═══════════════════════════════════════════════════════════════

INSERT INTO book_copies (book_id, copy_number, condition, status, added_at)
SELECT b.id, v.cn, v.cond, v.stat, NOW()
FROM (VALUES
  -- ── SCIENCE copies ───────────────────────────────────────────
  ('9780553380163','SC-001','GOOD','AVAILABLE'),
  ('9780553380163','SC-002','GOOD','ON_LOAN'),
  ('9780553380163','SC-003','WORN','AVAILABLE'),
  ('9780198788621','SC-004','GOOD','AVAILABLE'),
  ('9780198788621','SC-005','GOOD','AVAILABLE'),
  ('9780198788621','SC-006','WORN','ON_LOAN'),
  ('9780345539434','SC-007','GOOD','AVAILABLE'),
  ('9780345539434','SC-008','GOOD','AVAILABLE'),
  ('9780345539434','SC-009','WORN','AVAILABLE'),
  ('9780465023820','SC-010','GOOD','AVAILABLE'),
  ('9780465023820','SC-011','WORN','AVAILABLE'),
  ('9780226458120','SC-012','WORN','AVAILABLE'),
  ('9780226458120','SC-013','WORN','AVAILABLE'),
  ('9780140432053','SC-014','GOOD','AVAILABLE'),
  ('9780140432053','SC-015','WORN','ON_LOAN'),
  ('9781305270336','SC-016','NEW','AVAILABLE'),
  ('9781305270336','SC-017','NEW','AVAILABLE'),
  ('9781305270336','SC-018','GOOD','AVAILABLE'),
  ('9781305270336','SC-019','GOOD','ON_LOAN'),
  ('9780135159552','SC-020','NEW','AVAILABLE'),
  ('9780135159552','SC-021','NEW','AVAILABLE'),
  ('9780135159552','SC-022','GOOD','AVAILABLE'),
  ('9780135159552','SC-023','GOOD','AVAILABLE'),
  -- ── ENGINEERING copies ────────────────────────────────────────
  ('9780262033848','EN-008','GOOD','AVAILABLE'),
  ('9780262033848','EN-009','GOOD','AVAILABLE'),
  ('9780262033848','EN-010','GOOD','ON_LOAN'),
  ('9780262033848','EN-011','WORN','AVAILABLE'),
  ('9780201835953','EN-015','WORN','AVAILABLE'),
  ('9780201835953','EN-016','WORN','AVAILABLE'),
  ('9780133918922','EN-017','NEW','AVAILABLE'),
  ('9780133918922','EN-018','NEW','AVAILABLE'),
  ('9780133918922','EN-019','GOOD','AVAILABLE'),
  ('9780133918922','EN-020','GOOD','ON_LOAN'),
  ('9780134610672','EN-021','NEW','AVAILABLE'),
  ('9780134610672','EN-022','GOOD','AVAILABLE'),
  ('9780134610672','EN-023','GOOD','AVAILABLE'),
  ('9780078028229','EN-024','NEW','AVAILABLE'),
  ('9780078028229','EN-025','NEW','AVAILABLE'),
  ('9780078028229','EN-026','GOOD','AVAILABLE'),
  ('9780078028229','EN-027','GOOD','ON_LOAN'),
  -- ── HUMANITIES copies ─────────────────────────────────────────
  ('9780451524935','HU-005','GOOD','AVAILABLE'),
  ('9780451524935','HU-006','GOOD','AVAILABLE'),
  ('9780451524935','HU-007','WORN','ON_LOAN'),
  ('9780061743528','HU-008','GOOD','AVAILABLE'),
  ('9780061743528','HU-009','GOOD','AVAILABLE'),
  ('9780061743528','HU-010','WORN','AVAILABLE'),
  ('9780140455113','HU-011','GOOD','AVAILABLE'),
  ('9780140455113','HU-012','WORN','AVAILABLE'),
  ('9780140449334','HU-013','GOOD','AVAILABLE'),
  ('9780140449334','HU-014','WORN','AVAILABLE'),
  ('9780393317558','HU-015','GOOD','AVAILABLE'),
  ('9780393317558','HU-016','GOOD','ON_LOAN'),
  ('9780393317558','HU-017','WORN','AVAILABLE'),
  ('9780073407333','HU-018','GOOD','AVAILABLE'),
  ('9780073407333','HU-019','WORN','AVAILABLE'),
  ('9780415693431','HU-020','GOOD','AVAILABLE'),
  ('9780415693431','HU-021','GOOD','AVAILABLE'),
  ('9780415693431','HU-022','WORN','AVAILABLE'),
  -- ── LAW copies ────────────────────────────────────────────────
  ('9780141976402','LW-001','GOOD','AVAILABLE'),
  ('9780141976402','LW-002','GOOD','AVAILABLE'),
  ('9780141976402','LW-003','WORN','ON_LOAN'),
  ('9781454873204','LW-004','NEW','AVAILABLE'),
  ('9781454873204','LW-005','GOOD','AVAILABLE'),
  ('9781454873204','LW-006','GOOD','ON_LOAN'),
  ('9781628102154','LW-007','NEW','AVAILABLE'),
  ('9781628102154','LW-008','GOOD','AVAILABLE'),
  ('9781628102154','LW-009','GOOD','AVAILABLE'),
  ('9781107618459','LW-010','GOOD','AVAILABLE'),
  ('9781107618459','LW-011','WORN','AVAILABLE'),
  ('9780314158956','LW-012','NEW','AVAILABLE'),
  ('9780314158956','LW-013','GOOD','AVAILABLE'),
  ('9780199556328','LW-014','GOOD','AVAILABLE'),
  ('9780199556328','LW-015','GOOD','AVAILABLE'),
  ('9780199556328','LW-016','WORN','AVAILABLE'),
  ('9780314290342','LW-017','GOOD','AVAILABLE'),
  ('9780314290342','LW-018','GOOD','AVAILABLE'),
  ('9780314290342','LW-019','WORN','AVAILABLE'),
  -- ── MEDICINE copies ───────────────────────────────────────────
  ('9780702077050','MD-001','NEW','AVAILABLE'),
  ('9780702077050','MD-002','GOOD','ON_LOAN'),
  ('9781259644030','MD-003','NEW','AVAILABLE'),
  ('9781259644030','MD-004','GOOD','AVAILABLE'),
  ('9781439107943','MD-005','GOOD','AVAILABLE'),
  ('9781439107943','MD-006','GOOD','AVAILABLE'),
  ('9781439107943','MD-007','WORN','ON_LOAN'),
  ('9780071371599','MD-008','NEW','AVAILABLE'),
  ('9780071371599','MD-009','GOOD','AVAILABLE'),
  ('9780071371599','MD-010','GOOD','AVAILABLE'),
  ('9780323394390','MD-011','GOOD','AVAILABLE'),
  ('9780323394390','MD-012','GOOD','ON_LOAN'),
  ('9780323394390','MD-013','WORN','AVAILABLE'),
  ('9780323280396','MD-014','GOOD','AVAILABLE'),
  ('9780323280396','MD-015','WORN','AVAILABLE'),
  ('9780702066979','MD-016','NEW','AVAILABLE'),
  ('9780702066979','MD-017','GOOD','AVAILABLE'),
  ('9780702066979','MD-018','GOOD','AVAILABLE'),
  -- ── OTHER copies ──────────────────────────────────────────────
  ('9781603580557','OT-001','GOOD','AVAILABLE'),
  ('9781603580557','OT-002','GOOD','AVAILABLE'),
  ('9781603580557','OT-003','WORN','ON_LOAN'),
  ('9781285165776','OT-004','NEW','AVAILABLE'),
  ('9781285165776','OT-005','NEW','AVAILABLE'),
  ('9781285165776','OT-006','GOOD','AVAILABLE'),
  ('9781285165776','OT-007','GOOD','ON_LOAN'),
  ('9780857197689','OT-008','NEW','AVAILABLE'),
  ('9780857197689','OT-009','NEW','AVAILABLE'),
  ('9780857197689','OT-010','GOOD','AVAILABLE'),
  ('9780060731328','OT-011','GOOD','AVAILABLE'),
  ('9780060731328','OT-012','GOOD','AVAILABLE'),
  ('9780060731328','OT-013','WORN','ON_LOAN'),
  ('9780553804911','OT-014','GOOD','AVAILABLE'),
  ('9780553804911','OT-015','GOOD','AVAILABLE'),
  ('9780553804911','OT-016','WORN','AVAILABLE'),
  ('9780307887894','OT-017','GOOD','AVAILABLE'),
  ('9780307887894','OT-018','GOOD','AVAILABLE'),
  ('9780307887894','OT-019','WORN','ON_LOAN'),
  ('9780374533557','OT-020','GOOD','AVAILABLE'),
  ('9780374533557','OT-021','GOOD','AVAILABLE'),
  ('9780374533557','OT-022','WORN','AVAILABLE')
) AS v(isbn, cn, cond, stat)
JOIN books b ON b.isbn = v.isbn
ON CONFLICT (copy_number) DO NOTHING;