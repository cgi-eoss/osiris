-- recreate all functions due to type misnames

CREATE OR REPLACE FUNCTION osiris_costing_expressions_type_cast(VARCHAR)
  RETURNS osiris_costing_expressions_type AS $$ SELECT
                                                ('' || $1) :: osiris_costing_expressions_type $$ LANGUAGE SQL IMMUTABLE;

CREATE OR REPLACE FUNCTION osiris_credentials_type_cast(VARCHAR)
  RETURNS osiris_credentials_type AS $$ SELECT ('' || $1) :: osiris_credentials_type $$ LANGUAGE SQL IMMUTABLE;

CREATE OR REPLACE FUNCTION osiris_data_sources_policy_cast(VARCHAR)
  RETURNS osiris_data_sources_policy AS $$ SELECT ('' || $1) :: osiris_data_sources_policy $$ LANGUAGE SQL IMMUTABLE;

CREATE OR REPLACE FUNCTION osiris_files_type_cast(VARCHAR)
  RETURNS osiris_files_type AS $$ SELECT ('' || $1) :: osiris_files_type $$ LANGUAGE SQL IMMUTABLE;

CREATE OR REPLACE FUNCTION osiris_jobs_status_cast(VARCHAR)
  RETURNS osiris_jobs_status AS $$ SELECT ('' || $1) :: osiris_jobs_status $$ LANGUAGE SQL IMMUTABLE;

CREATE OR REPLACE FUNCTION osiris_publishing_requests_object_type_cast(VARCHAR)
  RETURNS osiris_publishing_requests_object_type AS $$ SELECT ('' ||
                                                             $1) :: osiris_publishing_requests_object_type $$ LANGUAGE SQL IMMUTABLE;

CREATE OR REPLACE FUNCTION osiris_publishing_requests_status_cast(VARCHAR)
  RETURNS osiris_publishing_requests_status AS $$ SELECT ('' ||
                                                        $1) :: osiris_publishing_requests_status $$ LANGUAGE SQL IMMUTABLE;

CREATE OR REPLACE FUNCTION osiris_roles_cast(VARCHAR)
  RETURNS osiris_roles AS $$ SELECT ('' || $1) :: osiris_roles $$ LANGUAGE SQL IMMUTABLE;

CREATE OR REPLACE FUNCTION osiris_services_licence_cast(VARCHAR)
  RETURNS osiris_services_licence AS $$ SELECT ('' || $1) :: osiris_services_licence $$ LANGUAGE SQL IMMUTABLE;

CREATE OR REPLACE FUNCTION osiris_services_status_cast(VARCHAR)
  RETURNS osiris_services_status AS $$ SELECT ('' || $1) :: osiris_services_status $$ LANGUAGE SQL IMMUTABLE;

CREATE OR REPLACE FUNCTION osiris_services_type_cast(VARCHAR)
  RETURNS osiris_services_type AS $$ SELECT ('' || $1) :: osiris_services_type $$ LANGUAGE SQL IMMUTABLE;

CREATE OR REPLACE FUNCTION osiris_wallet_transactions_type_cast(VARCHAR)
  RETURNS osiris_wallet_transactions_type AS $$ SELECT
                                                ('' || $1) :: osiris_wallet_transactions_type $$ LANGUAGE SQL IMMUTABLE;
