CREATE TABLE roles (
   id SERIAL PRIMARY KEY,
   name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE app_users (
   id SERIAL PRIMARY KEY,
   username VARCHAR(50) UNIQUE NOT NULL,
   password VARCHAR(200) NOT NULL
);

CREATE TABLE user_roles (
    user_id INT REFERENCES app_users(id),
    role_id INT REFERENCES roles(id),
    PRIMARY KEY(user_id, role_id)
);

INSERT INTO roles(name) VALUES ('EMPLOYEE1'), ('MANAGER1'), ('FINANCE1');