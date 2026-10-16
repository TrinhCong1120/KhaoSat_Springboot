--
-- PostgreSQL database dump
--

\restrict 9rXS96TG4Fwfdd7mJaQWBdEciZHdoLxME9jH6mBn4ej3jehHnqMaQCVhEnOeuVw

-- Dumped from database version 16.15
-- Dumped by pg_dump version 16.15

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA IF NOT EXISTS public;


--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON SCHEMA public IS 'standard public schema';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: answer_options; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.answer_options (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    answer_id uuid,
    option_id uuid
);


--
-- Name: answers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.answers (
    answer_date timestamp(6) without time zone,
    answer_number numeric(38,2),
    answer_text text,
    province character varying(255),
    province_code character varying(255),
    ward character varying(255),
    ward_code character varying(255),
    address_detail text,
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    question_id uuid,
    response_id uuid
);


--
-- Name: conditions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.conditions (
    action character varying(255) NOT NULL,
    source_value text NOT NULL,
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    source_question_id uuid NOT NULL,
    target_question_id uuid NOT NULL
);


--
-- Name: functions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.functions (
    code character varying(255),
    name character varying(255),
    id uuid DEFAULT gen_random_uuid() NOT NULL
);


--
-- Name: media_files; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.media_files (
    bucket_name character varying(255) NOT NULL,
    content_type character varying(255),
    media_type character varying(50) NOT NULL,
    object_key character varying(1000) NOT NULL,
    object_url text NOT NULL,
    original_filename character varying(500),
    owner_type character varying(50) NOT NULL,
    size_bytes bigint,
    uploaded_at timestamp(6) without time zone NOT NULL,
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    created_by_user_id uuid NOT NULL,
    owner_id uuid NOT NULL,
    CONSTRAINT chk_media_files_media_type CHECK (((media_type)::text = ANY (ARRAY[('IMAGE'::character varying)::text, ('VIDEO'::character varying)::text, ('AUDIO'::character varying)::text]))),
    CONSTRAINT chk_media_files_owner_type CHECK (((owner_type)::text = ANY (ARRAY[('SURVEY'::character varying)::text, ('PAGE'::character varying)::text, ('QUESTION'::character varying)::text, ('OPTION'::character varying)::text])))
);


--
-- Name: menus; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.menus (
    created_at timestamp(6) without time zone,
    icon character varying(255),
    name character varying(255) NOT NULL,
    order_index integer,
    path character varying(255),
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    function_id uuid,
    parent_id uuid
);


--
-- Name: options; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.options (
    option_text text,
    order_index integer,
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    question_id uuid
);


--
-- Name: pages; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pages (
    order_index integer,
    title character varying(500),
    description text,
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    survey_id uuid
);


--
-- Name: permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.permissions (
    code character varying(255) NOT NULL,
    type character varying(16) NOT NULL CHECK (type IN ('VIEW', 'CREATE', 'UPDATE', 'DELETE', 'OTHER')),
    http_method character varying(255) NOT NULL,
    is_active boolean,
    name character varying(255) NOT NULL,
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    function_id uuid NOT NULL
);


--
-- Name: question_types; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.question_types (
    code character varying(255),
    name character varying(255),
    id uuid DEFAULT gen_random_uuid() NOT NULL
);


--
-- Name: question_validation_rule_values; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.question_validation_rule_values (
    rule_code character varying(64) NOT NULL,
    parameter_name character varying(64) DEFAULT 'value'::character varying NOT NULL,
    group_index integer DEFAULT 0 NOT NULL,
    "position" integer DEFAULT 0 NOT NULL,
    text_value text,
    number_value numeric(38,2),
    date_value date,
    question_id uuid NOT NULL,
    option_id uuid,
    CONSTRAINT chk_rule_parameter_name CHECK (((parameter_name)::text ~ '^[a-z][a-z0-9_]*$'::text)),
    CONSTRAINT chk_rule_value_exactly_one CHECK ((num_nonnulls(text_value, number_value, date_value, option_id) = 1)),
    CONSTRAINT question_validation_rule_values_group_index_check CHECK ((group_index >= 0)),
    CONSTRAINT question_validation_rule_values_position_check CHECK (("position" >= 0))
);


