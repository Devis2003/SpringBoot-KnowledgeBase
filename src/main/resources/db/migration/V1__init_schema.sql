CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role VARCHAR(20) NOT NULL DEFAULT 'USER',
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE articles (
                          id BIGSERIAL PRIMARY KEY,
                          title VARCHAR(255) NOT NULL,
                          body TEXT NOT NULL,
                          author_id BIGINT NOT NULL,
                          deleted_at TIMESTAMP NULL,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT fk_articles_author
                              FOREIGN KEY (author_id)
                                  REFERENCES users(id)
                                  ON DELETE CASCADE
);

CREATE TABLE tags (
                      id BIGSERIAL PRIMARY KEY,
                      name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE article_tags (
                              article_id BIGINT NOT NULL,
                              tag_id BIGINT NOT NULL,

                              PRIMARY KEY (article_id, tag_id),

                              CONSTRAINT fk_article_tags_article
                                  FOREIGN KEY (article_id)
                                      REFERENCES articles(id)
                                      ON DELETE CASCADE,

                              CONSTRAINT fk_article_tags_tag
                                  FOREIGN KEY (tag_id)
                                      REFERENCES tags(id)
                                      ON DELETE CASCADE
);

CREATE TABLE refresh_tokens (
                                id BIGSERIAL PRIMARY KEY,
                                token VARCHAR(500) NOT NULL UNIQUE,
                                user_id BIGINT NOT NULL,
                                expires_at TIMESTAMP NOT NULL,
                                revoked BOOLEAN NOT NULL DEFAULT FALSE,
                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT fk_refresh_tokens_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users(id)
                                        ON DELETE CASCADE
);

CREATE INDEX idx_articles_author_id ON articles(author_id);
CREATE INDEX idx_articles_deleted_at ON articles(deleted_at);
CREATE INDEX idx_tags_name ON tags(name);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);