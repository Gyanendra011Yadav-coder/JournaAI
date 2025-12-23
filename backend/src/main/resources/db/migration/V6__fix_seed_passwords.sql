UPDATE users
SET password_hash = '{bcrypt}$2a$10$aNNR0UsWj2hsWFWzG1byPOukXfM3rRmhIn92PEi/5u9MwYjTPhcLy'
WHERE email IN ('admin@example.com', 'member@example.com');
