ALTER TABLE articles
    ADD COLUMN search_vector tsvector;

UPDATE articles
SET search_vector =
        to_tsvector('english', coalesce(title, '') || ' ' || coalesce(body, ''));

CREATE INDEX idx_articles_search_vector
    ON articles
    USING GIN (search_vector);


CREATE FUNCTION articles_search_vector_update()
    RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        to_tsvector('english', coalesce(NEW.title, '') || ' ' || coalesce(NEW.body, ''));
RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER articles_search_vector_trigger
    BEFORE INSERT OR UPDATE ON articles
                         FOR EACH ROW
                         EXECUTE FUNCTION articles_search_vector_update();