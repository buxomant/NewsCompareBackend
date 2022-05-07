INSERT INTO google_search_term(term)
VALUES('news') ON CONFLICT DO NOTHING;