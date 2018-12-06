/* run this in your MYSQL cli before running tests */

CREATE TABLE delete_me (
  id INTEGER NOT NULL PRIMARY KEY,
  name VARCHAR(50),
  price DECIMAL (10,2)
);