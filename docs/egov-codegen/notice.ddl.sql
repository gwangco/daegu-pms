CREATE TABLE notice (
  notice_id VARCHAR(20) NOT NULL,
  title VARCHAR(200) NOT NULL,
  content TEXT NOT NULL,
  important_yn CHAR(1) NOT NULL,
  view_count INT NOT NULL,
  use_yn CHAR(1) NOT NULL,
  writer_id VARCHAR(50) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_by VARCHAR(50),
  updated_at DATETIME,
  PRIMARY KEY (notice_id)
);