--
-- Name: question_validation_rules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.question_validation_rules (
    rule_code character varying(255) NOT NULL,
    error_message text,
    is_active boolean DEFAULT true NOT NULL,
    order_index integer DEFAULT 0 NOT NULL,
    question_id uuid NOT NULL,
    CONSTRAINT question_validation_rules_order_index_check CHECK ((order_index >= 0))
);


--
-- Name: questions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.questions (
    description text,
    is_required boolean,
    order_index integer,
    question_text text,
    validation_rules jsonb DEFAULT '[]'::jsonb NOT NULL,
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    page_id uuid,
    question_type_id uuid
);


--
-- Name: responses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.responses (
    request_id uuid NOT NULL,
    submitted_at timestamp(6) without time zone,
    token character varying(500),
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    survey_id uuid
);


--
-- Name: role_permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.role_permissions (
    role_id uuid NOT NULL,
    permission_id uuid NOT NULL
);


--
-- Name: roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.roles (
    name character varying(255) NOT NULL,
    id uuid DEFAULT gen_random_uuid() NOT NULL
);


--
-- Name: surveys; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.surveys (
    created_at timestamp(6) without time zone,
    creator_user character varying(255),
    creator_user_id uuid,
    description text,
    is_active boolean,
    title character varying(500),
    validation_revision bigint DEFAULT 1 NOT NULL,
    id uuid DEFAULT gen_random_uuid() NOT NULL
);


--
-- Name: user_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_roles (
    user_id uuid NOT NULL,
    role_id uuid NOT NULL
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    email character varying,
    is_active boolean,
    password_hash character varying(255) NOT NULL,
    username character varying(255) NOT NULL,
    telegram_chat_id bigint,
    id uuid DEFAULT gen_random_uuid() NOT NULL
);

CREATE UNIQUE INDEX users_telegram_chat_id_unique
    ON public.users (telegram_chat_id)
    WHERE telegram_chat_id IS NOT NULL;



--
-- Name: validation_rule_type_question_types; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.validation_rule_type_question_types (
    rule_code character varying(255) NOT NULL,
    question_type_code character varying(64) NOT NULL
);


--
-- Name: validation_rule_types; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.validation_rule_types (
    code character varying(64) NOT NULL,
    name character varying(150) NOT NULL,
    description text,
    is_active boolean DEFAULT true NOT NULL,
    CONSTRAINT chk_rule_type_code CHECK (((code)::text ~ '^[A-Z][A-Z0-9_]*$'::text))
);


--
-- Name: answer_options answer_options_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answer_options
    ADD CONSTRAINT answer_options_pkey PRIMARY KEY (id);


--
-- Name: answers answers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answers
    ADD CONSTRAINT answers_pkey PRIMARY KEY (id);


--
-- Name: conditions conditions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.conditions
    ADD CONSTRAINT conditions_pkey PRIMARY KEY (id);


--
-- Name: functions functions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.functions
    ADD CONSTRAINT functions_pkey PRIMARY KEY (id);


--
-- Name: media_files media_files_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.media_files
    ADD CONSTRAINT media_files_pkey PRIMARY KEY (id);


--
-- Name: menus menus_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.menus
    ADD CONSTRAINT menus_pkey PRIMARY KEY (id);


--
-- Name: options options_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.options
    ADD CONSTRAINT options_pkey PRIMARY KEY (id);


--
-- Name: pages pages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pages
    ADD CONSTRAINT pages_pkey PRIMARY KEY (id);


--
-- Name: permissions permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_pkey PRIMARY KEY (id);


--
-- Name: question_types question_types_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_types
    ADD CONSTRAINT question_types_pkey PRIMARY KEY (id);


--
-- Name: question_validation_rule_values question_validation_rule_values_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_validation_rule_values
    ADD CONSTRAINT question_validation_rule_values_pkey PRIMARY KEY (question_id, rule_code, parameter_name, group_index, "position");


--
-- Name: question_validation_rules question_validation_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_validation_rules
    ADD CONSTRAINT question_validation_rules_pkey PRIMARY KEY (question_id, rule_code);


--
-- Name: questions questions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.questions
    ADD CONSTRAINT questions_pkey PRIMARY KEY (id);


--
-- Name: responses responses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.responses
    ADD CONSTRAINT responses_pkey PRIMARY KEY (id);


--
-- Name: role_permissions role_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_pkey PRIMARY KEY (role_id, permission_id);


--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);


