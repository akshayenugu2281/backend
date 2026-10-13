-- ========================================================
-- Library360 Seed Data - V8 Extended College Catalog (PostgreSQL)
-- 24 curriculum titles with physical copies for a real college
-- shelves. Explicit ids continue after V3; accession numbers are
-- backend-format (ACC-<year>-<id>). Safe to apply on any V7 database.
-- ========================================================

-- Publishers
INSERT INTO publishers (id, name, website) VALUES
(6, 'Pearson Education', 'https://www.pearson.com'),
(7, 'McGraw-Hill Education', 'https://www.mheducation.com'),
(8, 'Cambridge University Press', 'https://www.cambridge.org');

-- Authors
INSERT INTO authors (id, full_name, bio, created_at, updated_at, version) VALUES
(7, 'Andrew S. Tanenbaum', 'Computer scientist and author of widely used operating systems texts.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(8, 'Thomas H. Cormen', 'Computer scientist and co-author of the standard algorithms textbook.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(9, 'Abraham Silberschatz', 'Computer scientist known for operating system and database textbooks.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(10, 'James Stewart', 'Mathematician and author of bestselling calculus textbooks.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(11, 'Jane Austen', 'English novelist of the Romantic era.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(12, 'J.R.R. Tolkien', 'English writer and professor, author of The Hobbit and The Lord of the Rings.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(13, 'Stephen Hawking', 'Theoretical physicist and cosmologist.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(14, 'Douglas C. Giancoli', 'Physicist and author of widely used physics textbooks.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(15, 'Jonathan Clayden', 'Chemist and co-author of a leading organic chemistry textbook.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(16, 'Bruce Alberts', 'Biochemist and lead author of Molecular Biology of the Cell.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(17, 'Plato', 'Classical Greek philosopher.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(18, 'Yuval Noah Harari', 'Historian and author of Sapiens.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(19, 'Behrouz A. Forouzan', 'Computer scientist and author of networking textbooks.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(20, 'Stuart Russell', 'Computer scientist and co-author of the standard AI textbook.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(21, 'Harper Lee', 'American novelist, author of To Kill a Mockingbird.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(22, 'F. Scott Fitzgerald', 'American novelist of the Jazz Age.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(23, 'Bill Bryson', 'Author of popular science and travel books.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(24, 'Richard Dawkins', 'Evolutionary biologist and science writer.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(25, 'A.P.J. Abdul Kalam', 'Aerospace scientist and former President of India.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(26, 'William Stallings', 'Computer scientist and author of networking textbooks.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(27, 'James F. Kurose', 'Computer scientist and co-author of a leading networking textbook.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Books (ids 7-30)
INSERT INTO books (id, isbn13, title, subtitle, edition, publish_year, language, pages, description, cover_url, category_id, publisher_id, deleted, created_at, updated_at, created_by, version) VALUES
(7, '9781118063330', 'Operating System Concepts', 'With Java', '9th Edition', 2018, 'en', 1120, 'The standard dinosaur book covering processes, threads, scheduling, memory, storage and protection.', 'https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=400&q=80', 12, 7, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 0),
(8, '9780133591620', 'Modern Operating Systems', NULL, '4th Edition', 2016, 'en', 1136, 'Principles and practice of modern operating systems with case studies of UNIX, Linux and Windows.', 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=400&q=80', 12, 6, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 0),
(9, '9780262033848', 'Introduction to Algorithms', NULL, '3rd Edition', 2009, 'en', 1312, 'The CLRS classic on algorithms, data structures, complexity and design paradigms.', 'https://images.unsplash.com/photo-1509228468518-180dd4864904?w=400&q=80', 12, 3, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 0),
(10, '9780133594140', 'Computer Networking', 'A Top-Down Approach', '7th Edition', 2017, 'en', 864, 'Internet architecture from applications down to the physical layer with real protocol examples.', 'https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=400&q=80', 12, 6, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 0),
(11, '9780538497909', 'Calculus', 'Early Transcendentals', '7th Edition', 2012, 'en', 1368, 'Single and multivariable calculus with applications across engineering and the sciences.', 'https://images.unsplash.com/photo-1635372722656-389f87a17b27?w=400&q=80', 6, 7, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 0),
(12, '9780141439518', 'Pride and Prejudice', NULL, 'Penguin Classics', 2002, 'en', 480, 'Austen sparkling comedy of manners following Elizabeth Bennet and Mr Darcy.', 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400&q=80', 1, 4, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 0),
(13, '9780618260300', 'The Hobbit', 'There and Back Again', 'Illustrated Edition', 2002, 'en', 320, 'Bilbo Baggins journey with dwarves and a wizard to reclaim a stolen treasure.', 'https://images.unsplash.com/photo-1507842217343-583bb7270b66?w=400&q=80', 1, 8, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 0),
(14, '9780553380163', 'A Brief History of Time', NULL, 'Updated Edition', 1998, 'en', 212, 'From the Big Bang to black holes, an landmark tour of modern cosmology.', 'https://images.unsplash.com/photo-1462331940025-496dfbfc7564?w=400&q=80', 13, 4, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 0),
(15, '9780130606204', 'Physics', 'Principles with Applications', '6th Edition', 2004, 'en', 1024, 'Algebra-based university physics from mechanics to modern physics.', 'https://images.unsplash.com/photo-1636466497217-26a8cbeaf0aa?w=400&q=80', 13, 6, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 0),
(16, '9780199270293', 'Organic Chemistry', NULL, '1st Edition', 2012, 'en', 1320, 'Mechanism-led organic chemistry with real research examples.', 'https://images.unsplash.com/photo-1532187863486-abf9dbad1b69?w=400&q=80', 14, 5, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 0),
(17, '9780815344322', 'Molecular Biology of the Cell', NULL, '6th Edition', 2014, 'en', 1464, 'The definitive cell biology reference for life-science programmes.', 'https://images.unsplash.com/photo-1579154204601-01588f351e67?w=400&q=80', 15, 3, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 0),
(18, '9780140455113', 'The Republic', NULL, 'Penguin Classics', 2007, 'en', 416, 'Plato dialogue on justice, the ideal city and the philosopher king.', 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=400&q=80', 8, 4, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 0),
(19, '9780062316097', 'Sapiens', 'A Brief History of Humankind', '1st Edition', 2015, 'en', 464, 'How Homo sapiens came to rule the world across centuries of change.', 'https://images.unsplash.com/photo-1505664194779-8beaceb93744?w=400&q=80', 5, 4, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 0),
(20, '9780072967753', 'Data Communications and Networking', NULL, '5th Edition', 2012, 'en', 1268, 'Signals, transmission media, switching and the TCP IP protocol suite.', 'https://images.unsplash.com/photo-1544197150-b99a580bb7a8?w=400&q=80', 12, 7, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 0),
(21, '9780073523323', 'Database System Concepts', NULL, '6th Edition', 2010, 'en', 1376, 'Data models, relational design, SQL, transactions and database internals.', 'https://images.unsplash.com/photo-1544383835-bda2bc66a55d?w=400&q=80', 12, 7, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 0),
(22, '9780136042594', 'Artificial Intelligence', 'A Modern Approach', '3rd Edition', 2010, 'en', 1152, 'The standard survey of intelligent agents, search, learning and robotics.', 'https://images.unsplash.com/photo-1620712943543-bcc4688e7485?w=400&q=80', 12, 6, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 0),
(23, '9780134494166', 'Clean Architecture', 'A Craftsman Guide to Software Structure', '1st Edition', 2017, 'en', 432, 'Timeless rules for structuring software systems that are easy to change.', 'https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=400&q=80', 12, 6, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 0),
(24, '9780134757599', 'Refactoring', 'Improving the Design of Existing Code', '2nd Edition', 2018, 'en', 448, 'The catalog of refactorings every working programmer should know.', 'https://images.unsplash.com/photo-1461749280684-dccba630e2f6?w=400&q=80', 12, 2, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 0),
(25, '9780451526342', 'Animal Farm', NULL, 'Centennial Edition', 2003, 'en', 144, 'Orwell allegorical tale of revolution and power on an English farm.', 'https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=400&q=80', 1, 4, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 0),
(26, '9780061120084', 'To Kill a Mockingbird', NULL, '50th Anniversary', 2010, 'en', 336, 'Atticus Finch defends an innocent man in Depression-era Alabama.', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=400&q=80', 7, 4, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 0),
(27, '9780743273565', 'The Great Gatsby', NULL, 'Scribner Edition', 2004, 'en', 180, 'Jay Gatsby glittering pursuit of the American dream in the Jazz Age.', 'https://images.unsplash.com/photo-1532012164546-f432f2e3777a?w=400&q=80', 7, 4, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 0),
(28, '9780767908177', 'A Short History of Nearly Everything', NULL, 'Illustrated Edition', 2005, 'en', 544, 'Bryson entertaining journey through cosmology, geology and biology.', 'https://images.unsplash.com/photo-1532094349884-543bc11b234d?w=400&q=80', 3, 4, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 0),
(29, '9780192860927', 'The Selfish Gene', NULL, '30th Anniversary', 2006, 'en', 360, 'The gene-centred view of evolution that reshaped biology.', 'https://images.unsplash.com/photo-1530026405186-ed1f139313f8?w=400&q=80', 15, 5, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 0),
(30, '9788173711467', 'Wings of Fire', 'An Autobiography', '1st Edition', 1999, 'en', 180, 'The life journey of A.P.J. Abdul Kalam from Rameswaram to Rashtrapati Bhavan.', 'https://images.unsplash.com/photo-1506880018603-83d5b814b5a6?w=400&q=80', 2, 8, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 0);

-- Book Authors
INSERT INTO book_authors (book_id, author_id) VALUES
(7, 9), (8, 7), (9, 8), (10, 26), (11, 10), (12, 11), (13, 12), (14, 13),
(15, 14), (16, 15), (17, 16), (18, 17), (19, 18), (20, 19), (21, 9), (22, 20),
(23, 1), (24, 2), (25, 5), (26, 21), (27, 22), (28, 23), (29, 24), (30, 25),
(10, 27);

-- Book Copies (accession numbers follow backend format ACC-<year>-<id>)
INSERT INTO book_copies (id, book_id, barcode, accession_number, shelf, "condition", status, acquired_on, notes, created_at, updated_at, version) VALUES
(10, 7, 'BC-OSC-001', 'ACC-2024-000010', 'A-04-01', 'GOOD', 'AVAILABLE', '2024-08-15', 'Core OS text', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(11, 7, 'BC-OSC-002', 'ACC-2024-000011', 'A-04-02', 'GOOD', 'AVAILABLE', '2024-08-15', 'Second copy', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(12, 8, 'BC-MOS-001', 'ACC-2024-000012', 'A-04-03', 'GOOD', 'AVAILABLE', '2024-08-15', 'Core OS text', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(13, 8, 'BC-MOS-002', 'ACC-2025-000013', 'A-04-04', 'GOOD', 'AVAILABLE', '2025-01-20', 'Replacement copy', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(14, 9, 'BC-CLRS-001', 'ACC-2024-000014', 'A-05-01', 'GOOD', 'AVAILABLE', '2024-08-15', 'Algorithms shelf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(15, 9, 'BC-CLRS-002', 'ACC-2024-000015', 'A-05-02', 'FAIR', 'AVAILABLE', '2024-08-15', 'Well thumbed', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(16, 10, 'BC-CN-001', 'ACC-2025-000016', 'A-06-01', 'GOOD', 'AVAILABLE', '2025-01-20', 'Networking shelf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(17, 11, 'BC-CAL-001', 'ACC-2024-000017', 'B-02-01', 'GOOD', 'AVAILABLE', '2024-08-15', 'Maths section', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(18, 12, 'BC-PP-001', 'ACC-2024-000018', 'D-01-01', 'GOOD', 'AVAILABLE', '2024-09-01', 'Fiction section', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(19, 13, 'BC-HOB-001', 'ACC-2024-000019', 'D-01-02', 'GOOD', 'AVAILABLE', '2024-09-01', 'Fiction section', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(20, 14, 'BC-BHT-001', 'ACC-2025-000020', 'C-04-02', 'GOOD', 'AVAILABLE', '2025-02-10', 'Science shelf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(21, 15, 'BC-PHY-001', 'ACC-2024-000021', 'C-01-01', 'GOOD', 'AVAILABLE', '2024-08-15', 'Physics lab shelf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(22, 16, 'BC-OCH-001', 'ACC-2025-000022', 'C-02-01', 'GOOD', 'AVAILABLE', '2025-02-10', 'Chemistry shelf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(23, 17, 'BC-MBC-001', 'ACC-2025-000023', 'C-03-01', 'GOOD', 'AVAILABLE', '2025-02-10', 'Biology shelf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(24, 18, 'BC-REP-001', 'ACC-2024-000024', 'E-01-01', 'GOOD', 'AVAILABLE', '2024-09-01', 'Philosophy shelf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(25, 19, 'BC-SAP-001', 'ACC-2025-000025', 'E-02-01', 'GOOD', 'AVAILABLE', '2025-03-01', 'History shelf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(26, 20, 'BC-DCN-001', 'ACC-2024-000026', 'A-06-02', 'GOOD', 'AVAILABLE', '2024-08-15', 'Networking shelf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(27, 21, 'BC-DSC-001', 'ACC-2024-000027', 'A-07-01', 'GOOD', 'AVAILABLE', '2024-08-15', 'Databases shelf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(28, 21, 'BC-DSC-002', 'ACC-2025-000028', 'A-07-02', 'GOOD', 'AVAILABLE', '2025-01-20', 'Second copy', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(29, 22, 'BC-AI-001', 'ACC-2024-000029', 'A-08-01', 'GOOD', 'AVAILABLE', '2024-08-15', 'AI shelf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(30, 22, 'BC-AI-002', 'ACC-2025-000030', 'A-08-02', 'GOOD', 'AVAILABLE', '2025-01-20', 'Second copy', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(31, 23, 'BC-CA-001', 'ACC-2025-000031', 'A-09-01', 'GOOD', 'AVAILABLE', '2025-01-20', 'New arrival', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(32, 24, 'BC-REF-001', 'ACC-2025-000032', 'A-09-02', 'GOOD', 'AVAILABLE', '2025-01-20', 'New arrival', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(33, 25, 'BC-AF-001', 'ACC-2024-000033', 'D-01-03', 'GOOD', 'AVAILABLE', '2024-09-01', 'Fiction section', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(34, 26, 'BC-TKM-001', 'ACC-2025-000034', 'D-03-01', 'GOOD', 'AVAILABLE', '2025-03-01', 'Literature shelf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(35, 27, 'BC-GG-001', 'ACC-2025-000035', 'D-03-02', 'GOOD', 'AVAILABLE', '2025-03-01', 'Literature shelf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(36, 28, 'BC-SH-001', 'ACC-2025-000036', 'C-05-01', 'GOOD', 'AVAILABLE', '2025-03-01', 'Science shelf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(37, 29, 'BC-SG-001', 'ACC-2025-000037', 'C-03-02', 'GOOD', 'AVAILABLE', '2025-03-01', 'Biology shelf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(38, 30, 'BC-WOF-001', 'ACC-2025-000038', 'D-04-01', 'GOOD', 'AVAILABLE', '2025-03-01', 'Biography shelf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Synchronize PostgreSQL serial sequences
SELECT setval(pg_get_serial_sequence('publishers', 'id'), COALESCE((SELECT MAX(id) FROM publishers), 1));
SELECT setval(pg_get_serial_sequence('authors', 'id'), COALESCE((SELECT MAX(id) FROM authors), 1));
SELECT setval(pg_get_serial_sequence('books', 'id'), COALESCE((SELECT MAX(id) FROM books), 1));
SELECT setval(pg_get_serial_sequence('book_copies', 'id'), COALESCE((SELECT MAX(id) FROM book_copies), 1));
