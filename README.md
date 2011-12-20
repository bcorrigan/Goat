An IRC bot with many commands, written by me and robotslave, if we forget about the fact that huge chunks of goat's interior have been lifted from many parts of the internet over the last ten years.

Nearly all written in java, apart from the twitter stuff which is in scala.

Probably more useful to steal code from for your own bot, than to run your own instance, which would be a painful experience probably, as goat has not been designed for easy public consumption:

(**bc**): lsmod  
(**goat**): Loaded modules: CTCP, ModuleCommands, NickServ, Help, Auth, Core, Users, ServerCommands, GoatSay, More, Confessions2, WordGame, Say, Weather, ZipCodes, RandWords, Psychiatrist, Colours, Define, Threat, Calc, TODO, Adventure, Horoscope, Remind, CurrencyConverter, StockQuote, Quiz, Google, Bible, BookTitle, ShutUp, UnitConverter, Googlism, DiceRoll, Lastfm, Etymology, TwitterModule, Guru, CountDown, Uno.

# Weather
(**bc**): weather  
(**goat**): 46F/8C, partly cloudy.  Wind W 16mph.  Humidity 87%.  Windchill 39.3F/4.0C.  Sunrise 8:43am, sunset 3:47pm.  Moon 38%-.  Reported 28 minutes ago at EGPK.  Score 17.58.  

# Twitter stuff
(**bc**): trends near  
(**goat**): Trends of the moment for Glasgow:  1:Kim Jong 2:Boxing Day 3:#IfiHadThreeWishes 4:North Korea 5:Michael Buble 6:#YourPerfectGift 7:Willie Collum 8:The Krankies 9:Merry Christmas 10:Kris Boyd  
(**bc**): t willie collum radius=5  
(**goat**): 4 hours and 57 minutes ago, GordonRBell: Willie Collum for the game against the huns. Can't believe he ruled Hooper offside and that was never a penalty.  
(**bc**): t location=http://maps.google.co.uk/?ll=39.048552,125.780196&spn=0.036795,0.03901&t=h&z=15&vpsrc=6 radius=50  
(**goat**): 7 minutes and 56 seconds ago, 753_193: 明日は喪に服すか  
(**bc**): guru translate from korean 明日は喪に服すか  
(**goat**): I mourn tomorrow  
(**bc**): define kim jong dict=trends  
(**goat**): bc, I'm starting to hate you.  
(**goat**): kim jong (trends): North Korean Leader, Kim Jong Il, has died at the age of 69. 2011-12-19T04:19:25Z Definition by whatthetrend.com  
(**goat**): Definitions available: trends(11)  

# lastfm support
(**bc**): nowplaying  
(**goat**): st : Uninvited — Alanis Morissette  

# yahoo stocks
(**bc**): quote AAPL  
(**goat**): Apple Inc. (AAPL):  382.46 +1.44 (0.38%), 4:54pm GMT Open: 382.47 Range: 381.52 - 384.85 Volume: 3.83M Market cap: 355.5B EBITDA: 35.57B Book: 82.45 52-week Range: 310.5 - 426.7 Float: 923.69M Short Ratio: 0.8  

# wordy stuff
(**bc**): bandname  
(**goat**): connoting cultism  
(**bc**): title  
(**goat**): The Hustler of the Birch  
(**bc**): title Referee  
(**goat**): Magnificent Referee  

# grouphug confessions  
(**bc**): confess about referee  
(**goat**): I have 6 things to confess about referee, starting with:  
(**goat**):   I don't care about sleeping and being rested for my responsibilities. I have to referee soccer tomorrow from 9-11, which involves a lot running. Afterwards I have to go to an open casting call, and I really need the money. It's 2 am right now and I'm high from smoking pot so I can't exactly sleep. I hope I burnout soon because I know that I need to be alert and not-tired looking tomorrow…Yet I cannot sleep.  

# various google services
(**bc**): google referee  
(**goat**): REFEREE, the Magazine for Sports Officials  http://www.referee.com/  
(**bc**): gis referee  
(**goat**): http://images.google.com/images?safe=off&nfpr=1&q=referee  
(**bc**): guru translate to german from english I am a referee  
(**goat**): Translation: 'i am a referee' in English means 'Ich bin ein Schiedsrichter' in German  

