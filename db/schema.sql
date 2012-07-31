--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: evec; Type: DATABASE; Schema: -; Owner: evec
--

CREATE DATABASE evec WITH TEMPLATE = template0 ENCODING = 'UTF8' LC_COLLATE = 'en_US.UTF-8' LC_CTYPE = 'en_US.UTF-8';


ALTER DATABASE evec OWNER TO evec;

\connect evec

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

--
-- Name: max(integer, integer); Type: FUNCTION; Schema: public; Owner: evec
--

CREATE FUNCTION max(integer, integer) RETURNS integer
    LANGUAGE plpgsql
    AS $_$     BEGIN
         IF $1 > $2 THEN
             RETURN $1;
         END IF;
         RETURN $2;
     END;
 $_$;


ALTER FUNCTION public.max(integer, integer) OWNER TO evec;

--
-- Name: min(integer, integer); Type: FUNCTION; Schema: public; Owner: evec
--

CREATE FUNCTION min(integer, integer) RETURNS integer
    LANGUAGE plpgsql
    AS $_$
     BEGIN
         IF $1 < $2 THEN
             RETURN $1;
         END IF;
         RETURN $2;
     END;
 $_$;


ALTER FUNCTION public.min(integer, integer) OWNER TO evec;

--
-- Name: min(double precision, double precision); Type: FUNCTION; Schema: public; Owner: evec
--

CREATE FUNCTION min(double precision, double precision) RETURNS double precision
    LANGUAGE plpgsql
    AS $_$     BEGIN
         IF $1 < $2 THEN
             RETURN $1;
         END IF;
         RETURN $2;
     END;$_$;


ALTER FUNCTION public.min(double precision, double precision) OWNER TO evec;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: api_market_transid; Type: TABLE; Schema: public; Owner: evec; Tablespace: 
--

CREATE TABLE api_market_transid (
    userid bigint,
    char_lasttrans bigint DEFAULT 0 NOT NULL,
    corp_lasttrans bigint DEFAULT 0 NOT NULL
);


ALTER TABLE public.api_market_transid OWNER TO evec;

--
-- Name: archive_market; Type: TABLE; Schema: public; Owner: evec; Tablespace: 
--

CREATE TABLE archive_market (
    regionid bigint NOT NULL,
    systemid bigint NOT NULL,
    stationid bigint NOT NULL,
    typeid bigint NOT NULL,
    bid integer DEFAULT 0 NOT NULL,
    price double precision NOT NULL,
    orderid bigint NOT NULL,
    minvolume integer NOT NULL,
    volremain integer NOT NULL,
    volenter integer NOT NULL,
    issued timestamp without time zone NOT NULL,
    duration interval NOT NULL,
    range integer NOT NULL,
    reportedby bigint NOT NULL,
    reportedtime timestamp without time zone DEFAULT now() NOT NULL,
    source text
);


ALTER TABLE public.archive_market OWNER TO evec;

--
-- Name: archive_transactions; Type: TABLE; Schema: public; Owner: evec; Tablespace: 
--

CREATE TABLE archive_transactions (
    userid bigint,
    accountkey integer,
    transtime timestamp without time zone,
    transactionid bigint,
    quantity integer,
    typename character varying(255),
    typeid bigint,
    price numeric,
    clientid bigint,
    clientname character varying(255),
    characterid bigint,
    charactername character varying(255),
    stationid bigint,
    stationname text,
    transactiontype character varying(100),
    corp boolean
);


ALTER TABLE public.archive_transactions OWNER TO evec;

--
-- Name: basket; Type: TABLE; Schema: public; Owner: evec; Tablespace: 
--

CREATE TABLE basket (
    basket character varying(255) NOT NULL,
    typeid bigint NOT NULL,
    weight double precision DEFAULT 1 NOT NULL
);


ALTER TABLE public.basket OWNER TO evec;

--
-- Name: blueprint_types; Type: TABLE; Schema: public; Owner: evec; Tablespace: 
--

