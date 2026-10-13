-- Account approval, college identity, configurable registration, and private helpdesk (PostgreSQL).
ALTER TABLE users
  ADD COLUMN first_name VARCHAR(80) NULL,
  ADD COLUMN last_name VARCHAR(80) NULL,
  ADD COLUMN faculty_id VARCHAR(40) NULL,
  ADD COLUMN employee_number VARCHAR(40) NULL,
  ADD COLUMN designation VARCHAR(100) NULL,
  ADD COLUMN department VARCHAR(100) NULL,
  ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED';

CREATE UNIQUE INDEX uk_users_faculty_id ON users(faculty_id);
CREATE UNIQUE INDEX uk_users_employee_number ON users(employee_number);
CREATE INDEX idx_users_approval_status ON users(approval_status, enabled);

CREATE TABLE signup_field_settings (
  id BIGSERIAL PRIMARY KEY,
  account_type VARCHAR(20) NOT NULL,
  field_key VARCHAR(50) NOT NULL,
  label VARCHAR(100) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  required_flag BOOLEAN NOT NULL DEFAULT FALSE,
  sort_order INT NOT NULL DEFAULT 0,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_signup_field_type_key UNIQUE (account_type, field_key)
);
CREATE INDEX idx_signup_field_type_order ON signup_field_settings(account_type, enabled, sort_order);

INSERT INTO signup_field_settings(account_type,field_key,label,enabled,required_flag,sort_order) VALUES
('STUDENT','phoneNumber','Mobile number',TRUE,TRUE,1),
('STUDENT','dateOfBirth','Date of birth',TRUE,TRUE,2),
('STUDENT','admissionNumber','Admission number',TRUE,TRUE,3),
('STUDENT','rollNumber','Roll number',TRUE,TRUE,4),
('STUDENT','branch','Branch / department',TRUE,TRUE,5),
('STUDENT','academicYear','Academic year',TRUE,TRUE,6),
('STUDENT','className','Class / programme',TRUE,TRUE,7),
('STUDENT','section','Section',TRUE,TRUE,8),
('FACULTY','phoneNumber','Mobile number',TRUE,TRUE,1),
('FACULTY','dateOfBirth','Date of birth',TRUE,FALSE,2),
('FACULTY','facultyId','Faculty ID',TRUE,TRUE,3),
('FACULTY','employeeNumber','Employee number',TRUE,FALSE,4),
('FACULTY','department','Department',TRUE,TRUE,5),
('FACULTY','designation','Designation',TRUE,TRUE,6);

SELECT setval(pg_get_serial_sequence('signup_field_settings', 'id'), COALESCE((SELECT MAX(id) FROM signup_field_settings), 1));

CREATE TABLE support_conversations (
  id BIGSERIAL PRIMARY KEY,
  requester_user_id BIGINT NOT NULL,
  subject VARCHAR(160) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  assigned_staff_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  closed_at TIMESTAMP NULL,
  CONSTRAINT fk_support_requester FOREIGN KEY(requester_user_id) REFERENCES users(id),
  CONSTRAINT fk_support_staff FOREIGN KEY(assigned_staff_id) REFERENCES users(id)
);
CREATE INDEX idx_support_requester_status ON support_conversations(requester_user_id,status,updated_at);
CREATE INDEX idx_support_staff_status ON support_conversations(assigned_staff_id,status,updated_at);

CREATE TABLE support_messages (
  id BIGSERIAL PRIMARY KEY,
  conversation_id BIGINT NOT NULL,
  sender_user_id BIGINT NOT NULL,
  body VARCHAR(3000) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  read_at TIMESTAMP NULL,
  CONSTRAINT fk_support_message_conversation FOREIGN KEY(conversation_id) REFERENCES support_conversations(id) ON DELETE CASCADE,
  CONSTRAINT fk_support_message_sender FOREIGN KEY(sender_user_id) REFERENCES users(id)
);
CREATE INDEX idx_support_messages_conversation ON support_messages(conversation_id,created_at);