--
-- Name: surveys surveys_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.surveys
    ADD CONSTRAINT surveys_pkey PRIMARY KEY (id);


--
-- Name: user_roles user_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

CREATE TABLE public.survey_access (
    id uuid DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
    survey_id uuid NOT NULL REFERENCES public.surveys(id) ON DELETE CASCADE,
    user_id uuid NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    can_view boolean DEFAULT true NOT NULL,
    can_edit boolean DEFAULT false NOT NULL,
    CONSTRAINT uq_survey_access_user UNIQUE (survey_id, user_id),
    CONSTRAINT chk_survey_access_view CHECK (can_view)
);

CREATE INDEX ix_survey_access_user ON public.survey_access (user_id, survey_id);
CREATE INDEX ix_surveys_creator_user_id ON public.surveys (creator_user_id);

ALTER TABLE public.surveys
    ADD CONSTRAINT fk_surveys_creator_user_id FOREIGN KEY (creator_user_id)
    REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: validation_rule_type_question_types validation_rule_type_question_types_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.validation_rule_type_question_types
    ADD CONSTRAINT validation_rule_type_question_types_pkey PRIMARY KEY (rule_code, question_type_code);


--
-- Name: validation_rule_types validation_rule_types_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.validation_rule_types
    ADD CONSTRAINT validation_rule_types_pkey PRIMARY KEY (code);


--
-- Name: idx_media_files_created_by_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_media_files_created_by_user_id ON public.media_files USING btree (created_by_user_id);


--
-- Name: idx_media_files_owner; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_media_files_owner ON public.media_files USING btree (owner_type, owner_id, uploaded_at DESC);


--
-- Name: idx_media_files_owner_media; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_media_files_owner_media ON public.media_files USING btree (owner_type, owner_id, media_type, uploaded_at DESC);


--
-- Name: idx_question_validation_rule_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_question_validation_rule_code ON public.question_validation_rules USING btree (rule_code);


--
-- Name: idx_validation_rule_type_question_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_validation_rule_type_question_type ON public.validation_rule_type_question_types USING btree (question_type_code);


--
-- Name: idx_validation_values_option; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_validation_values_option ON public.question_validation_rule_values USING btree (option_id) WHERE (option_id IS NOT NULL);


--
-- Name: ux_question_types_code; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_question_types_code ON public.question_types USING btree (code);


--
-- Name: answers fk3erw1a3t0r78st8ty27x6v3g1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answers
    ADD CONSTRAINT fk3erw1a3t0r78st8ty27x6v3g1 FOREIGN KEY (question_id) REFERENCES public.questions(id);


--
-- Name: options fk5bmv46so2y5igt9o9n9w4fh6y; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.options
    ADD CONSTRAINT fk5bmv46so2y5igt9o9n9w4fh6y FOREIGN KEY (question_id) REFERENCES public.questions(id);


--
-- Name: pages fk66x4dlxofxed48ysy5rvvutd9; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pages
    ADD CONSTRAINT fk66x4dlxofxed48ysy5rvvutd9 FOREIGN KEY (survey_id) REFERENCES public.surveys(id);


--
-- Name: answer_options fk9hu9eir3fpbcbq83yxmqo0roh; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answer_options
    ADD CONSTRAINT fk9hu9eir3fpbcbq83yxmqo0roh FOREIGN KEY (answer_id) REFERENCES public.answers(id);


--
-- Name: validation_rule_type_question_types fk_validation_rule_type_question_types_question_type; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.validation_rule_type_question_types
    ADD CONSTRAINT fk_validation_rule_type_question_types_question_type FOREIGN KEY (question_type_code) REFERENCES public.question_types(code) ON DELETE RESTRICT;


--
-- Name: validation_rule_type_question_types fk_validation_rule_type_question_types_rule; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.validation_rule_type_question_types
    ADD CONSTRAINT fk_validation_rule_type_question_types_rule FOREIGN KEY (rule_code) REFERENCES public.validation_rule_types(code) ON DELETE CASCADE;


--
-- Name: menus fkcctt24h6nix02cxipt9rbqtnc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.menus
    ADD CONSTRAINT fkcctt24h6nix02cxipt9rbqtnc FOREIGN KEY (parent_id) REFERENCES public.menus(id);


--
-- Name: questions fkdtjkqt2uaig00ai58kc0i6p54; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.questions
    ADD CONSTRAINT fkdtjkqt2uaig00ai58kc0i6p54 FOREIGN KEY (page_id) REFERENCES public.pages(id);


