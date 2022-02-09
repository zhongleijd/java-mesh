CREATE OR REPLACE FUNCTION function_set_nuser_updated_time()
RETURNS TRIGGER
AS $$
BEGIN
  new.last_modified_date = CURRENT_TIMESTAMP;
RETURN new;
END; $$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION function_set_perf_test_updated_time()
RETURNS TRIGGER
AS $$
BEGIN
  new.last_modified_date = CURRENT_TIMESTAMP;
RETURN new;
END; $$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION function_set_tag_updated_time()
RETURNS TRIGGER
AS $$
BEGIN
  new.last_modified_date = CURRENT_TIMESTAMP;
RETURN new;
END; $$ LANGUAGE plpgsql;
