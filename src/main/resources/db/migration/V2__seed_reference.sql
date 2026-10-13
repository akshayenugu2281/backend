-- ========================================================
-- Library360 Seed Data - V2 Reference Data
-- ========================================================

-- Roles
INSERT INTO roles (id, name) VALUES
(1, 'ADMIN'),
(2, 'LIBRARIAN'),
(3, 'FACULTY'),
(4, 'STUDENT');

-- Permissions
INSERT INTO permissions (id, code, description) VALUES
(1,  'book:read',          'View books and catalog'),
(2,  'book:write',         'Create, update, delete books'),
(3,  'member:read',        'View member profiles'),
(4,  'member:write',       'Create, update members'),
(5,  'loan:issue',         'Issue books to members'),
(6,  'loan:return',        'Accept book returns'),
(7,  'loan:renew',         'Renew loans'),
(8,  'reservation:create', 'Create reservations'),
(9,  'fine:collect',       'Collect fine payments'),
(10, 'fine:waive',         'Waive fines'),
(11, 'report:read',        'View reports and analytics'),
(12, 'user:manage',        'Manage users and roles'),
(13, 'policy:manage',      'Manage loan policies'),
(14, 'audit:read',         'View audit logs'),
(15, 'fine:read',          'View fines and payments');

-- ADMIN: all permissions
INSERT INTO role_permissions (role_id, permission_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7),
(1, 8), (1, 9), (1, 10), (1, 11), (1, 12), (1, 13), (1, 14), (1, 15);

-- LIBRARIAN: catalog, member, circulation, fines (read + collect), reports
INSERT INTO role_permissions (role_id, permission_id) VALUES
(2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 6), (2, 7),
(2, 8), (2, 9), (2, 11), (2, 15);

-- FACULTY: read catalog, own loans/reservations
INSERT INTO role_permissions (role_id, permission_id) VALUES
(3, 1), (3, 7), (3, 8);

-- STUDENT: read catalog, own reservations
INSERT INTO role_permissions (role_id, permission_id) VALUES
(4, 1), (4, 8);

-- Loan Policies
INSERT INTO loan_policies (id, member_type, max_loans, loan_days, max_renewals, max_reservations,
                           fine_per_day, fine_cap, block_fine_threshold, hold_hours, updated_at, version)
VALUES
(1, 'STUDENT', 3, 14, 2, 3, 5.00, 200.00, 100.00, 48, NOW(6), 0),
(2, 'FACULTY', 10, 30, 3, 10, 2.00, 200.00, 300.00, 48, NOW(6), 0);

-- Default Categories
INSERT INTO categories (id, name, slug, parent_id, description) VALUES
(1,  'Fiction',           'fiction',            NULL, 'Novels, short stories, and literary fiction'),
(2,  'Non-Fiction',       'non-fiction',        NULL, 'Factual and informational works'),
(3,  'Science',           'science',            2,    'Natural and physical sciences'),
(4,  'Technology',        'technology',         2,    'Computer science, engineering, and IT'),
(5,  'History',           'history',            2,    'Historical accounts and studies'),
(6,  'Mathematics',       'mathematics',        2,    'Pure and applied mathematics'),
(7,  'Literature',        'literature',         1,    'Classic and modern literature'),
(8,  'Philosophy',        'philosophy',         2,    'Philosophical works and theories'),
(9,  'Arts',              'arts',               NULL, 'Visual arts, music, and performing arts'),
(10, 'Reference',         'reference',          NULL, 'Encyclopedias, dictionaries, and handbooks'),
(11, 'Periodicals',       'periodicals',        NULL, 'Journals, magazines, and newspapers'),
(12, 'Computer Science',  'computer-science',   4,    'Programming, algorithms, and software engineering'),
(13, 'Physics',           'physics',            3,    'Classical and modern physics'),
(14, 'Chemistry',         'chemistry',          3,    'General and organic chemistry'),
(15, 'Biology',           'biology',            3,    'Life sciences and biology');
