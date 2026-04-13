
CREATE TABLE IF NOT EXISTS bills (
    id INT AUTO_INCREMENT PRIMARY KEY,
    citizen_email VARCHAR(255) NOT NULL,
    units INT NOT NULL,
    total_cost DOUBLE NOT NULL,
    due_date DATETIME DEFAULT NULL,        -- Due date track panna
    reminder_sent BOOLEAN DEFAULT FALSE,   -- Reminder anupunama nu check panna
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Security Audit Logs Table (Admin activity track panna optional but recommended)
CREATE TABLE IF NOT EXISTS admin_logs (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    action_performed VARCHAR(255),
    target_email VARCHAR(255),
    action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);