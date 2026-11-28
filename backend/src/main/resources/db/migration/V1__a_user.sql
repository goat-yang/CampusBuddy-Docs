-- Owner A. User, tag, refresh token, idempotency.
-- Activity tables belong to B and must use a later Flyway version.

CREATE TABLE t_user (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  campus_id VARCHAR(20) NULL,
  school_email VARCHAR(128) NULL,
  openid VARCHAR(64) NULL,
  phone VARCHAR(32) NULL,
  password_hash VARCHAR(100) NULL,
  nickname VARCHAR(20) NOT NULL,
  avatar VARCHAR(512) NULL,
  college VARCHAR(64) NULL,
  grade VARCHAR(32) NULL,
  major VARCHAR(64) NULL,
  campus VARCHAR(16) NULL,
  bio VARCHAR(200) NULL,
  credit_score INT NOT NULL DEFAULT 100,
  user_role VARCHAR(16) NOT NULL,
  affiliation VARCHAR(8) NULL,
  account_status VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_user_campus ON t_user (campus_id);
CREATE UNIQUE INDEX uk_user_email ON t_user (school_email);
CREATE UNIQUE INDEX uk_user_openid ON t_user (openid);
CREATE UNIQUE INDEX uk_user_phone ON t_user (phone);

CREATE TABLE t_tag (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(32) NOT NULL,
  category VARCHAR(32) NOT NULL,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE t_user_tag (
  user_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  sort_no INT NOT NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, tag_id)
);

CREATE TABLE t_refresh_token (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  jti VARCHAR(36) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  revoked BOOLEAN NOT NULL DEFAULT FALSE,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_refresh_jti ON t_refresh_token (jti);
CREATE INDEX idx_refresh_user ON t_refresh_token (user_id);

CREATE TABLE t_idempotency (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  idem_key VARCHAR(36) NOT NULL,
  path VARCHAR(160) NOT NULL,
  body_hash CHAR(64) NOT NULL,
  http_status INT NOT NULL,
  response_body LONGTEXT NOT NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_idem_user_path_key ON t_idempotency (user_id, path, idem_key);

INSERT INTO t_tag (id, name, category, deleted) VALUES
  (1, '拼饭', 'MEAL', FALSE),
  (2, '跑步', 'SPORT', FALSE),
  (3, '自习', 'STUDY', FALSE),
  (4, '出游', 'TRIP', FALSE),
  (5, '竞赛', 'CONTEST', FALSE),
  (6, '桌游', 'FUN', FALSE),
  (7, '篮球', 'SPORT', FALSE),
  (8, '羽毛球', 'SPORT', FALSE),
  (9, '图书馆', 'STUDY', FALSE),
  (10, '电影', 'FUN', FALSE),
  (11, '骑行', 'SPORT', FALSE),
  (12, '其他', 'OTHER', FALSE);
