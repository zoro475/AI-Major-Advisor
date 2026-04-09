-- ============================================================
-- AI Recommendation Tables
-- Chạy trên database: ai_major_advisor
-- ============================================================

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'recommendation_result')
CREATE TABLE recommendation_result (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    submission_id       BIGINT          NOT NULL,
    ai_summary          NVARCHAR(MAX)   NULL,
    ai_raw_response     NVARCHAR(MAX)   NULL,
    model_used          NVARCHAR(100)   NULL,
    processing_time_ms  BIGINT          NULL,
    created_at          DATETIME2       NOT NULL DEFAULT GETDATE(),

    CONSTRAINT fk_recommendation_submission
        FOREIGN KEY (submission_id) REFERENCES survey_submission(id) ON DELETE CASCADE
);

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'recommendation_item')
CREATE TABLE recommendation_item (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    result_id           BIGINT          NOT NULL,
    major_id            BIGINT          NOT NULL,
    major_name          NVARCHAR(200)   NOT NULL,
    field_name          NVARCHAR(100)   NULL,
    match_score         INT             NOT NULL,
    reason              NVARCHAR(MAX)   NOT NULL,
    career_paths        NVARCHAR(MAX)   NULL,
    salary_range        NVARCHAR(100)   NULL,
    skills_to_improve   NVARCHAR(MAX)   NULL,
    display_order       INT             NOT NULL DEFAULT 0,

    CONSTRAINT fk_item_result FOREIGN KEY (result_id) REFERENCES recommendation_result(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_major  FOREIGN KEY (major_id)  REFERENCES major(id)
);

-- Indexes
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_recommendation_submission')
    CREATE INDEX idx_recommendation_submission ON recommendation_result(submission_id);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_recommendation_item_result')
    CREATE INDEX idx_recommendation_item_result ON recommendation_item(result_id);