# etymology
(**bc**): etymology referee  
(**goat**): referee 1620s, "person who examines patent applications" (see refer). Sporting use is first recorded 1840 (specifically of baseball from 1856); the verb is first attested 1889, from the noun.  

# Terror threat monitoring
(**bc**): threat  
(**goat**): The current Department of Homeland Security terror threat level is GREEN (**low danger**)  

# nerdy dice rolling
(**bc**): roll 2d20 + 4d6  
(**goat**): bc: 2d20:19,13:32 4d6:6,2,2,5:15  Total:47  

# currency conversions
(**bc**): convert  
(**goat**): The supported currencies are: [AUD, BGN, BRL, CAD, CHF, CNY, CZK, DKK, EUR, GBP, HKD, HRK, HUF, IDR, ILS, INR, JPY, KRW, LTL, LVL, MXN, MYR, NOK, NZD, PHP, PLN, RON, RUB, SEK, SGD, THB, TRY, USD, ZAR]  
(**bc**): convert 1 quid to USD  
(**goat**): £1 GBP = 1.553 USD  

# many dictionaries supported
(**bc**): dictionaries  
(**goat**): gcide, wn, moby-thes, elements, vera, jargon, foldoc, easton, hitchcock, bouvier, devil, world02, gaz2k-counties, gaz2k-places, gaz2k-zips, eng-swe, nld-eng, eng-cze, eng-swa, ita-eng, tur-deu, nld-fra, lat-eng, eng-fra, deu-fra, eng-hin, dan-eng, nld-deu, jpn-deu, swa-eng, fra-deu, fra-eng, deu-ita, slo-eng, eng-rom, hin-eng, spa-eng, eng-lat, por-deu, gla-deu, swe-eng, scr-eng, deu-nld, ita-deu, fra-nld,… [more]  
(**bc**): more  
(**goat**): … afr-deu, ara-eng, deu-por, tur-eng, eng-spa, eng-ara, eng-rus, wel-eng, hun-eng, eng-cro, eng-por, world95, eng-wel, cro-eng, lat-deu, por-eng, eng-nld, eng-deu, iri-eng, eng-tur, eng-scr, eng-iri, cze-eng, deu-eng, eng-ita, eng-hun, english, trans, all, urban, oed, trends  
(**bc**): define cult dict=urban  
(**goat**): cult (urban): a religious group which promotes worship of a human leader and devotion of one's life to a specific purpose.  Some have members practice certain rituals or follow a set of principle rules.  The group usually believes its way is the only correct way to live life, and all non-members are doomed to some horrible fate if they cannot be persuaded to join. Ex: "Mormonism could easily be seen as a… [more]  
(**goat**): Definitions available: urban(7)  
(**bc**): more  
(**goat**): … cult.  Jesus is the savior, and unless you devote your life to following his teachings you will suffer a firey torture in Hell."  
(**bc**): define cult  
(**goat**): Cult (gcide): Cult \Cult\ (k[u^]lt) n. [F. culte, L. cultus care, culture, fr.  colere to cultivate. Cf. {Cultus}.]  1. Attentive care; homage; worship.  [1913 Webster]  Every one is convinced of the reality of a better  self, and of the cult or homage which is due to it.  --Shaftesbury.  [1913 Webster]  2. A system of religious belief and worship.  [1913 Webster]  That which was the religion of Moses is the … [more]  
(**goat**): Definitions available: gcide(1) wn(1) moby-thes(1)  

# more google stuff
(**bc**): sexiness george peppard  
(**goat**): "george peppard" is 18% sexy.  
(**bc**): sexiness pythagoras theorem  
(**goat**): "pythagoras theorem" is 7% sexy.  
(**bc**): googlefight scotland vs. england  
(**goat**): The winner is england, with a score of 77300000!  

# bible querying
(**bc**): bibles  
(**goat**): King James Version (KJV), New International Version (NIV), New American Standard Bible(NASB), The Message (MSG), Amplified Bible (AMP), New Living Translation (NLT), English Standard Version (ESV), COntemporary English Version (CEV), New King James Version (NKJV), 21st Century King James Version (KJ21), American Standard Version (ASV), Young's Literal Translation (YLT), Darby Translation (DARBY), New Life… [more]  
(**bc**): more  
(**goat**): … Version (NLV), Holman Christian Standard Bible (HCSB), New International Reader's Version (NIRV), Wycliffe New Testament (WNT), World English New Testament (WE), New International Version - UK (NIVUK), Hatian Creole Version (HCV), Luther Bibel 1545 (LUTH1545).  
(**bc**): bible genesis 1:1 bible=MSG  
(**goat**):   Genesis 1  Heaven and Earth  1-2 First this: God created the Heavens and Earth—all you see, all you don't see. Earth was a soup of nothingness, a bottomless emptiness, an inky blackness. God's Spirit brooded like a bird above the watery abyss.  

