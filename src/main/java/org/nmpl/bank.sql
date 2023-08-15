-- Create the bankdb database if it doesn't exist
CREATE DATABASE IF NOT EXISTS bankdb;

-- Use the bankdb database
USE bankdb;

-- Create the User_Account_Master table
CREATE TABLE User_Account_Master (
                                     id INT PRIMARY KEY AUTO_INCREMENT,
                                     username VARCHAR(50) NOT NULL,
                                     password VARCHAR(50) NOT NULL,
                                     balance DECIMAL(10, 2) DEFAULT 0.00
);

-- Create the Transaction_Master table
CREATE TABLE Transaction_Master (
                                    id INT PRIMARY KEY AUTO_INCREMENT,
                                    user_id INT,
                                    transaction_type VARCHAR(20),
                                    amount DECIMAL(10, 2),
                                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    FOREIGN KEY (user_id) REFERENCES User_Account_Master(id)
);

CREATE TABLE teller_master (
                               teller_id INT AUTO_INCREMENT PRIMARY KEY,
                               teller_name VARCHAR(255) NOT NULL,
                               teller_password VARCHAR(255) NOT NULL
);

INSERT INTO teller_master (teller_name, teller_password)
VALUES
    ('myteller', '1234qwer'),
    ('teller2', 'password2'),
    ('teller3', 'password3');

-- Insert test data into User_Account_Master
INSERT INTO User_Account_Master (username, password, balance) VALUES
                                                                  ('user1', 'password1', 1000.00),
                                                                  ('user2', 'password2', 1500.00);

-- Insert test data into Transaction_Master
INSERT INTO Transaction_Master (user_id, transaction_type, amount) VALUES
                                                                       (1, 'Deposit', 500.00),
                                                                       (2, 'Withdraw', 200.00);