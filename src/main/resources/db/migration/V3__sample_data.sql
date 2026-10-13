-- ========================================================
-- Library360 Sample Data - V3 Catalog & Circulation Seed
-- ========================================================

-- Publishers
INSERT INTO publishers (id, name, website) VALUES
(1, 'O''Reilly Media', 'https://www.oreilly.com'),
(2, 'Addison-Wesley Professional', 'https://www.informit.com/addison-wesley'),
(3, 'MIT Press', 'https://mitpress.mit.edu'),
(4, 'Penguin Random House', 'https://www.penguinrandomhouse.com'),
(5, 'Oxford University Press', 'https://global.oup.com');

-- Authors
INSERT INTO authors (id, full_name, bio, created_at, updated_at, version) VALUES
(1, 'Robert C. Martin', 'Known informally as Uncle Bob, software engineer and author of Clean Code and Clean Architecture.', NOW(6), NOW(6), 0),
(2, 'Martin Fowler', 'Chief Scientist at ThoughtWorks, author specializing in object-oriented design and refactoring.', NOW(6), NOW(6), 0),
(3, 'Joshua Bloch', 'Author of Effective Java and former chief Java architect at Sun Microsystems and Google.', NOW(6), NOW(6), 0),
(4, 'Donald E. Knuth', 'Professor Emeritus of Computer Science at Stanford and author of The Art of Computer Programming.', NOW(6), NOW(6), 0),
(5, 'George Orwell', 'English novelist, essayist, and critic famous for Animal Farm and Nineteen Eighty-Four.', NOW(6), NOW(6), 0),
(6, 'Carl Sagan', 'American astronomer, planetary scientist, and science communicator.', NOW(6), NOW(6), 0);

-- Books
INSERT INTO books (id, isbn13, title, subtitle, edition, publish_year, language, pages, description, cover_url, category_id, publisher_id, deleted, created_at, updated_at, created_by, version) VALUES
(1, '9780132350884', 'Clean Code', 'A Handbook of Agile Software Craftsmanship', '1st Edition', 2008, 'en', 464, 'Even bad code can function. But if code isn''t clean, it can bring a development organization to its knees. Every year, countless hours and significant resources are lost because of poorly written code. But it doesn''t have to be that way.', 'https://images.unsplash.com/photo-1532012164546-f432f2e3777a?w=400&q=80', 12, 2, false, NOW(6), NOW(6), 'system', 0),
(2, '9780134685991', 'Effective Java', 'Best Practices for the Java Platform', '3rd Edition', 2017, 'en', 412, 'The definitive guide to best-practices Java programming platform that addresses modern Java 8 and 9 idioms, lambdas, and streams.', 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400&q=80', 12, 2, false, NOW(6), NOW(6), 'system', 0),
(3, '9780201633610', 'Design Patterns', 'Elements of Reusable Object-Oriented Software', '1st Edition', 1994, 'en', 395, 'Captures a wealth of experience in design patterns that have emerged from object-oriented programming systems.', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=400&q=80', 12, 2, false, NOW(6), NOW(6), 'system', 0),
(4, '9780345539434', 'Cosmos', NULL, 'Illustrated Edition', 1980, 'en', 365, 'Explores the universe and humanity''s place within it through history, science, philosophy, and astrophysics.', 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=400&q=80', 13, 4, false, NOW(6), NOW(6), 'system', 0),
(5, '9780451524935', '1984', 'A Novel', 'Centennial Edition', 1949, 'en', 328, 'The dystopian classic depicting the perils of totalitarianism, pervasive mass surveillance, and totalitarian control.', 'https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=400&q=80', 1, 4, false, NOW(6), NOW(6), 'system', 0),
(6, '9780201896831', 'The Art of Computer Programming', 'Vol 1: Fundamental Algorithms', '3rd Edition', 1997, 'en', 650, 'The bible of all fundamental computer algorithms and mathematical foundations of computing by Donald Knuth.', 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=400&q=80', 12, 2, false, NOW(6), NOW(6), 'system', 0);

-- Book Authors
INSERT INTO book_authors (book_id, author_id) VALUES
(1, 1),
(2, 3),
(3, 1),
(4, 6),
(5, 5),
(6, 4);

-- Book Copies
INSERT INTO book_copies (id, book_id, barcode, shelf, `condition`, status, acquired_on, notes, created_at, updated_at, version) VALUES
(1, 1, 'BC-CC-001', 'A-01-01', 'GOOD', 'AVAILABLE', '2025-01-15', 'Main lending copy', NOW(6), NOW(6), 0),
(2, 1, 'BC-CC-002', 'A-01-02', 'EXCELLENT', 'AVAILABLE', '2025-01-15', 'Second lending copy', NOW(6), NOW(6), 0),
(3, 2, 'BC-EJ-001', 'A-02-01', 'EXCELLENT', 'AVAILABLE', '2025-02-10', 'Faculty reference', NOW(6), NOW(6), 0),
(4, 2, 'BC-EJ-002', 'A-02-02', 'GOOD', 'AVAILABLE', '2025-02-10', 'Circulation copy', NOW(6), NOW(6), 0),
(5, 3, 'BC-DP-001', 'B-01-03', 'FAIR', 'AVAILABLE', '2024-08-20', 'Classic patterns text', NOW(6), NOW(6), 0),
(6, 4, 'BC-CS-001', 'C-04-01', 'EXCELLENT', 'AVAILABLE', '2025-03-01', 'Science shelf', NOW(6), NOW(6), 0),
(7, 5, 'BC-1984-01', 'D-02-05', 'GOOD', 'AVAILABLE', '2024-11-12', 'Fiction section', NOW(6), NOW(6), 0),
(8, 6, 'BC-TAOCP-1', 'A-03-01', 'EXCELLENT', 'AVAILABLE', '2025-01-05', 'Reference only', NOW(6), NOW(6), 0);