# wordgame
(**bc**): wordgame  
(**goat**): Starting a 10 round match!  
(**goat**): Ready!  
(**goat**): Set!  
(**goat**): \*\*\* New Letters: V E T E T N T S E S I A N   
(**bc**): vents  
(**goat**): bc sets the pace with vents. score:5  
(**bc**): venetians  
(**bc**): venetian  
(**goat**): bc steals the lead with venetian. score: 8  
(**rs**): attentiveness  
(**goat**): rs steals the lead with attentiveness. score: 26  
(**goat**): rs WINS IT!!  
(**goat**): rs has won with attentiveness and gets 26 points!  This was the longest possible. 2 possible winning answers: attentiveness, tentativeness. Top 5 non-winning out of 396: anisettes, attentive, insensate, intensest, intestate.  

# numbers game
(**bc**): countdown  
(**goat**): \*\*\* New Numbers: 50 25 4 9 6 2  Target: 876  
(**bc**): 50*2*9  
(**goat**): bc has the best answer so far: 900. Just 24 off the target of 876!  
(**bc**): 50\*2\*9-25  
(**goat**): bc has the best answer so far: 875. Just 1 off the target of 876!  
(**goat**): 10 secs..  
(**goat**): The best answer was 875 by bc. But the best possible answer was: 876 = (25 * 9 - 6) * 4  

# calculator
(**bc**): calc 234\*sin(2)  
(**goat**): 212.77559787720951672266864862334716  
(**bc**): calc 1000!  
(**goat**): 2568 digits: 40238726007709377354370243392300398571937486421071463254379991042993851239862902059204420848696940480047998861019719605863166687299480855890132382966994459099742450408707375991882362772718873251977950595099527612087497546249704360141827809464649629105639388743788648733711918104582578364784997701247663288983595573543251318532395846307555740911426241747434934755342864657661166779739666882029120737… [more]  
(**av**): more  
(**goat**): …914385371958824980812686783837455973174613608537953452422158659320192809087829730843139284440328123155861103697680135730421616874760967587134831202547858932076716913244842623613141250878020800026168315102734182797770478463586817016436502415369139828126481021309276124489635992870511496497541990934222156683257208082133318611681155361583654698404670897560290095053761647584772842188967964624494516076535340819890… [more]  
(**bc**): calc roman(**9427**)  
(**goat**): MMMMMMMMMCDXXVIIxR  

# quiz
(**bc**): Quiz  
(**goat**): Question #1  
(**goat**): Geography.  Name the only Central American country without an Atlantic coastline.  
(**goat**): tip: E_ _a______  
(**ct**): costa ricka  
(**ct**): el salvador  
(**goat**): ct: Congratulations, you got the correct answer, "El Salvador".  

# reminders
(**bc**): remind me in 1 minute to demonstrate goat  
(**goat**): bc: Okay, I'll remind you about that on 19/12/11 17:51 Europe/London  
...1 minute later...  
(**goat**): bc, you asked me to remind you to demonstrate goat  

# news
(**bc**): gnews north korea  
(**goat**): 1) 10pm PST,  — North Korean leader Kim Jong Il dies (Los Angeles Times)  2) 10am PST,  — North Korea mourns Kim Jong Il; son is 'successor' (Denver Post)  3) 5am PST,  — US Aid a Step Toward Korea Nuke Talks (TIME)  4) 9am PST,  — Inside North Korea's First Family: Rivals to Kim Jong-un's Power (Daily Beast)  
(**bc**): glink 3  
(**goat**): http://www.time.com/time/world/article/0,8599,2102759,00.html  US Aid a Step Toward Korea Nuke Talks  5am PST,  (TIME)  —  18, 2011 The United States is poised to announce a significant donation of food aid to North Korea this week, the first concrete accomplishment after months of behind-the-scenes diplomatic contacts between the two wartime enemies. An agreement by North ...  

....and many more commands I can't even be bothered demonstrating