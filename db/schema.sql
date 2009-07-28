--
-- PostgreSQL database dump
--

SET client_encoding = 'UTF8';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;

--
-- Name: plpgsql; Type: PROCEDURAL LANGUAGE; Schema: -; Owner: postgres
--

CREATE PROCEDURAL LANGUAGE plpgsql;


ALTER PROCEDURAL LANGUAGE plpgsql OWNER TO postgres;

SET search_path = public, pg_catalog;

--
-- Name: max(integer, integer); Type: FUNCTION; Schema: public; Owner: evec
--

CREATE FUNCTION max(integer, integer) RETURNS integer
    AS $_$     BEGIN
         IF $1 > $2 THEN
             RETURN $1;
         END IF;
         RETURN $2;
     END;
 $_$
    LANGUAGE plpgsql;


ALTER FUNCTION public.max(integer, integer) OWNER TO evec;

--
-- Name: min(integer, integer); Type: FUNCTION; Schema: public; Owner: evec
--

CREATE FUNCTION min(integer, integer) RETURNS integer
    AS $_$
     BEGIN
         IF $1 < $2 THEN
             RETURN $1;
         END IF;
         RETURN $2;
     END;
 $_$
    LANGUAGE plpgsql;


ALTER FUNCTION public.min(integer, integer) OWNER TO evec;

--
-- Name: min(double precision, double precision); Type: FUNCTION; Schema: public; Owner: evec
--

CREATE FUNCTION min(double precision, double precision) RETURNS double precision
    AS $_$     BEGIN
         IF $1 < $2 THEN
             RETURN $1;
         END IF;
         RETURN $2;
     END;$_$
    LANGUAGE plpgsql;


ALTER FUNCTION public.min(double precision, double precision) OWNER TO evec;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: adlocation; Type: TABLE; Schema: public; Owner: evec; Tablespace: 
--

CREATE TABLE adlocation (
    adid integer NOT NULL,
    type bigint NOT NULL,
    expires timestamp without time zone NOT NULL
);


ALTER TABLE public.adlocation OWNER TO evec;

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
    issued date NOT NULL,
    duration interval NOT NULL,
    range integer NOT NULL,
    reportedby bigint NOT NULL,
    reportedtime timestamp without time zone DEFAULT now() NOT NULL
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
-- Name: browserads; Type: TABLE; Schema: public; Owner: evec; Tablespace: 
--

CREATE TABLE browserads (
    adid integer NOT NULL,
    owner bigint NOT NULL,
    name character varying(100) NOT NULL,
    adcopy character varying(300) NOT NULL,
    other text NOT NULL
);


ALTER TABLE public.browserads OWNER TO evec;

--
-- Name: browserads_adid_seq; Type: SEQUENCE; Schema: public; Owner: evec
--

CREATE SEQUENCE browserads_adid_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.browserads_adid_seq OWNER TO evec;

--
-- Name: browserads_adid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: evec
--

ALTER SEQUENCE browserads_adid_seq OWNED BY browserads.adid;


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
-- Name: corp_wallet; Type: TABLE; Schema: public; Owner: evec; Tablespace: 
--

CREATE TABLE corp_wallet (
    corpid bigint,
    walletkey integer,
    balance numeric,
    timeat timestamp without time zone
);


ALTER TABLE public.corp_wallet OWNER TO evec;

--
-- Name: corppages; Type: TABLE; Schema: public; Owner: evec; Tablespace: 
--

CREATE TABLE corppages (
    corpid bigint NOT NULL,
    pagename text NOT NULL,
    contents text NOT NULL,
    edit timestamp without time zone DEFAULT now() NOT NULL,
    title text NOT NULL,
    view character varying(20) DEFAULT 'public'::character varying NOT NULL
);


ALTER TABLE public.corppages OWNER TO evec;

--
-- Name: corps; Type: TABLE; Schema: public; Owner: evec; Tablespace: 
--

CREATE TABLE corps (
    corpid bigint NOT NULL,
    corpname text NOT NULL,
    description text NOT NULL,
    headquarters text NOT NULL,
    join_password text DEFAULT ''::text NOT NULL,
    ticker character varying(10) DEFAULT ''::character varying NOT NULL,
    ceo bigint,
    evecpoints double precision DEFAULT 10 NOT NULL
);


