-- College profile fields and server-side AI provider settings.
ALTER TABLE users
  ADD COLUMN admission_number VARCHAR(40) NULL,
  ADD COLUMN roll_number VARCHAR(40) NULL,
  ADD COLUMN branch VARCHAR(80) NULL,
  ADD COLUMN academic_year VARCHAR(30) NULL,
  ADD COLUMN class_name VARCHAR(50) NULL,
  ADD COLUMN section_name VARCHAR(20) NULL,
  ADD COLUMN date_of_birth DATE NULL,
  ADD COLUMN phone_number VARCHAR(30) NULL,
  ADD COLUMN profile_updated_at TIMESTAMP NULL;
CREATE UNIQUE INDEX uk_users_admission_number ON users(admission_number);
CREATE UNIQUE INDEX uk_users_roll_number ON users(roll_number);
CREATE TABLE app_settings (setting_key VARCHAR(100) NOT NULL PRIMARY KEY, setting_value TEXT NULL, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP);
