CREATE TABLE faq (
  faq_id VARCHAR(20) NOT NULL,
  category_code VARCHAR(20) NOT NULL,
  question VARCHAR(300) NOT NULL,
  answer VARCHAR(4000) NOT NULL,
  sort_order INT NOT NULL,
  use_yn CHAR(1) NOT NULL,
  writer_id VARCHAR(50) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_by VARCHAR(50),
  updated_at DATETIME,
  PRIMARY KEY (faq_id)
);