CREATE TABLE blueprint_types (
    blueprinttypeid integer,
    parentblueprinttypeid integer,
    producttypeid integer,
    productiontime integer,
    techlevel integer,
    researchproductivitytime integer,
    researchmaterialtime integer,
    researchcopytime integer,
    researchtechtime integer,
    productivitymodifier integer,
    materialmodifier integer,
    wastefactor integer,
    chanceofreverseengineering double precision,
    maxproductionlimit integer
);


ALTER TABLE public.blueprint_types OWNER TO evec;

--
-- Name: constellations; Type: TABLE; Schema: public; Owner: evec; Tablespace: 
--

CREATE TABLE constellations (
    constellationid bigint NOT NULL,
    constellationname text NOT NULL,
    faction bigint NOT NULL,
    regionid bigint NOT NULL
);


ALTER TABLE public.constellations OWNER TO evec;

--
-- Name: current_market; Type: TABLE; Schema: public; Owner: evec; Tablespace: 
--

CREATE TABLE current_market (
    regionid bigint NOT NULL,
    systemid bigint NOT NULL,
    stationid bigint NOT NULL,
    typeid bigint NOT NULL,
    bid integer DEFAULT 0 NOT NULL,
    price double precision NOT NULL,
    orderid bigint NOT NULL,
    minvolume integer NOT NULL,
    volremain integer NOT NULL,
    volenter integer NOT NULL,
    issued date NOT NULL,
    duration interval NOT NULL,
    range integer NOT NULL,
    reportedby bigint NOT NULL,
    reportedtime timestamp without time zone DEFAULT now() NOT NULL,
    source integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.current_market OWNER TO evec;

--
-- Name: jumps; Type: TABLE; Schema: public; Owner: evec; Tablespace: 
--

CREATE TABLE jumps (
    fromregion integer NOT NULL,
    fromconstellation integer NOT NULL,
    fromsystem integer NOT NULL,
    tosystem integer NOT NULL,
    toconstellation integer NOT NULL,
    toregion integer NOT NULL
);


ALTER TABLE public.jumps OWNER TO evec;

--
-- Name: materials_for_activity; Type: TABLE; Schema: public; Owner: evec; Tablespace: 
--

CREATE TABLE materials_for_activity (
    typeid integer,
    activity integer,
    requiredtypeid integer,
    quantity integer,
    damageperjob double precision
);


ALTER TABLE public.materials_for_activity OWNER TO evec;

--
-- Name: regions; Type: TABLE; Schema: public; Owner: evec; Tablespace: 
--

CREATE TABLE regions (
    regionid bigint NOT NULL,
    regionname text NOT NULL
);


ALTER TABLE public.regions OWNER TO evec;

--
-- Name: stations; Type: TABLE; Schema: public; Owner: evec; Tablespace: 
--

CREATE TABLE stations (
    stationid bigint NOT NULL,
    stationname text NOT NULL,
    systemid bigint NOT NULL,
    corpid bigint DEFAULT 0 NOT NULL
);


ALTER TABLE public.stations OWNER TO evec;

--
-- Name: systems; Type: TABLE; Schema: public; Owner: evec; Tablespace: 
--

CREATE TABLE systems (
    systemid bigint NOT NULL,
    systemname text NOT NULL,
    regionid bigint NOT NULL,
    faction bigint NOT NULL,
    security double precision NOT NULL,
    constellationid bigint NOT NULL,
    truesec double precision
);


ALTER TABLE public.systems OWNER TO evec;

--
-- Name: trends_type_region; Type: TABLE; Schema: public; Owner: evec; Tablespace: 
--

CREATE TABLE trends_type_region (
    typeid integer,
    region bigint,
    average double precision,
    median double precision,
    volume double precision,
    stddev double precision,
    buyup double precision,
    timeat timestamp with time zone DEFAULT now(),
    systemid bigint,
    bid integer DEFAULT 0,
    minimum double precision DEFAULT 0,
    maximum double precision DEFAULT 0
);


ALTER TABLE public.trends_type_region OWNER TO evec;

--
-- Name: types; Type: TABLE; Schema: public; Owner: evec; Tablespace: 
--

CREATE TABLE types (
    typeid bigint NOT NULL,
    typename text NOT NULL,
    typeclass text,
    size double precision DEFAULT 0.01 NOT NULL,
    published integer,
    marketgroup integer,
    groupid integer,
    raceid integer
);


ALTER TABLE public.types OWNER TO evec;

--
-- Name: user_prefs; Type: TABLE; Schema: public; Owner: evec; Tablespace: 
--

CREATE TABLE user_prefs (
    userid bigint NOT NULL,
    preference text NOT NULL,
    value text NOT NULL
);


ALTER TABLE public.user_prefs OWNER TO evec;

--
-- Name: users; Type: TABLE; Schema: public; Owner: evec; Tablespace: 
--

CREATE TABLE users (
    userid bigint NOT NULL,
    username text NOT NULL,
    password text NOT NULL,
    email text NOT NULL,
    corporation text,
    alliance text,
    isdirector integer NOT NULL,
    corpid bigint,
    lastlogin timestamp without time zone DEFAULT now() NOT NULL,
    registered timestamp without time zone DEFAULT now() NOT NULL,
    ismember integer DEFAULT 0 NOT NULL,
    uploads integer DEFAULT 0 NOT NULL,
    evecpoints double precision DEFAULT 10 NOT NULL,
    limited_apikey character varying(255),
    full_apikey character varying(255),
    apiuserid bigint,
    api_cacheuntil timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.users OWNER TO evec;

--
-- Name: wallet; Type: TABLE; Schema: public; Owner: evec; Tablespace: 
--

CREATE TABLE wallet (
    userid bigint,
    walletkey integer,
    balance numeric,
    timeat timestamp without time zone
);


ALTER TABLE public.wallet OWNER TO evec;

--
-- Name: wallet_journal; Type: TABLE; Schema: public; Owner: evec; Tablespace: 
--

CREATE TABLE wallet_journal (
    userid bigint,
    corp boolean DEFAULT false NOT NULL,
    accountkey integer,
    refid bigint,
    reftypeid integer,
    ownername1 character varying(100),
    ownerid1 bigint,
    ownername2 character varying(100),
    ownerid2 bigint,
    argname1 integer,
    argid1 integer,
    amount numeric,
    balance numeric,
    reason text
);


ALTER TABLE public.wallet_journal OWNER TO evec;

--
-- Name: wallet_market_transactions; Type: TABLE; Schema: public; Owner: evec; Tablespace: 
--

CREATE TABLE wallet_market_transactions (
    userid bigint,
    accountkey integer,
    transtime timestamp without time zone,
    transactionid bigint,
    quantity integer,
    typename character varying(255),
    typeid bigint,
    price numeric,
    clientid bigint,
    clientname character varying(255),
    characterid bigint,
    charactername character varying(255),
    stationid bigint,
    stationname text,
    transactiontype character varying(100),
    corp boolean DEFAULT false NOT NULL
);


ALTER TABLE public.wallet_market_transactions OWNER TO evec;

--
-- Name: constellations_pkey; Type: CONSTRAINT; Schema: public; Owner: evec; Tablespace: 
--

ALTER TABLE ONLY constellations
    ADD CONSTRAINT constellations_pkey PRIMARY KEY (constellationid);


--
-- Name: regions_regionid_key; Type: CONSTRAINT; Schema: public; Owner: evec; Tablespace: 
--

ALTER TABLE ONLY regions
    ADD CONSTRAINT regions_regionid_key UNIQUE (regionid);

ALTER TABLE regions CLUSTER ON regions_regionid_key;


--
-- Name: stations_pkey; Type: CONSTRAINT; Schema: public; Owner: evec; Tablespace: 
--

ALTER TABLE ONLY stations
    ADD CONSTRAINT stations_pkey PRIMARY KEY (stationid);


--
-- Name: systems_systemid_key; Type: CONSTRAINT; Schema: public; Owner: evec; Tablespace: 
--

ALTER TABLE ONLY systems
    ADD CONSTRAINT systems_systemid_key UNIQUE (systemid);


--
-- Name: types_typeid_key; Type: CONSTRAINT; Schema: public; Owner: evec; Tablespace: 
--

ALTER TABLE ONLY types
    ADD CONSTRAINT types_typeid_key UNIQUE (typeid);

ALTER TABLE types CLUSTER ON types_typeid_key;


--
-- Name: users_pkey; Type: CONSTRAINT; Schema: public; Owner: evec; Tablespace: 
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_pkey PRIMARY KEY (userid);


--
-- Name: archive_reportedtime; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX archive_reportedtime ON archive_market USING btree (reportedtime);

ALTER TABLE archive_market CLUSTER ON archive_reportedtime;


--
-- Name: archive_type_reported; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX archive_type_reported ON archive_market USING btree (typeid, reportedtime);


--
-- Name: at_transtime; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX at_transtime ON archive_transactions USING btree (transtime);


--
-- Name: blueprint_types_blueprinttypeid; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE UNIQUE INDEX blueprint_types_blueprinttypeid ON blueprint_types USING btree (blueprinttypeid);


--
-- Name: c_m_price; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX c_m_price ON current_market USING btree (price);


--
-- Name: c_m_region_type_reptime; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX c_m_region_type_reptime ON current_market USING btree (regionid, typeid, reportedtime);

ALTER TABLE current_market CLUSTER ON c_m_region_type_reptime;


--
-- Name: c_m_volremain; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX c_m_volremain ON current_market USING btree (volremain);


--
-- Name: current_market_bid_system_reported_time; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX current_market_bid_system_reported_time ON current_market USING btree (bid, systemid, reportedtime);


--
-- Name: current_market_region_price_bid_vol_rep; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX current_market_region_price_bid_vol_rep ON current_market USING btree (regionid, price, bid, volremain, reportedtime);


--
-- Name: current_market_region_type_bid; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX current_market_region_type_bid ON current_market USING btree (regionid, typeid, bid);


--
-- Name: current_market_rsystem_price_bid_vol_rep; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX current_market_rsystem_price_bid_vol_rep ON current_market USING btree (systemid, bid, price, volremain, reportedtime);


--
-- Name: current_market_typeid; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX current_market_typeid ON current_market USING btree (typeid);


--
-- Name: mfa_typeid_activity; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX mfa_typeid_activity ON materials_for_activity USING btree (typeid, activity);

ALTER TABLE materials_for_activity CLUSTER ON mfa_typeid_activity;


--
-- Name: regions_regionid; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX regions_regionid ON regions USING btree (regionid);


--
-- Name: regions_regionname; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX regions_regionname ON regions USING btree (regionname);


--
-- Name: station_systemid; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX station_systemid ON stations USING btree (systemid);

ALTER TABLE stations CLUSTER ON station_systemid;


--
-- Name: systems_regionid; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX systems_regionid ON systems USING btree (regionid);


--
-- Name: systems_systemname; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX systems_systemname ON systems USING btree (systemname);


--
-- Name: trends_type_region_ts; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX trends_type_region_ts ON trends_type_region USING btree (timeat);


--
-- Name: trends_type_region_type_region; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX trends_type_region_type_region ON trends_type_region USING btree (typeid, region);


--
-- Name: types_type_size; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX types_type_size ON types USING btree (typeid, size);


--
-- Name: types_typename; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX types_typename ON types USING btree (typename);


--
-- Name: users_username_password; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX users_username_password ON users USING btree (username, password);

ALTER TABLE users CLUSTER ON users_username_password;


--
-- Name: wallet_mt_type; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX wallet_mt_type ON wallet_market_transactions USING btree (typeid);


--
-- Name: wallet_mt_type_st; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX wallet_mt_type_st ON wallet_market_transactions USING btree (typeid, stationid);


--
-- Name: wallet_timeat; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX wallet_timeat ON wallet USING btree (timeat);


--
-- Name: wallet_userid; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX wallet_userid ON wallet USING btree (userid);


--
-- Name: wallet_userid_walletkey; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX wallet_userid_walletkey ON wallet USING btree (userid, walletkey);


--
-- Name: wallet_userid_walletkey_timeat; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX wallet_userid_walletkey_timeat ON wallet USING btree (userid, walletkey, timeat);


--
-- Name: wmt_transtime; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX wmt_transtime ON wallet_market_transactions USING btree (transtime);


--
-- Name: api_market_counts_userid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: evec
--

ALTER TABLE ONLY api_market_transid
    ADD CONSTRAINT api_market_counts_userid_fkey FOREIGN KEY (userid) REFERENCES users(userid) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