--
-- Name: answers fkdvrc91061f1tdfkl8wv77k4t3; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answers
    ADD CONSTRAINT fkdvrc91061f1tdfkl8wv77k4t3 FOREIGN KEY (response_id) REFERENCES public.responses(id);


--
-- Name: questions fkdyyji0ey6u2yifjdt8ybu3tyw; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.questions
    ADD CONSTRAINT fkdyyji0ey6u2yifjdt8ybu3tyw FOREIGN KEY (question_type_id) REFERENCES public.question_types(id);


--
-- Name: role_permissions fkegdk29eiy7mdtefy5c7eirr6e; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT fkegdk29eiy7mdtefy5c7eirr6e FOREIGN KEY (permission_id) REFERENCES public.permissions(id);


--
-- Name: responses fkemug20qu5fygiy69wj7sel8hc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.responses
    ADD CONSTRAINT fkemug20qu5fygiy69wj7sel8hc FOREIGN KEY (survey_id) REFERENCES public.surveys(id);


--
-- Name: menus fkfe3jd0uqjpc0iqgw9pbcuhs29; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.menus
    ADD CONSTRAINT fkfe3jd0uqjpc0iqgw9pbcuhs29 FOREIGN KEY (function_id) REFERENCES public.functions(id);


--
-- Name: permissions fkgfomuvpjtwmtksfgvnau8i1ah; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT fkgfomuvpjtwmtksfgvnau8i1ah FOREIGN KEY (function_id) REFERENCES public.functions(id);


--
-- Name: user_roles fkh8ciramu9cc9q3qcqiv4ue8a6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT fkh8ciramu9cc9q3qcqiv4ue8a6 FOREIGN KEY (role_id) REFERENCES public.roles(id);


--
-- Name: user_roles fkhfh9dx7w3ubf1co1vdev94g3f; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT fkhfh9dx7w3ubf1co1vdev94g3f FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: conditions fkksxuheoy4pg8ljlgxvo0di5o7; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.conditions
    ADD CONSTRAINT fkksxuheoy4pg8ljlgxvo0di5o7 FOREIGN KEY (target_question_id) REFERENCES public.questions(id);


--
-- Name: role_permissions fkn5fotdgk8d1xvo8nav9uv3muc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT fkn5fotdgk8d1xvo8nav9uv3muc FOREIGN KEY (role_id) REFERENCES public.roles(id);


--
-- Name: answer_options fkpaub45j3djvy5md5yph6qhv0o; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.answer_options
    ADD CONSTRAINT fkpaub45j3djvy5md5yph6qhv0o FOREIGN KEY (option_id) REFERENCES public.options(id);


--
-- Name: conditions fktqigslc6g0vkn3l2trt93rorq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.conditions
    ADD CONSTRAINT fktqigslc6g0vkn3l2trt93rorq FOREIGN KEY (source_question_id) REFERENCES public.questions(id);


--
-- Name: question_validation_rule_values question_validation_rule_values_option_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_validation_rule_values
    ADD CONSTRAINT question_validation_rule_values_option_id_fkey FOREIGN KEY (option_id) REFERENCES public.options(id) ON DELETE RESTRICT;


--
-- Name: question_validation_rule_values question_validation_rule_values_question_id_rule_code_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_validation_rule_values
    ADD CONSTRAINT question_validation_rule_values_question_id_rule_code_fkey FOREIGN KEY (question_id, rule_code) REFERENCES public.question_validation_rules(question_id, rule_code) ON DELETE CASCADE;


--
-- Name: question_validation_rules question_validation_rules_question_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_validation_rules
    ADD CONSTRAINT question_validation_rules_question_id_fkey FOREIGN KEY (question_id) REFERENCES public.questions(id) ON DELETE CASCADE;


--
-- Name: question_validation_rules question_validation_rules_rule_code_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.question_validation_rules
    ADD CONSTRAINT question_validation_rules_rule_code_fkey FOREIGN KEY (rule_code) REFERENCES public.validation_rule_types(code) ON DELETE RESTRICT;


--
-- PostgreSQL database dump complete
--

\unrestrict 9rXS96TG4Fwfdd7mJaQWBdEciZHdoLxME9jH6mBn4ej3jehHnqMaQCVhEnOeuVw
