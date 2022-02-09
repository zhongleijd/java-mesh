CREATE TRIGGER trigger_set_nuser_updated_time BEFORE UPDATE ON nuser FOR EACH ROW EXECUTE PROCEDURE function_set_nuser_updated_time();

CREATE TRIGGER trigger_set_perf_test_updated_time BEFORE UPDATE ON perf_test FOR EACH ROW EXECUTE PROCEDURE function_set_perf_test_updated_time();

CREATE TRIGGER trigger_set_tag_updated_time BEFORE UPDATE ON tag FOR EACH ROW EXECUTE PROCEDURE function_set_tag_updated_time();
