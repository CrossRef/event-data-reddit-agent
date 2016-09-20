CREATE TABLE seen_reddit_item (
  id INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
  reddit_id VARCHAR(1024),
  url VARCHAR(1024),
  seen DATETIME
) ENGINE=InnoDB CHARACTER SET=utf8mb4;

-- Typical reddit IDs are ~6 characters
-- Typical URLs are ~50 characters.
CREATE INDEX seen_id ON seen_reddit_item(reddit_id(7), url(128));
