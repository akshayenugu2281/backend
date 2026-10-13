-- V3 used EXCELLENT, which is not a supported CopyCondition enum value.
UPDATE book_copies SET "condition" = 'GOOD' WHERE "condition" = 'EXCELLENT';
