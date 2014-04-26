/*
 * This will randomly generate a story plot for a given genre.
 * Plots under "wondermark" originally based on http://wondermark.com/554/
 */

package goat.util;

import scala.util.Random
import scala.collection.MapProxy


class PlotMaker(val self: Map[String, Either[PlotMaker, List[Either[PlotMaker, String]]]])
  extends MapProxy[String, Either[PlotMaker, List[Either[PlotMaker, String]]]] {

  import PlotMaker._

  /* functions */

  def pick(pmv: PlotMapValue): String =
    pmv match {
      case Left(plotMaker) => plotMaker.generate
      case Right(plotList) =>
        sample(plotList)(random) match {
          case Left(plotMaker) => plotMaker.generate
          case Right(string) => string
        }
      }

  def pickTemplate: String = pick(self("templates"))

  def keyName(str: String) =
    str.replaceFirst("[0-9]$", "")

  def generate: String =
    (for(k <- self.keys if k != "templates"; i <- "" +: (0 to 9).map(_.toString)) yield k + i)
        .fold(pickTemplate)((s, r) => s.replaceAll("\\[" + r + "\\]", pick(self(keyName(r)))))

  def title = pick(self("title"))
}


object PlotMaker {

  /* type inconvenience definitions.  ffs, scala. */
  type PlotListElement = Either[PlotMaker, String]
  type PlotList = List[PlotListElement]
  type PlotMapValue = Either[PlotMaker, PlotList]
  type PlotMapEntry = (String, PlotMapValue)
  type PlotMap = Map[String, PlotMapValue]
  def PlotMap(seq: PlotMapEntry*) = new PlotMaker(seq.toMap)
  def PlotList(seq: PlotListElement*) = seq.toList
  implicit def makeLeft[A, B](a: A):Either[A, B] = Left(a)
  implicit def makeRight[A, B](b: B):Either[A, B] = Right(b)


  /* functions (class methods) */

  def rawGenres = genreDefinitions.keys.toList.filter(!_.endsWith("Title"))

  def genres = rawGenres.sorted

  def genresAsString = listAsString(genres)

  def hasGenre(genre: String) = rawGenres.contains(genre)

  def randomGenre = sample(rawGenres)

  def plot(genre: String): String = genreDefinitions(genre).generate

  def title(genre: String): String = titleCase(genreDefinitions(genre).title).replaceAll("Vs.", "vs.")


  /* utility functions; these ought to go live in a library somewhere */

  def listAsString(list: List[String]): String = {
    val rlist = list.reverse
    rlist match {
      case Nil => ""
      case _ => rlist.tail.reverse.reduceRight(_.toString + ", " + _.toString) + " and " + rlist.head
    }
  }

  implicit val random = new Random

  def sample[A](list: List[A])(implicit random: Random):A =
    list(random.nextInt(list.length))

  def titleCase(s: String, sep: String): String = s.split(sep).map(_.capitalize) mkString sep

  def titleCase(s: String): String = titleCase(titleCase(s, "-"), " ")

  /* data */


  /* Authoring hints:
   *
   * You need around a dozen items in your major lists if you don't want your plot to
   *   wear out its welcome too quickly.  Shorter lists are fine for filler.
   *
   * You'll usually get much better results if you write lots of longer phrases instead of
   *   trying to achieve variety by combining many short phrases together.
   *
   * With that said, judicious use of short lists can do wonderful things.
   *
   * Try writing out a full plot with all the specific details in place.  Then replace
   *    various bits of it with more generic wording.  Keep this around in a comment, it
   *    can help keep things focused as you add more.  Then write a template (or preferably
   *    several).  Repeat!
   *
   * More top-level templates will increase apparent variety way more than adding
   *    more list items.
   *
   * With that said, do add more list items.  They're much easier to invent than templates,
   *    and more is generally better.
   *
   * Be aware of the fact that as you get deeper into sub-templates, your lists get (much)
   *    less likely to be randomly selected.  There's probably some math involved.  If a
   *    particular branch of plotland isn't getting enough love, try adding a duplicate
   *    (or better, a near-duplicate) of whatever template invokes it.
   *
   */

