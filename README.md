An IRC bot with many commands, written by me and robotslave, if we forget about the fact that huge chunks of goat's interior have been lifted from many parts of the internet over the last ten years.

Originally written in java, with newer modules in scala and python (via jython)

Probably more useful to steal code from for your own bot, than to run your own instance, which would be a painful experience probably, as goat has not been designed for easy public consumption.  But if you must:

# Setup

1.  Copy config/passwords.properties.sample to config/passwords.properties

    Edit to taste

1.  Same deal with config/goatrc.sample

    the sample goatrc contains a long list of modules you can enable, but that list tends to get out of date.  Modules are presently located in three places in the source; you can find them in:
    * [src/main/java/goat/module/](https://github.com/bcorrigan/Goat/tree/master/src/main/java/goat/module)
    * [src/main/scala/goat/module/](https://github.com/bcorrigan/Goat/tree/master/src/main/scala/goat/module)
    * [scripts/](https://github.com/bcorrigan/Goat/tree/master/scripts)

1.  Make sure you've got sbt installed.

    The recommended way to get sbt these days is by [installing the typesafe stack](http://typesafe.com/resources/typesafe-stack/downloading-installing.html)

1.  You need Java 7 to compile and run goat.

    The output of both `java -version` and `javac -version` should tell you you're running java 1.7.

    On ubuntu, this can be a colossal pain in the butt to figure out if you haven't dealt with it before.  After you've done `sudo apt-get install openjdk-7-jre` you'll want to do both a `sudo update-alternatives --config javac` and a `sudo update-alternatives --config javac` to make sure you've got Java 7 configured as your default.

1.  Run `sbt compile`

    sbt might complain about a missing file, give you a url for that file, and tell you where to put it.  Follow those instructions.

1.  Run `sbt run`


# Sample usage

&lt;**bc**&gt; lsmod<br>
&lt;<b>goat</b>&gt; Loaded modules: CTCP, ModuleCommands, NickServ, Help, Auth, Core, Users, ServerCommands, GoatSay, More, Confessions2, WordGame, Say, Weather, ZipCodes, RandWords, Psychiatrist, Colours, Define, Threat, Calc, TODO, Adventure, Horoscope, Remind, CurrencyConverter, StockQuote, Quiz, Google, Bible, BookTitle, ShutUp, UnitConverter, Googlism, DiceRoll, Lastfm, Etymology, TwitterModule, Guru, CountDown, Uno.

### weather
&lt;<b>bc</b>&gt; weather<br>
&lt;<b>goat</b>&gt; 46F/8C, partly cloudy.  Wind W 16mph.  Humidity 87%.  Windchill 39.3F/4.0C.  Sunrise 8:43am, sunset 3:47pm.  Moon 38%-.  Reported 28 minutes ago at EGPK.  Score 17.58.

### Twitter stuff
&lt;<b>bc</b>&gt; trends near<br>
&lt;<b>goat</b>&gt; Trends of the moment for Glasgow:  1:Kim Jong 2:Boxing Day 3:#IfiHadThreeWishes 4:North Korea 5:Michael Buble 6:#YourPerfectGift 7:Willie Collum 8:The Krankies 9:Merry Christmas 10:Kris Boyd<br>
&lt;<b>bc</b>&gt; t willie collum radius=5<br>
&lt;<b>goat</b>&gt; 4 hours and 57 minutes ago, GordonRBell: Willie Collum for the game against the huns. Can't believe he ruled Hooper offside and that was never a penalty.<br>
&lt;<b>bc</b>&gt; t location=http://maps.google.co.uk/?ll=39.048552,125.780196&spn=0.036795,0.03901&t=h&z=15&vpsrc=6 radius=50<br>
&lt;<b>rs</b>&gt; tweetsearch kimchee<br>
&lt;<b>goat</b>&gt; 2 minutes and 3 seconds ago, Jinho Lee [@snow9610]: 떡국 !! 다들 새해 복 많이 받으세요 :) Ddeok-guk & Kimchee !!! Happy new year !! @ 이현 하이클래스웰가 http://t.co/i43Woqed<br>
&lt;<b>rs</b>&gt; twanslate<br>
&lt;<b>goat</b>&gt; 2 minutes and 10 seconds ago, Jinho Lee [@snow9610]: (from KOREAN) Big country!! Happy new year everybody:) Ddeok-guk & Kimchee !!! Happy new year !! @ County high class wealth is http://t.co/i43Woqed

### LastFM support
&lt;<b>bc</b>&gt; nowplaying<br>
&lt;<b>goat</b>&gt; st : Uninvited — Alanis Morissette

### Yahoo! stock quotes
&lt;<b>bc</b>&gt; quote AAPL<br>
&lt;<b>goat</b>&gt; Apple Inc. (AAPL):  382.46 +1.44 (0.38%), 4:54pm GMT Open: 382.47 Range: 381.52 - 384.85 Volume: 3.83M Market cap: 355.5B EBITDA: 35.57B Book: 82.45 52-week Range: 310.5 - 426.7 Float: 923.69M Short Ratio: 0.8

### wordy stuff
&lt;<b>bc</b>&gt; bandname<br>
&lt;<b>goat</b>&gt; connoting cultism<br>
&lt;<b>bc</b>&gt; title<br>
&lt;<b>goat</b>&gt; The Hustler of the Birch<br>
&lt;<b>bc</b>&gt; title Referee<br>
&lt;<b>goat</b>&gt; Magnificent Referee

### grouphug confessions
>Note: grouphug.us is deceased

&lt;<b>bc</b>&gt; confess about referee<br>
&lt;<b>goat</b>&gt; I have 6 things to confess about referee, starting with:<br>
&lt;<b>goat</b>&gt; I don't care about sleeping and being rested for my responsibilities. I have to referee soccer tomorrow from 9-11, which involves a lot running. Afterwards I have to go to an open casting call, and I really need the money. It's 2 am right now and I'm high from smoking pot so I can't exactly sleep. I hope I burnout soon because I know that I need to be alert and not-tired looking tomorrow…Yet I cannot sleep.

### various google services
&lt;<b>bc</b>&gt; google referee<br>
&lt;<b>goat</b>&gt; REFEREE, the Magazine for Sports Officials  http://www.referee.com/<br>
&lt;<b>bc</b>&gt; gis referee<br>
&lt;<b>goat</b>&gt; http://images.google.com/images?safe=off&nfpr=1&q=referee

### translation
&lt;<b>rs</b>&gt; translate hola chivo<br>
&lt;<b>goat</b>&gt; (from Spanish)  Hello goat<br>
&lt;<b>rs</b>&gt; detectlang hola chivo<br>
&lt;<b>goat</b>&gt; I think that's Spanish<br>
&lt;<b>rs</b>&gt; languages<br>
&lt;<b>goat</b>&gt; I am fluent in:  arabic (ar), bulgarian (bg), catalan (ca), chinese_simplified (zh-CHS), chinese_traditional (zh-CHT), czech (cs), danish (da), dutch (nl), english (en), estonian (et), finnish (fi), french (fr), german (de), greek (el), haitian_creole (ht), hebrew (he), hindi (hi), hmong_daw (mww), hungarian (hu), indonesian (id), italian (it), japanese (ja), korean (ko), latvian (lv), lithuanian (lt), norwegian (no), polish (pl), portuguese [more]

### etymology
&lt;<b>bc</b>&gt; etymology referee<br>
&lt;<b>goat</b>&gt; referee 1620s, "person who examines patent applications" (see refer). Sporting use is first recorded 1840 (specifically of baseball from 1856); the verb is first attested 1889, from the noun.

### terror threat level monitoring
>Note: the US terror alert system has been discontinued

&lt;<b>bc</b>&gt; threat<br>
&lt;<b>goat**&gt; The current Department of Homeland Security terror threat level is GREEN (**low danger</b>)

### nerdy dice rolling

&lt;<b>bc</b>&gt; roll 2d20 + 4d6<br>
&lt;<b>goat</b>&gt; bc: 2d20:19,13:32 4d6:6,2,2,5:15  Total:47

### currency conversions
&lt;<b>bc</b>&gt; convert<br>
&lt;<b>goat</b>&gt; The supported currencies are: \[AUD, BGN, BRL, CAD, CHF, CNY, CZK, DKK, EUR, GBP, HKD, HRK, HUF, IDR, ILS, INR, JPY, KRW, LTL, LVL, MXN, MYR, NOK, NZD, PHP, PLN, RON, RUB, SEK, SGD, THB, TRY, USD, ZAR\]<br>
&lt;<b>bc</b>&gt; convert 1 quid to USD<br>
&lt;<b>goat</b>&gt; £1 GBP = 1.553 USD

### many dictionaries supported
&lt;<b>bc</b>&gt; dictionaries<br>
&lt;<b>goat</b>&gt; gcide, wn, moby-thes, elements, vera, jargon, foldoc, easton, hitchcock, bouvier, devil, world02, gaz2k-counties, gaz2k-places, gaz2k-zips, eng-swe, nld-eng, eng-cze, eng-swa, ita-eng, tur-deu, nld-fra, lat-eng, eng-fra, deu-fra, eng-hin, dan-eng, nld-deu, jpn-deu, swa-eng, fra-deu, fra-eng, deu-ita, slo-eng, eng-rom, hin-eng, spa-eng, eng-lat, por-deu, gla-deu, swe-eng, scr-eng, deu-nld, ita-deu, fra-nld,… \[more\]<br>
&lt;<b>bc</b>&gt; more<br>
&lt;<b>goat</b>&gt; … afr-deu, ara-eng, deu-por, tur-eng, eng-spa, eng-ara, eng-rus, wel-eng, hun-eng, eng-cro, eng-por, world95, eng-wel, cro-eng, lat-deu, por-eng, eng-nld, eng-deu, iri-eng, eng-tur, eng-scr, eng-iri, cze-eng, deu-eng, eng-ita, eng-hun, english, trans, all, urban, oed, trends<br>
&lt;<b>bc</b>&gt; define cult dict=urban<br>
&lt;<b>goat</b>&gt; cult (urban): a religious group which promotes worship of a human leader and devotion of one's life to a specific purpose.  Some have members practice certain rituals or follow a set of principle rules.  The group usually believes its way is the only correct way to live life, and all non-members are doomed to some horrible fate if they cannot be persuaded to join. Ex: "Mormonism could easily be seen as a… \[more\]<br>
&lt;<b>goat</b>&gt; Definitions available: urban(7)<br>
&lt;<b>bc</b>&gt; more<br>
&lt;<b>goat</b>&gt; … cult.  Jesus is the savior, and unless you devote your life to following his teachings you will suffer a firey torture in Hell."<br>
&lt;<b>bc</b>&gt; define cult<br>
&lt;<b>goat</b>&gt; Cult (gcide): Cult \Cult\ (k[u^]lt) n. [F. culte, L. cultus care, culture, fr.  colere to cultivate. Cf. {Cultus}.]  1. Attentive care; homage; worship.  [1913 Webster]  Every one is convinced of the reality of a better  self, and of the cult or homage which is due to it.  --Shaftesbury.  [1913 Webster]  2. A system of religious belief and worship.  [1913 Webster]  That which was the religion of Moses is the … \[more\]<br>
&lt;<b>goat</b>&gt; Definitions available: gcide(1) wn(1) moby-thes(1)<br>

### more google stuff
&lt;<b>bc</b>&gt; sexiness george peppard<br>
&lt;<b>goat</b>&gt; "george peppard" is 18% sexy.<br>
&lt;<b>bc</b>&gt; sexiness pythagoras theorem<br>
&lt;<b>goat</b>&gt; "pythagoras theorem" is 7% sexy.<br>
&lt;<b>bc</b>&gt; googlefight scotland vs. england<br>
&lt;<b>goat</b>&gt; The winner is england, with a score of 77300000!

### bible querying
&lt;<b>bc</b>&gt; bibles<br>
&lt;<b>goat</b>&gt; King James Version (KJV), New International Version (NIV), New American Standard Bible(NASB), The Message (MSG), Amplified Bible (AMP), New Living Translation (NLT), English Standard Version (ESV), COntemporary English Version (CEV), New King James Version (NKJV), 21st Century King James Version (KJ21), American Standard Version (ASV), Young's Literal Translation (YLT), Darby Translation (DARBY), New Life \[more\]<br>
&lt;<b>bc</b>&gt; more<br>
&lt;<b>goat</b>&gt; &hellip;Version (NLV), Holman Christian Standard Bible (HCSB), New International Reader's Version (NIRV), Wycliffe New Testament (WNT), World English New Testament (WE), New International Version - UK (NIVUK), Hatian Creole Version (HCV), Luther Bibel 1545 (LUTH1545).<br>
&lt;<b>bc</b>&gt; bible genesis 1:1 bible=MSG<br>
&lt;<b>goat</b>&gt;   Genesis 1  Heaven and Earth  1-2 First this: God created the Heavens and Earth—all you see, all you don't see. Earth was a soup of nothingness, a bottomless emptiness, an inky blackness. God's Spirit brooded like a bird above the watery abyss.

### wordgame
&lt;<b>bc</b>&gt; wordgame<br>
&lt;<b>goat</b>&gt; Starting a 10 round match!<br>
&lt;<b>goat</b>&gt; Ready!<br>
&lt;<b>goat</b>&gt; Set!<br>
&lt;<b>goat</b>&gt; \*\*\* New Letters: V E T E T N T S E S I A N<br>
&lt;<b>bc</b>&gt; vents<br>
&lt;<b>goat</b>&gt; bc sets the pace with vents. score:5<br>
&lt;<b>bc</b>&gt; venetians<br>
&lt;<b>bc</b>&gt; venetian<br>
&lt;<b>goat</b>&gt; bc steals the lead with venetian. score: 8<br>
&lt;<b>rs</b>&gt; attentiveness<br>
&lt;<b>goat</b>&gt; rs steals the lead with attentiveness. score: 26<br>
&lt;<b>goat</b>&gt; rs WINS IT!!<br>
&lt;<b>goat</b>&gt; rs has won with attentiveness and gets 26 points!  This was the longest possible. 2 possible winning answers: attentiveness, tentativeness. Top 5 non-winning out of 396: anisettes, attentive, insensate, intensest, intestate.<br>

### numbers game
&lt;<b>bc</b>&gt; countdown<br>
&lt;<b>goat</b>&gt; \*\*\* New Numbers: 50 25 4 9 6 2  Target: 876<br>
&lt;<b>bc</b>&gt; 50*2*9<br>
&lt;<b>goat</b>&gt; bc has the best answer so far: 900. Just 24 off the target of 876!<br>
&lt;<b>bc</b>&gt; 50\*2\*9-25<br>
&lt;<b>goat</b>&gt; bc has the best answer so far: 875. Just 1 off the target of 876!<br>
&lt;<b>goat</b>&gt; 10 secs..<br>
&lt;<b>goat</b>&gt; The best answer was 875 by bc. But the best possible answer was: 876 = (25 * 9 - 6) * 4<br>

### calculator
&lt;<b>bc</b>&gt; calc 234\*sin(2)<br>
&lt;<b>goat</b>&gt; 212.77559787720951672266864862334716<br>
&lt;<b>bc</b>&gt; calc 1000!<br>
&lt;<b>goat</b>&gt; 2568 digits: 40238726007709377354370243392300398571937486421071463254379991<br>
04299385123986290205920442084869694048004799886101971960586316668729948085589013238296699445<br>
90997424504087073759918823627727188732519779505950995276120874975462497043601418278094646496<br>
29105639388743788648733711918104582578364784997701247663288983595573543251318532395846307555<br>
740911426241747434934755342864657661166779739666882029120737 \[more\]<br>
&lt;<b>av</b>&gt; more<br>
&lt;<b>goat</b>&gt; &hellip;91438537195882498081268678383745597317461360853795345242215865932019280908<br>
78297308431392844403281231558611036976801357304216168747609675871348312025478589320767169132<br>
44842623613141250878020800026168315102734182797770478463586817016436502415369139828126481021<br>
30927612448963599287051149649754199093422215668325720808213331861168115536158365469840467089<br>
7560290095053761647584772842188967964624494516076535340819890 \[more\]<br>
&lt;<b>bc</b>&gt; calc roman(9427)<br>
&lt;<b>goat</b>&gt; MMMMMMMMMCDXXVIIxR

### quiz
&lt;<b>bc</b>&gt; Quiz<br>
&lt;<b>goat</b>&gt; Question #1<br>
&lt;<b>goat</b>&gt; Geography.  Name the only Central American country without an Atlantic coastline.<br>
&lt;<b>goat</b>&gt; tip: E_ \_a\_\_\_\_\_\_<br>
&lt;<b>ct</b>&gt; costa ricka<br>
&lt;<b>ct</b>&gt; el salvador<br>
&lt;<b>goat</b>&gt; ct: Congratulations, you got the correct answer, "El Salvador".<br>

### reminders
&lt;<b>bc</b>&gt; remind me in 1 minute to demonstrate goat<br>
&lt;<b>goat</b>&gt; bc: Okay, I'll remind you about that on 19/12/11 17:51 Europe/London<br>
    <i>...1 minute later...</i><br>
&lt;<b>goat</b>&gt; bc, you asked me to remind you to demonstrate goat<br>

### news
&lt;<b>bc</b>&gt; gnews north korea<br>
&lt;<b>goat</b>&gt; 1) 10pm PST,  — North Korean leader Kim Jong Il dies (Los Angeles Times)  2) 10am PST,  — North Korea mourns Kim Jong Il; son is 'successor' (Denver Post)  3) 5am PST,  — US Aid a Step Toward Korea Nuke Talks (TIME)  4) 9am PST,  — Inside North Korea's First Family: Rivals to Kim Jong-un's Power (Daily Beast)<br>
&lt;<b>bc</b>&gt; glink 3<br>
&lt;<b>goat</b>&gt; http://www.time.com/time/world/article/0,8599,2102759,00.html  US Aid a Step Toward Korea Nuke Talks  5am PST,  (TIME)  —  18, 2011 The United States is poised to announce a significant donation of food aid to North Korea this week, the first concrete accomplishment after months of behind-the-scenes diplomatic contacts between the two wartime enemies. An agreement by North ...

### ....and many more commands!
See:

* [config/goatrc](https://github.com/bcorrigan/Goat/blob/master/config/goatrc.sample)
* [src/main/java/goat/module/](https://github.com/bcorrigan/Goat/tree/master/src/main/java/goat/module)
* [src/main/scala/goat/module/](https://github.com/bcorrigan/Goat/tree/master/src/main/scala/goat/module)
* [scripts/](https://github.com/bcorrigan/Goat/tree/master/scripts)
