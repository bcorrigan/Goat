-- Goat schema definition file.

-- Setup type stuff
SET PROPERTY "hsqldb.default_table_type" 'cached' ;
CREATE SCHEMA goat AUTHORIZATION DBA;
SET SCHEMA goat ;

--
-- Table Definitions.  Add stuff for your new widget below.
--

-- Basic irc data 


CREATE TABLE networks (
	id INTEGER GENERATED BY DEFAULT AS IDENTITY (START WITH 0) PRIMARY KEY,
	name VARCHAR NOT NULL,
	primary_server VARCHAR NOT NULL,
	UNIQUE (name)
) ;
-- insert a dummy value to use as a default
INSERT INTO networks (name, primary_server) VALUES ('', '') ;
-- and I haven't thought of a graceful way to set this up, yet...
INSERT INTO networks (name, primary_server) VALUES('slashnet', 'irc.slashnet.net') ;

CREATE TABLE servers (
	id INTEGER GENERATED BY DEFAULT AS IDENTITY (START WITH 0) PRIMARY KEY,
	hostname VARCHAR NOT NULL,
	network INTEGER DEFAULT 0 NOT NULL,
	PRIMARY KEY (id),
	UNIQUE (id),
	FOREIGN KEY (network) REFERENCES networks(id),
	UNIQUE (hostname),
	CHECK (hostname <> '')
) ;

CREATE TABLE channels (
	id INTEGER GENERATED BY DEFAULT AS IDENTITY (START WITH 0) PRIMARY KEY,
	name VARCHAR NOT NULL,
	network INT DEFAULT 0 NOT NULL,
	PRIMARY KEY (id),
	FOREIGN KEY (network) REFERENCES networks (id),
	UNIQUE (id),
	UNIQUE (name, network)
) ;
INSERT INTO channels (name) VALUES ('') ;

CREATE TABLE hostmasks (
	id INTEGER GENERATED BY DEFAULT AS IDENTITY (START WITH 0) PRIMARY KEY,
	hostmask VARCHAR NOT NULL,
	UNIQUE (hostmask)
) ;
INSERT INTO hostmasks (hostmask) VALUES ('') ;

CREATE TABLE nicks (
	id INTEGER GENERATED BY DEFAULT AS IDENTITY (START WITH 0) PRIMARY KEY,
	seen TIMESTAMP NOT NULL,
	last_hostmask INTEGER NOT NULL,
	network INT NOT NULL,
	name VARCHAR NOT NULL,
	PRIMARY KEY (id),
	UNIQUE (id),
	UNIQUE (name, network),
	FOREIGN KEY (network) REFERENCES networks (id),
	FOREIGN KEY (last_hostmask) REFERENCES hostmasks (id),
	CHECK (name <> '')
) ;

CREATE TABLE irc_commands (
	id INTEGER GENERATED BY DEFAULT AS IDENTITY (START WITH 0) PRIMARY KEY,
	name VARCHAR NOT NULL,
	PRIMARY KEY (id),
	UNIQUE (name),
	CHECK (name <> '')
) ;

CREATE TABLE ctcp_commands (
	id INTEGER GENERATED BY DEFAULT AS IDENTITY (START WITH 0) PRIMARY KEY,
	name VARCHAR NOT NULL,
	PRIMARY KEY (id),
	UNIQUE (name)
) ;
-- insert null command at id 0
INSERT INTO ctcp_commands (name) VALUES ('') ;

CREATE TABLE bot_commands (
	id INTEGER GENERATED BY DEFAULT AS IDENTITY (START WITH 0) PRIMARY KEY,
	name VARCHAR NOT NULL,
	PRIMARY KEY (id),
	UNIQUE (name)
) ;
-- insert null command at id 0
INSERT INTO bot_commands (name) VALUES ('') ;

CREATE TABLE messages (
	id INTEGER GENERATED BY DEFAULT AS IDENTITY (START WITH 0) PRIMARY KEY,
	text VARCHAR NOT NULL,
	timestamp TIMESTAMP NOT NULL,
	sender INTEGER NOT NULL,
	hostmask INTEGER NOT NULL,
	irc_command INTEGER NOT NULL,
	ctcp_command INTEGER DEFAULT 0 NOT NULL,
	bot_command INTEGER DEFAULT 0 NOT NULL,
	channel INTEGER NOT NULL,
	PRIMARY KEY (id),
	UNIQUE (id),
	FOREIGN KEY (sender) REFERENCES nicks (id),
	FOREIGN KEY (hostmask) REFERENCES hostmasks (id),
	FOREIGN KEY (channel) REFERENCES channels (id),
	FOREIGN KEY (irc_command) REFERENCES irc_commands (id),
	FOREIGN KEY (ctcp_command) REFERENCES ctcp_commands (id),
	FOREIGN KEY (bot_command) REFERENCES bot_commands (id),
	CHECK (text <> '')
);


-- Ye Olde "Great big join" view
CREATE VIEW messages_view (
		id, 
		timestamp, 
		sender,
		hostmask, 
		channel, 
		network,
		irc_command, 
		ctcp_command, 
		bot_command,
		body
	) 
	AS SELECT 
		messages.id,
		messages.timestamp,
		nicks.name,
		hostmasks.hostmask,
		channels.name,
		networks.name,
		irc_commands.name,
		ctcp_commands.name,
		bot_commands.name,
		messages.text
	FROM 
		messages, nicks, hostmasks, channels, networks, irc_commands, ctcp_commands, bot_commands
	WHERE
		messages.channel = channels.id
		AND messages.hostmask = hostmasks.id
		AND messages.sender = nicks.id
		AND channels.network = networks.id
		AND messages.irc_command = irc_commands.id
		AND messages.ctcp_command = ctcp_commands.id
		AND messages.bot_command = bot_commands.id
;