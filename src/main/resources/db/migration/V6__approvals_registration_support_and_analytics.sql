-- Account approval, college identity, configurable registration, and private helpdesk.
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
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  account_type VARCHAR(20) NOT NULL,
  field_key VARCHAR(50) NOT NULL,
  label VARCHAR(100) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  required_flag BOOLEAN NOT NULL DEFAULT FALSE,
  sort_order INT NOT NULL DEFAULT 0,
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_signup_field_type_key (account_type, field_key),
  INDEX idx_signup_field_type_order (account_type, enabled, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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

CREATE TABLE support_conversations (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  requester_user_id BIGINT NOT NULL,
  subject VARCHAR(160) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  assigned_staff_id BIGINT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  closed_at DATETIME(6) NULL,
  CONSTRAINT fk_support_requester FOREIGN KEY(requester_user_id) REFERENCES users(id),
  CONSTRAINT fk_support_staff FOREIGN KEY(assigned_staff_id) REFERENCES users(id),
  INDEX idx_support_requester_status(requester_user_id,status,updated_at),
  INDEX idx_support_staff_status(assigned_staff_id,status,updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE support_messages (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  conversation_id BIGINT NOT NULL,
  sender_user_id BIGINT NOT NULL,
  body VARCHAR(3000) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  read_at DATETIME(6) NULL,
  CONSTRAINT fk_support_message_conversation FOREIGN KEY(conversation_id) REFERENCES support_conversations(id) ON DELETE CASCADE,
  CONSTRAINT fk_support_message_sender FOREIGN KEY(sender_user_id) REFERENCES users(id),
  INDEX idx_support_messages_conversation(conversation_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
