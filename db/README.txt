This is the directory where goat's DB files live.

Files in goat.db/ and test.db/ are created by hsqldb.  Don't mess with them.

The proper way to store the Goat DB data in svn will be as a text file pooped out via a 
db dump utility.  The whole process should probably be an ant target; details here once 
I've got things figured out. -- rs 2006-03-08

If you cook up a new data model for some goat module or widget, put your data definition SQL 
in in sql/schema.sql.  This file is picked up by the test framework, so if your 
table definitions are in there, all you have to do is subclass goat.GoatTest (which is
itself a subclass of junit.TestCase) and you'll have a clean db setup for each junit test 
you write.  The db/sql directory is also a good place to stick any sql files you might
use to load up test data.

Loading an sql file from your java code is not straightforward when using hsql.  You can 
use goat.util.GoatDB.loadFile(String filename), which makes rough use of hsqldb.SqlTool and
has not been torture-tested, but should do the job for you.

The file setup-goat.db.sql should be used only for stuff that needs to be done to the 
deployed db, but not the test db.  Top-level property settings or other DBA crap should
otherwise go in schema.sql.

The file sqltool.rc is the config file for hsqldb's SqlTool.  If you use that tool, you'll
want to run it from goat's root directory and give it the option "--rcfile db/sqltool.rc".
The config file is used by goat.util.GoatDB, so change it with caution, as some of the 
stuff in it is unfortunately hard-coded at other points in the goat code.