ALTER TABLE public.corps OWNER TO evec;

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
    reportedtime timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.current_market OWNER TO evec;

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
    systemid bigint NOT NULL
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
-- Name: uploads; Type: TABLE; Schema: public; Owner: evec; Tablespace: 
--

CREATE TABLE uploads (
    userid bigint NOT NULL,
    typeid bigint NOT NULL,
    regionid bigint NOT NULL,
    stamp timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.uploads OWNER TO evec;

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
-- Name: adid; Type: DEFAULT; Schema: public; Owner: evec
--

ALTER TABLE browserads ALTER COLUMN adid SET DEFAULT nextval('browserads_adid_seq'::regclass);


--
-- Name: browserads_pkey; Type: CONSTRAINT; Schema: public; Owner: evec; Tablespace: 
--

ALTER TABLE ONLY browserads
    ADD CONSTRAINT browserads_pkey PRIMARY KEY (adid);

ALTER TABLE browserads CLUSTER ON browserads_pkey;


--
-- Name: constellations_pkey; Type: CONSTRAINT; Schema: public; Owner: evec; Tablespace: 
--

ALTER TABLE ONLY constellations
    ADD CONSTRAINT constellations_pkey PRIMARY KEY (constellationid);


--
-- Name: corps_pkey; Type: CONSTRAINT; Schema: public; Owner: evec; Tablespace: 
--

ALTER TABLE ONLY corps
    ADD CONSTRAINT corps_pkey PRIMARY KEY (corpid);

ALTER TABLE corps CLUSTER ON corps_pkey;


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


--
-- Name: c_m_volremain; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX c_m_volremain ON current_market USING btree (volremain);


--
-- Name: corppages_page; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE UNIQUE INDEX corppages_page ON corppages USING btree (corpid, pagename);


--
-- Name: current_market_bid_system_reported_time; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX current_market_bid_system_reported_time ON current_market USING btree (bid, systemid, reportedtime);


--
-- Name: current_market_orderid; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE UNIQUE INDEX current_market_orderid ON current_market USING btree (orderid);


--
-- Name: current_market_region_price_bid_vol_rep; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX current_market_region_price_bid_vol_rep ON current_market USING btree (regionid, price, bid, volremain, reportedtime);


--
-- Name: current_market_region_type_bid; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX current_market_region_type_bid ON current_market USING btree (regionid, typeid, bid);

ALTER TABLE current_market CLUSTER ON current_market_region_type_bid;


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
-- Name: types_type_size; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX types_type_size ON types USING btree (typeid, size);


--
-- Name: types_typename; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX types_typename ON types USING btree (typename);


--
-- Name: upload_upser; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX upload_upser ON uploads USING btree (userid);


--
-- Name: uploads_stamp; Type: INDEX; Schema: public; Owner: evec; Tablespace: 
--

CREATE INDEX uploads_stamp ON uploads USING btree (stamp);

ALTER TABLE uploads CLUSTER ON uploads_stamp;


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
-- Name: adlocation_adid; Type: FK CONSTRAINT; Schema: public; Owner: evec
--

ALTER TABLE ONLY adlocation
    ADD CONSTRAINT adlocation_adid FOREIGN KEY (adid) REFERENCES browserads(adid) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: adlocation_type; Type: FK CONSTRAINT; Schema: public; Owner: evec
--

ALTER TABLE ONLY adlocation
    ADD CONSTRAINT adlocation_type FOREIGN KEY (type) REFERENCES types(typeid) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: api_market_counts_userid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: evec
--

ALTER TABLE ONLY api_market_transid
    ADD CONSTRAINT api_market_counts_userid_fkey FOREIGN KEY (userid) REFERENCES users(userid) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: corppages_corp; Type: FK CONSTRAINT; Schema: public; Owner: evec
--

ALTER TABLE ONLY corppages
    ADD CONSTRAINT corppages_corp FOREIGN KEY (corpid) REFERENCES corps(corpid) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