  val genreDefinitions = Map[String, PlotMaker](

    "kungfu" -> PlotMap(
      "templates" -> PlotList(
        "In [settingAdjective] [setting], [heroAdjective] [hero] who " +
          "study [technique] stumble across [discovery] which lead to conflict " +
          "with [villain] with help of [companion], culminating in [climax].",
        "As a child, [heroAdjective] [hero] of [setting] watched as [villain] [murderMethod] his " +
          "[murderVictim].  Now, after years of [techniqueAdjective] [technique] training, " +
          "[heroAdjective] [hero] will avenge [murderVictim], with help of [companion], in [climax]."),

      "settingAdjective" -> PlotList(
        "colonial",
        "ancient",
        "timeless",
        "alternate-history",
        "anachronistic",
        "agricultural",
        "utopian",
        "oppressed",
        "capitalistic",
        "modern day",
        "feudal",
        "agrarian"),
      "setting" -> PlotList(
        "Japan",
        "China",
        "Thailand",
        "Hong Kong",
        "Shanghai",
        "Tokyo",
        "Cambodia",
        "Vietnam",
        "Taipei",
        "Taiwan",
        "Peasant Village",
        "Slum"),
      "heroAdjective" -> PlotList(
        "simple",
        "bumbling",
        "fat",
        "scrawny",
        "prophesied",
        "disgraced",
        "widowed",
        "wizened",
        "blind",
        "crippled",
        "drunken"),
      "hero" -> PlotList(
        "farmer",
        "fisherman",
        "monk",
        "bureaucrat",
        "samurai",
        "ronin",
        "mercenary",
        "thief",
        "student",
        "goatherd",
        "laborer",
        "soldier",
        "assassin"),
      "villain" -> PlotList(
        "gang of toughs",
        "interantional crime league",
        "local warlord",
        "his older brother",
        "white imperialists",
        "supernatural monsters",
        "evil monks",
        "ancient wizard-ninja",
        "army led by sadist",
        "yakuza",
        "treasure hunters"),
      // companion should be split into parts
      "companion" -> PlotList(
        "cute shy girl",
        "cute dangerous girl",
        "fearless, useless girl",
        "best friend",
        "childhood rival",
        "bumbling white man",
        "Chris Rock",
        "cranky, decrepit old man",
        "domineering, screeching old woman",
        "kindly teacher",
        "his father's sword",
        "a magic talking belt",
        "fat, friendly innkeeper"),
      "technique" -> PlotList(
        "kung fu",
        "muay thai",
        "karate",
        "wushu",
        "judo",
        "jiu jitsu",
        "bushido",
        "grecco roman wrestling",
        "sumo",
        "tai chi",
        "ikibana"),
      "techniqueAdjective" -> PlotList(
        "secret",
        "forbidden",
        "furtive",
        "ancient",
        "illicit",
        "mystical",
        "forgotten",
        "long lost"),
      "murderMethod" -> PlotList(
        "butchered",
        "dismembered",
        "burned",
        "destroyed",
        "drowned",
        "poisoned",
        "slaughtered",
        "stabbed",
        "raped",
        "crushed"),
      "murderVictim" -> PlotList(
        "wife",
        "fiancee",
        "betrothed",
        "true love",
        "mother",
        "father",
        "brother",
        "little sister",
        "entire village",
        "puppy"),
      "discovery" -> PlotList(
        "government corruption",
        "secret shipment of contraband",
        "theft of ancient heirloom",
        "crazy old man",
        "underground resistance movement",
        "overheard conversation",
        "forbidden slave beauty girl",
        "murder",
        "plot against emperor",
        "angry spirits",
        "illegal martial arts contest"),
      "climax" -> PlotList(
        "allegory of government unity",
        "heroic sacrifice",
        "daring rescue",
        "restore land to peasant",
        "tragedy",
        "death of hero",
        "death of all enemy",
        "romance tragedy",
        "luck for fool",
        "blow up fort",
        "explosion",
        "big fight",
        "big big fight",
        "many death fight",
        "fight atop tower",
        "fight on mountain",
        "fight on boat",
        "fight in tower",
        "fight with monster",
        "fight with sword",
        "fight in forest",
        "fighting",
        "enormous fight",
        "fight",
        "fight",
        "fight"),
      "title" -> PlotMap(

        "templates" -> PlotList(
          "The [adjective] [noun]",
          "[adjective] [noun]",
          "[noun1] [noun2]",
          "[noun1] of the [adjective] [noun2]",
          "Enter The [noun]"),

        "adjective" -> PlotList(
          "Iron",
          "Dark",
          "Last",
          "First",
          "Middle",
          "Final",
          "Steel",
          "Crouching",
          "Hidden",
          "Surprising",
          "Unknown",
          "Black",
          "White",
          "Lost",
          "Sharp",
          "Blind",
          "Flying",
          "Sudden",
          "Dying"),
        "noun" -> PlotList(
          "Fist",
          "Dragon",
          "Tiger",
          "Leopard",
          "Crane",
          "Snake",
          "Monkey",
          "Mantis",
          "Panda",
          "Idol",
          "Blade",
          "Sword",
          "War",
          "Foot",
          "Dagger",
          "Path",
          "Way",
          "Death",
          "Eye"))),

    "wondermark" -> PlotMap(

      "templates" -> PlotList(
        "In [settingDescription] [settingLocation], a young [hero] stumbles across [maguffin], " +
        "which spurs him into conflict with [villain] with the help of [companion] and her [luggage], " +
        "culminating in [ending]."),

      "settingDescription" -> PlotList(
        "a neo-noir",
        "an alternate-history",
        "an ancient",
        "a post-apocalyptic",
        "a dystopian",
        "a VR-simulated",
        "a metaphorical",
        "an anachronistic",
        "a leather-clad",
        "a coal-powered",
        "a dragon-filled",
        "a shrill",
        "a sex-saturated",
        "a transhumanist",
        "a cyberpunk",
        "an interwar",
        "an agricultural",
        "a utopian",
        "a fascist",
        "a racist",
        "a sexist",
        "a utopian",
        "a laughably innacurate",
        "an unrelentingly criminal",
        "a cyberpunk",
        "a mechanoid",
        "a zombie-filled",
        "an all woman",
        "a nudist",
        "an oppressive",
        "a heteronormative",
        "a theocratic",
        "a dying",
        "a dilapidated",
        "a cartoonish",
        "a poverty-stricken",
        "an abandoned",
        "a forgotten",
        "an eternally dark",
        "a hellish",
        "a steampunk",
        "a feudal",
        "a female dominated"),
      "settingLocation" -> PlotList(
        "America",
        "Japan",
        "Soviet Russia",
        "Victorian Britain",
        "medieval Europe",
        "Aztec empire",
        "Atlantis",
        "terraformed Mars",
        "Antarctica",
        "one-way spaceflight",
        "Outer Rim world",
        "set from Road Warrior",
        "Dyson sphere",
        "Belle Epoque Paris",
        "Rome",
        "China",
        "Germany",
        "Athens",
        "Sparta",
        "Ancient Realm",
        "Robot Realm",
        "slave planet",
        "Trantor",
        "Everytown, USA",
        "Disneyland knockoff",
        "Old West",
        "military dictatorship",
        "North Korea",
        "Gotham City",
        "fortress of solitude",
        "barren wasteland",
        "dirigible",
        "battlefield",
        "cloud city",
        "mine",
        "American frontier",
        "Los Angeles",
        "San Francisco",
        "London",
        "New York",
        "jungle village",
        "mining camp",
        "explorer's ship",
        "biodome",
        "lunar base",
        "underwater city",
        "survivalist compound",
        "elven village",
        "castle",
        "bandit camp",
        "log cabin"),
      "hero" -> PlotList(
        "flying message courier",
        "student of metaphysics",
        "milquetoast office drone",
        "schlub with mild OCD",
        "farm boy with dreams",
        "techno-obsessed geek",
        "brooding loner",
        "wisecracking mercenary",
        "idealistic revolutionary",
        "journeyman inventor",
        "collector of oddities",
        "author self-insert",
        "robot seeking out its humanity",
        "far right revolutionary",
        "newly qualified veterinarian",
        "gentleman professor of archaeology",
        "patent clerk",
        "truth-seeking journalist",
        "feckless but obscenely wealthy heir",
        "trainee priest harbouring secret doubts",
        "nerd virgin",
        "sperg",
        "Joe Sixpack",
        "doting father",
        "documentarian",
        "reformed ex-convict",
        "magical negro",
        "scam artist",
        "FBI agent in deep cover",
        "pimp with a heart of gold",
        "anthropomorphic horse",
        "bi-sexual male prostitute",
        "Jackie Chan",
        "Rob Schneider",
        "convenience store clerk",
        "children's show host",
        "wannabe clown",
        "vampire hearthrob",
        "garage band drummer",
        "werewolf",
        "MRA",
        "prepper",
        "junkie",
        "private investigator",
        "earnest police detective",
        "Doctor Who",
        "rookie cop",
        "male exotic dancer",
        "extreme sport enthusiast",
        "retired commando",
        "closeted gay politician"),
      "maguffin" -> PlotList(
        "a magic diadem",
        "an arcane prophecy",
        "a dusty tome",
        "a crazy old man",
        "a dangerous alien artifact",
        "an enchanted sword",
        "an otherworldly portal",
        "a dream-inducing drug",
        "an encrypted data feed",
        "a time-travelling soldier",
        "an exiled angel",
        "a talking fish",
        "a talking robotic head from the far future",
        "a humming monitor-obelisk",
        "a skull of a dragon",
        "a jewel encrusted crown",
        "a plain wooden goblet",
        "a secret government programme",
        "a great and near-mythical creature thought to be extinct",
        "a mathematical insight of great beauty and practical use",
        "an improbably complicated conspiracy",
        "an additional ten commandments",
        "a priceless bejeweled dildo",
        "an incredible deal on car insurance",
        "a bag of dirty money",
        "a secret passageway",
        "a mind control device",
        "a rising caliphate",
        "a fantastical terrorist plot",
        "a superhero's secret identity",
        "an underground resistance movement",
        "the fabled fountain of youth",
        "his long-lost journal",
        "a mixtape from a lost love",
        "an escaped monkey",
        "a partially overheard conversation",
        "five kilos of Peruvian white",
        "his boss's dirty little secret",
        "a beautiful but forbidden slave girl",
        "a murder",
        "an alien landing",
        "God himself",
        "the ghost of his dead brother",
        "something that evokes long-buried memories"),
      "villain" -> PlotList(
        "a megalomaniacal dictator",
        "a government conspiracy",
        "a profit-obsessed corporation",
        "a sneering wizard",
        "supernatural monsters",
        "computer viruses made real",
        "murderous robots",
        "an army led by a sadist",
        "forces that encourage conformity",
        "a charismatic polititian on the rise",
        "humanity's selfish nature",
        "his own insecurity vis-a-vis girls",
        "bureaucrats from the civil service",
        "a twisted artificial intelligence intent on universal domination",
        "a well-meaning but foolish socialist",
        "an arrogant atheist",
        "Anonymous",
        "the prophet of a suicide cult",
        "a vast right-wing conspiracy",
        "the bias of the leftist media",
        "a sleeper cell of jihadis",
        "his evil twin",
        "a nasty bout of depression",
        "the Church",
        "the march of technological progress",
        "the Germans",
        "a vicious sexual predator",
        "a demonic tentacle monster",
        "a super-genius toddler",
        "the living dead",
        "his own mind",
        "his extremely dysfunctional family",
        "the lord of the spirit realm",
        "a fragile, old gravedigger",
        "his worst nightmares",
        "a demon loose from hell",
        "a ruthless serial killer",
        "an elusive and deadly sniper"),
      "companion" -> PlotList(
        "a sarcastic female techno-geek",
        "a tomboyish female mechanic",
        "a shape-shifting female assassin",
        "a leather-clad female in shades",
        "a girl who's always loved him",
        "a bookish female scholar with mousy brown hair",
        "a cherubic girl with pigtails and spunk",
        "a hot woman who inexplicable is attracted to the damaged protaganist",
        "a supposedly androgynous robot with an unaccountably sexy female voice and the suggestion of metallic breasts",
        "a dalmation furry blessed with a disturbingly attractive behind, big eyes, and full lips",
        "an androgynous boy who turns out to be an attractive young girl much to the relief of all",
        "a gorgeous blonde who is dumb as a brick but has excellent taste in futuristic boots",
        "a nunchuck-wielding femi-ninja",
        "a young female schoolteacher",
        "a chain-smoking, wizened MILF",
        "a sultry widow aching for release",
        "a lost, wanton stripper",
        "a streetsmart orphan girl",
        "a sassy female bounty hunter",
        "a wily gypsy girl",
        "a snooty female research scientist",
        "a cigar chomping lesbian",
        "a veteran female soldier who must take medication to keep her sane",
        "a librarian who hides her beauty behind a pair of thick-framed spectacles",
        "a feminine robot from heinlein's fantasies",
        "the leader of a polyamarous anti-capitalist collective",
        "a sheltered Amish girl",
        "a post-op transexual genderqueer womyn",
        "a tomboy princess",
        "an adolescent female love interest",
        "a fantasy girl made flesh",
        "a sassy black woman",
        "his ex",
        "his overprotective mother",
        "his soulmate",
        "an illegal immigrant who convinces him that she's a wereseal"),
      "luggage" -> PlotList(
        "wacky pet",
        "welding gear",
        "closet full of assault rifles",
        "reference book",
        "cleavage",
        "facility with magic",
        "condescending tone",
        "discomfort in formal wear",
        "propensity for being captured",
        "ability to ignore the lead character's blatant sexism and hostility towards her liberated sexuality",
        "copy of vogue",
        "interfering, spying duenna",
        "obsession with the bechdel test",
        "amazing martial arts abilities",
        "facility with reading",
        "vast wealth",
        "powerful father",
        "talking car",
        "mechanical third arm",
        "positive attitude",
        "encyclopedic knowledge of Woody Allen films",
        "obsessive boyfriend",
        "superior racial background",
        "case of the mondays",
        "fat acceptance activism",
        "silly feminine interests that turn out to be quite useful in the end",
        "surprising the male protagonist by impaling herself on his sexual organ for no apparent reason"),
      "ending" -> PlotList(
        "a fistfight atop a tower",
        "a daring rescue attempt",
        "a heroic sacrifice that no one will ever remember",
        "a philosophical arguement punctuated by violence",
        "a false victory with the promise of future danger",
        "the invocation of a spell at the last possible moment",
        "eternal love professed without irony",
        "the land restored to health",
        "authorial preaching through the mouths of the characters",
        "convoluted nonsense that squanders the readers' goodwill",
        "wish-fulfillment solutions to real-world problems",
        "a cliffhanger for the sake of prompting a series",
        "entirely avoidable tragedy",
        "restoration of a static and possibly repressive cis-phobic society",
        "all of humanity cursed to a bleak and desolate future",
        "a revelation that it was all a dream",
        "a technological singularity",
        "the death of every single character",
        "an inappropriately happy ending",
        "a quarter hour of gratuitous explosions",
        "an enlightened understanding of different cultures",
        "a romance that ends tragically due only to wounded pride",
        "an intense but pointless denouement that answers no questions",
        "the protaganist accepting his differences as strengths"),
      "title" -> PlotMap(
        "templates" -> PlotList(
          "[prefix][root]",
          "[prefix][root] II",
          "[prefix][root] Reloaded",
          "[prefix][root] IV",
          "Return of the [prefix][root]",
          "The [prefix][root]",
          "Rise of the [prefix][root]",
          "[prefix][root] vs. [prefix2][root]"),

        "prefix" -> PlotList(
          "Chrono",
          "Neuro",
          "Aero",
          "Cosmo",
          "Reve",
          "Necro",
          "Cyber",
          "Astro",
          "Psycho",
          "Steam",
          "Meta",
          "Black",
          "White",
          "Power",
          "Vibro",
          "Dark",
          "Death",
          "Inner",
          "Jingo",
          "Mega",
          "Anti",
          "Aqua"),
        "root" -> PlotList(
          "punk",
          "mech",
          "noiac",
          "poli",
          "naut",
          "phage",
          "droid",
          "bot",
          "blade",
          "tron",
          "mancer",
          "War",
          "man",
          "mage",
          "run",
          "fall",
          "path",
          "freeze",
          "crash"))),

    "linuxZealot" -> PlotMap(
      "templates" -> PlotList(
        "Linux Zealot is [nerdActivity].  [phonePlot]" +
            " Linux Zealot still [nerdCondition].",
        "Linux Zealot is [nerdActivity]. Suddenly, [forumPlot]" +
            " Linux Zealot still [nerdCondition].",
        "Linux Zealot is [nerdActivity].  [moralityPlay]"),

      "nerdActivity" -> PlotMap(
        "templates" -> PlotList(
          // social
          "seeking validation in IRC",
          "cleaning his fleshlight",
          "stalking the woman who lives down the street",
          // media consumption
          "watching pornographic Asian cartoons",
          "listening to a They Might Be Giants bootleg",
          "reading copyright-infringing space fiction",
          "watching a grainy xvid of Pirates of Silicon Valley",
          "learning japanese to better understand his favourite animes",
          "reading some Lego blogging",
          // political
          "re-encoding patent encumbered AACs to ogg vorbis",
          "writing a perl script to automatically obtain First post on Slashdot",
          "configuring his gentoo to use obscure supposedly performance-enhancing compilation flags"
          "daydreaming about The Singularity",
          "setting up a Tor relay",
          "mining for bitcoin using his grandmother's electricity",
          // technical
          "installing lunix on his Kindle",
          "compiling the latest point release of the lunix kernel",
          "adding features to his IRC bot",
          "building a personal RSS reader",
          "hand-editing configuration files",
          "devising a cockeyed templating system",
          // other
          "eating at his computer",
          "picking at a sore on his thigh",
          //fruitless linux
          "trying to [fruitlessLinuxEndeavor] in Linux",
          "sweating profusely after hours of trying to [fruitlessLinuxEndeavor] without booting into his Windows partition"),
       "fruitlessLinuxEndeavor" -> PlotList(
          "make Civilization: Call to Power work",
          "get his sound card to work",
          "get Flash to work in Firefox",
          "get his webcam to work",
          "watch Youtube videos",
          "copy and paste between applications")),
      "nerdCondition" -> PlotMap(
          "templates" -> PlotList(
            "has no [asset]",
            "has [problem]",
            "wears [fashion]",
            "lives [housing]",
            "[other]"),
          "asset" -> PlotList(
            "friends",
            "money",
            "girlfriend",
            "social life",
            "self-awareness"),
          "problem" -> PlotList(
            "bad breath",
            "horrible acne",
            "borderline personality disorder",
            "bedwetting episodes"),
          "fashion" -> PlotList(
            "underwear with his name written in them",
            "his hair in greasy dredlocks",
            "boots with cargo shorts",
            "a fucking purple cape"),
          "housing" -> PlotList(
            "with his mother",
            "in a cloud of cat urine",
            "in a basement",
            "in a fetid dorm room"),
          "other" -> PlotList(
            "smells like a soiled hobo",
            "hasn't touched a woman")),
      "forumPlot" -> PlotMap(
        "templates" -> PlotList(
          "a new comment appears on [forumSite], claiming \"[forumClaim]\"!" +
          "  Linux Zealot leaps into action, writing [forumResponse], but it" +
          " is too late :(."),
        "forumSite" -> PlotList(
          "Slashdot",
          "Kotaku",
          "Engadet",
          "Diaspora",
          "Hacker News",
          "r/programming",
          "reddit",
          "r/linux",
          "hardOCP",
          "Friendster",
          "GitHub",
          "Tom's Hardware"),
        "forumClaim" -> PlotList(
          "iOS is superior to Android",
          "women do not like men who don't bathe",
          "insecure, territorial nerds scare women away from technical subjects",
          "Computer hacking is a crime",
          "Visual Studio is better than emacs",
          "Gnome removes configuration options",
          "apt-get is not an intuitive way for your grandmother to install software",
          "Many computer games are flagrantly sexist",
          "Lunix is not ready for the desktop",
          "Software patents promote innovation",
          "Pirating games hurts publishers",
          "Steam on linux doesn't have many games",
          "Libre Office is often incompatible with Microsoft Office",
          "Lunix is based on code stolen from SCO",
          "Stealing music does economic harm to musicians",
          "Richard Stallman is not a charismatic ambassador for Free Software",
          "The BSD license is more free than the GPL",
          "Lunix has poor sound support",
          "Eric S. Raymond is wrong about Gun Control",
          "Foreign programmers are just as good as local ones, or better",
          "Electronica is not very good",
          "Cartoons and comic books are for children",
          "Your son may be a computer hacker",
          "The Gnu Public License is a harmful computer virus",
          "Oracle is more appropriate for many enterprises than MariaDB",
          "C is not a good language for writing a GUI",
          "Apple laptops are well designed, desirable status symbols",
          "Wikipedia can sometimes be unreliable because anybody can edit it",
          "Managers are usually better at making strategic decisions than their reports",
          "XKCD is self-fellating nerd tripe",
          "Newspaper paywalls are needed to secure a revenue stream",
          "Having three completely independent copy/paste buffers in X-Windows is confusing",
          "Monads are difficult for most programmers to understand",
          "LISP is not a particularly useful programming language"),
        "forumResponse" -> PlotList(
          "a seven-page manifesto",
          "a painstaking point-by-point rebuttal",
          "a scathing grammar correction",
          "a mistakingly double-posted one line insult",
          "a typo-laden tangential diatribe",
          "an angry blog post recounting Free Software principles",
          "an exhaustively researched dissertation",
          "a pedantic correction regarding the naming of GNU/Linux",
          "a furious screed fixating on an innocuous detail",
          "a laboured, windy explanation of exactly how they are wrong",
          "the first of months of comment-stalking replies",
          "a comment epitomising a breathless and maniacal outlook",
          "\"NO U\"")),
      "phonePlot" -> PlotMap(
        "templates" -> PlotList(
          "Out of the blue, his [friend] calls, in need of " +
            "[phoneFavor]. Linux Zealot to the rescue:" +
            " \"[phoneSolution]!\"  [phoneMood]",
          "Gradually, he realizes the strange noise he " +
            "has been hearing intermittently is his telephone.  It " +
            "is his [friend], calling to ask for [phoneFavor].  " +
            "\"[phoneSolution]\", he [phoneDelivery].  [phoneMood]"),
        "friend" -> PlotList(
          // linux zealot does not have friends, just family.
          "grandmother",
          "grandfather",
          "mother",
          "stepmother",
          "stepfather",
          "half-brother",
          "sister",
          "uncle",
          "aunt",
          "nephew",
          "niece"),
        "phoneFavor" -> PlotList(
          "help with organizing recipes",
          "a simple malware scan",
          "a formula for an Excel spreadsheet",
          "\"how to find the facebook\"",
          "a way to get rid of annoying popups",
          "instructions on how to play some music",
          "some pointers on configuring Outlook"),
        "phoneSolution" -> PlotList(
          "You just need to install GNU/Linux",
          "If you installed GNU/Linux you wouldn't need to worry about that",
          "GNU/Linux makes this easier, try that instead",
          "GNU/Linux doesn't have that problem",
          "Format and install GNU/Linux",
          "Read the Gnu Manifesto at http://gnu.org/gnu/manifesto.html to see why you need to use GNU/Linux",
          "Quit using winblowz"),
        "phoneDelivery" -> PlotList(
          "shouts, gesticulating stiffly",
          "barks, briefly startling himself",
          "mumbles rotely, fidgeting uncomfotably all the while",
          "says, his voice cracking with disuse",
          "yells, completely unaware of his own volume",
          "sneers, free of the average man's sense of tact",
          "mutters ominously, as if he might blow up a school at any moment"),
        "phoneMood" -> PlotList(
          // [this] Linux Zealot still [nerdCondition]
          "Sadly, although he thinks he has solved the problem,",
          "He enjoys a brief sense of smug superiority, but",
          "In addition to having done more harm than good,",
          "Sadly, Despite his unshakeable self-righteousness,",
          "Slowly, it dawns on him that this is the first time he has spoken in a week. ",
          "Though the ordeal of talking on the telephone is over,",
          "While he has likely ensured his family will leave him alone for a while,")),
      
      "moralityPlay" -> PlotMap(
        "templates" -> PlotList(
          // for some reason, linux zealot acts on his rigid belief system.  He daydreams of glory!
          // Later, his action has entirely predictable consequences.  Poor Linux Zealot!
          "[mobileReason], [mobileContractViolation].  Surely, [idol] would approve!  [mobileDisaster].  " +
              "Poor Linux Zealot!",
          "[musicBelief], he [musicFeel] while he [musicAction].  Unfortunately [musicConsequence].  " +
              "Poor Linux Zealot!"),
        "idol" -> PlotList(
          "Richard Stallman",
          "Eric Raymond",
          "Ray Kurzweil",
          "Ayn Rand",
          "Julian Assange",
          "Linux Torvez",
          "Lineaus Torvaldez",
          "Linus Torfalds",
          "Leenus Torvhalds",
          "Linnus Torvhalds"),
        "mobileReason" -> PlotList(
          "When a relative asks for help with Instagram",
          "After reading an article about NSA spying",
          "Consumed with rage at Apple's success",
          "In a fit of Android rapture",
          "In his crusade to impose the Linux Way on everything he sees",
          "In the grip of his compulsion to fix things that already work"),
        "mobileContractViolation" -> PlotList(
          "he jailbreaks an Android phone",
          "he takes the SIM card out of a 6-month old iPhone and puts it in a 3-year-old Android",
          "he removes carrier improvements to Android",
          "he opens up a phone and rewires the headphone jack",
          "he replaces the battery in a phone with a cheap unbranded knockoff"),
        "mobileDisaster" -> PlotList(
          "Several hours later, the phone overheats, filling the room with toxic vapours",
          "One month later, a fine for unauthorized use is billed",
          "Three days later, a routine update rolls back all of LZ's useless work",
          "Belatedly, LZ realizes he has premanently deleted every email, text, and photo",
          "Now the phone can do everything it could do before, except answer phone calls"),
        // [musicBelief], he [musicFeel] while he [musicAction]. Unfortunately [musicConsequence]
        "musicBelief" -> PlotList(
          "Believing that information wants to be free",
          "As a form of protest against restrictive copyright law",
          "Because he believes big media companies just rip off artists anyway",
          "After reading on Slashdot about how evil music companies are",
          "Convinced by a breathless article in favor of repealing all copyright forever",
          "In a plan to follow in the footsteps of WikiLeaks founder Julian Assange",
          "As an act of civil disobedience",
          "In an attempt to impress a theoretical future woman"),
        "musicFeel" -> PlotList(
          "doesn't feel the least bit guilty",
          "knows he's done nothing wrong",
          "shifts in his chair righteously",
          "feels quite pleased with himself",
          "can hardly contain his excitement",
          "shouts \"take that\""),
        "musicAction" -> PlotMap(
          "templates" -> PlotList(
            "steals the latest album by [musicArtist] using Gnutella",
            "internet-steals every album ever released by [musicArtist]",
            "illegally posts a song by [musicArtist] to YouTube",
            "lists CD-R copies of [musicArtist] Albums on eBay",
            "illegally distributes the first [musicArtist] album to 50,000 internet \"friends\"",
            "steals the chart topping hit by [musicArtist] with the help of an online \"friend\"",
            "breaks the copy protection software on the new [musicArtist] album",
            "burns a stolen FLAC of his favorite [musicArtist] to CD"),
          "musicArtist" -> PlotList(
            "They Might Be Giants",
            "\"Weird Al\" Yankovic",
            "MC Chris",
            "Jonathan Coulton",
            "Anthrax",
            "Rush",
            "Tim Minchin",
            "Garfunkle and Oates",
            "Tom Lehrer",
            // If only we had some simple way to concoct fake band names...
            "Pink Floyd",
            "Frank Zappa",
            "Captain Beefheart",
            "Art of Noise",
            "Firesign Theatre",
            "P.D.Q. Bach",
            "Tool",
            "Shonen Knife",
            "Nine Inch Nails")),
        "musicConsequence" -> PlotList(
          "because he didn't support the artist, the band breaks up citing poor sales",
          "the FBI detects this and shows up with a warrant for his arrest",
          "the RIAA finds out and files a quarter million dollar lawsuit against him",
          "his ISP finds out and disconnects his internet for copyright infrigement",
          "no one notices or cares what he does and he makes no impact on the world",
          "his computer catches a virus from shady music-stealing websites")),
      "title" -> PlotMap(
        "templates" -> PlotList(
          "Linux Zealot [adventure]",
          "linux zealot and the [nasty] [thing]",
          "linux zealot vs. the [nasty] [thing]"),
        "adventure" -> PlotList(
          "Stays at Home",
          "Types on his Keyboard",
          "Takes a Study Break",
          "Drinks Store-Brand Soda",
          "Sits down",
          "waits for a package",
          "replies",
          "returns",
          "reposted",
          "picks his toes",
          "Remains Seated",
          "Doesn't Brush his Teeth",
          "Forgets to Excercise",
          "Wonders What Day It Is",
          "Leans Forward On His Desk"),
        "nasty" -> PlotList(
          "odiferous",
          "malodorous",
          "fetid",
          "noxious",
          "sulphurous",
          "noisesome",
          "hairy",
          "bearded",
          "dusty",
          "decomposing",
          "rotting",
          "decaying",
          "putrescent",
          "rancid",
          "leprous",
          "somewhat whiffy",  // it's not always so bad, is it?
          "quarantined",
          "biohazardous",
          "filth-encrusted",
          "spittle-flecked",
          "pus-soaked",
          "mucuous-smeared",
          "repulsive",
          "revolting"
          ),
        "thing" -> PlotList(
          "keyboard",
          "mouse ball",
          "cable",
          "fantasy novel",
          "pizza",
          "sandwich",
          "socks",
          "t-shirt",
          "mug",
          "soda bottle",
          "socks",
          "fleshlight",
          "screwdriver",
          "pocket-knife",
          "sweatpants",
          "sandals",
          "sneakers",
          "hat",
          "knapsack",
          "toothbrush",
          "soap",
          "bathrobe",
          "mattress"))))
}
