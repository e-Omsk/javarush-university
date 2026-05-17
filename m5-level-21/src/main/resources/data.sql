-- MERGE вместо INSERT: H2 in-memory база (jdbc:h2:mem:testdb) переживает между
-- @SpringBootTest-контекстами, поэтому повторный запуск data.sql не должен падать
-- на уникальном email. MERGE по KEY(email) – upsert.
MERGE INTO USERS (name, email) KEY (email) VALUES ('Alice', 'alice@example.com');
MERGE INTO USERS (name, email) KEY (email) VALUES ('Bob', 'bob@example.com');
MERGE INTO USERS (name, email) KEY (email) VALUES ('Charlie', 'charlie@example.com');
