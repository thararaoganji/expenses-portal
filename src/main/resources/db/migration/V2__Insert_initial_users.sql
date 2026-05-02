-- In this example, the password for 'user_employee' is 'password-employee',
-- for 'user_manager' is 'password-manager', and for 'user_finance' is 'password-finance'.
-- The hashes below were generated using BCrypt.
-- IMPORTANT: Replace these hashes with the ones you generated previously.

-- Insert users into app_user table
INSERT INTO app_user (username, password, name, email) VALUES
(
    'user_employee',
    '$2a$10$hSLfCb85kHTR0/5F13NWsOr4IYHr.LRVGZ0PwJoQnU4Ki/2yUfny2', -- password-employee
    'Employee User',
    'employee@example.com'
),
(
    'user_manager',
    '$2a$10$Js/Lo2EFRmVxAfX8r7ETdurD2VP9sSHszVp00m2yGp2n.5ml0BDA.', -- password-manager
    'Manager User',
    'manager@example.com'
),
(
    'user_finance',
    '$2a$10$c0fQrzfBdwdMUKyy4iqU/O9Ncl0xgwgBgwElcUE2m.jFwRoQ1ybEi', -- password-finance
    'Finance User',
    'finance@example.com'
);

-- Link users to their roles in the join table
INSERT INTO app_user_roles (user_id, role_id) VALUES
((SELECT id FROM app_user WHERE username = 'user_employee'), (SELECT id FROM role WHERE name = 'ROLE_EMPLOYEE')),
((SELECT id FROM app_user WHERE username = 'user_manager'), (SELECT id FROM role WHERE name = 'ROLE_MANAGER')),
((SELECT id FROM app_user WHERE username = 'user_finance'), (SELECT id FROM role WHERE name = 'ROLE_FINANCE'));
