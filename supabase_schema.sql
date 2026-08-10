-- SUPABASE SQL DATABASE SCHEMA DEFINITION
-- Copy and paste this directly into your Supabase SQL Editor (https://supabase.com)
-- This table matches the model used in the Android application exactly.

CREATE TABLE IF NOT EXISTS students (
    sl_no TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    
    -- Parents Details
    parent_name TEXT,
    parent_phone TEXT NOT NULL,
    parent_flat_no TEXT,
    parent_email TEXT,
    
    -- Student Details
    standard TEXT,
    school TEXT,
    board TEXT,
    student_email TEXT,
    subjects_json TEXT DEFAULT '[]', -- JSON string mapping subject names & test scores
    
    -- Attendance status maps
    attendance_json TEXT DEFAULT '{}', -- JSON string mapping 'yyyy-MM-dd' to boolean (present)
    
    -- Files / Upload structures
    test_papers_json TEXT DEFAULT '[]', -- JSON string mapping test papers & image upload signatures
    reminders_json TEXT DEFAULT '[]', -- JSON string mapping upcoming test dates & pay alarms
    
    -- Syllabus info
    syllabus TEXT DEFAULT '',
    
    -- Accounting
    is_fees_paid BOOLEAN DEFAULT false,
    tuition_fee DOUBLE PRECISION DEFAULT 0.0,
    due_amount DOUBLE PRECISION DEFAULT 0.0,
    demo_fee DOUBLE PRECISION DEFAULT 0.0,
    
    -- Timestamp
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Index parent_phone for blazing-fast login verification lookups
CREATE INDEX IF NOT EXISTS idx_students_parent_phone ON students(parent_phone);

-- (Optional) Setup row level security (RLS) - for simplicity during prototyping, you can keep read/write public, 
-- or write custom policies. Here we enable simple public read/write if you don't use strict RLS,
-- but the app handles authentication checks securely in-code based on phone verify.
ALTER TABLE students ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow public read/write access"
ON students FOR ALL
TO public
USING (true)
WITH CHECK (true);

-- ====================================================================
-- SESSION SCHEDULES TABLE
-- ====================================================================
CREATE TABLE IF NOT EXISTS session_schedules (
    id TEXT PRIMARY KEY,
    student_sl_no TEXT NOT NULL REFERENCES students(sl_no) ON DELETE CASCADE,
    topic TEXT NOT NULL,
    date TEXT NOT NULL,
    time TEXT DEFAULT '',
    duration_minutes INTEGER DEFAULT 60,
    is_completed BOOLEAN DEFAULT false,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Index foreign keys for blazing-fast lookups
CREATE INDEX IF NOT EXISTS idx_session_schedules_student ON session_schedules(student_sl_no);

-- Enable Row Level Security (RLS)
ALTER TABLE session_schedules ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow public read/write access on session_schedules"
ON session_schedules FOR ALL
TO public
USING (true)
WITH CHECK (true);

-- ====================================================================
-- ATTENDANCE RECORDS TABLE (for granular record keeping)
-- ====================================================================
CREATE TABLE IF NOT EXISTS attendance_records (
    id TEXT PRIMARY KEY,
    student_sl_no TEXT NOT NULL REFERENCES students(sl_no) ON DELETE CASCADE,
    date TEXT NOT NULL,
    is_present BOOLEAN NOT NULL DEFAULT false,
    remarks TEXT DEFAULT '',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Index foreign keys for blazing-fast lookups
CREATE INDEX IF NOT EXISTS idx_attendance_records_student ON attendance_records(student_sl_no);

-- Enable Row Level Security (RLS)
ALTER TABLE attendance_records ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow public read/write access on attendance_records"
ON attendance_records FOR ALL
TO public
USING (true)
WITH CHECK (true);

