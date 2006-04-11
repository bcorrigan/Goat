This is the directory where goat's DB files live.

The proper way to store the Goat DB data in svn will be as a text file pooped out via a 
db dump utility.  The whole process should probably be an ant target; details here once 
I've got things figured out. -- rs 2006-03-08

If you cook up a new data model for some goat module or widget, put your data definition SQL 
in in sql/schema.sql.  This file is picked up by the test framework, so if your 
table definitions are in there, all you have to do is subclass goat.GoatTest (which is
itself a subclass of junit.TestCase) and you'll have a clean db setup for each junit test 
you write.  The db/sql directory is also a good place to stick any sql files you might
use to load up test data.

Loading an sql file from your java code is not straightforward when using jdbc.  You can 
use goat.util.GoatDB.loadFile(String filename), which works so long as you don't put more than one statement on a line, and end all statements with a semicolon followed by a newline (whitespace in between is OK).

There is still some DBA setup stuff that hasn't been automated.  For now, you
will need to create your db (be sure to use unicode encoding), create users,
create schemas, and populate deployment and sandbox schemas by hand (junit schema is automatically populated during testing).

to create db:
	createdb [name] -E unicode

to create user, start psql as admin user
	psql -U pgsql [dbname]

and issue command
	create user [name] with login encrypted password '[secret]';

to create schema in psql:
	create schema [schema name] authorization [schema user];

to populate a schema, start psql as schema owner
	psql -U [user name] [db name]

and then
	\i db/sql/schema.sql
