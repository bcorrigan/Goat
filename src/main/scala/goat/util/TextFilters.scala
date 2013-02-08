package goat.util

import scala.util.matching.Regex

object TextFilters {

  val scotch_pairs = List[(Regex, String)](
    ("""(?i)\byes\b""".r, "aye"),
    ("""(?i)there""".r, "thar"),
    ("""(?i)eir\b""".r, "ar"),
    ("""(?i)\babout\b""".r, "aboot"),
    ("""(?i)\bhe\b""".r, "'e"),
    ("""(?i)\bthem\b""".r, "'em"),
    ("""(?i)\bhim""".r, "'im"),
    ("""(?i)\bout of\b""".r, "outta"),
    ("""(?i)\bof course\b""".r, "'course"),
    ("""(?i)\bof\b""".r, "o'"),
    ("""(?i)\band\b""".r, "an'"),
    ("""(?i)\bto\b""".r, "ta"),
    ("""(?i)at ta\b""".r, "atta"),
    ("""(?i)ot ta\b""".r, "otta"),
// tog:tag
// that:tha
// the:tha
    ("""(?i)\bwouldn't have\b""".r, "wouldn'ta"),
    ("""(?i)\b(cannot|can not|can't)\b""".r, "cannae"),
    ("""(?i)\b(don't|does not|doesn't|didn't|did not)\b""".r, "dinnae"),
// 're$:r
    ("""(?i)\bfor\b""".r, "fer"),
    ("""(?i)\bver\b""".r, "'er"),
// ber$:b'r
    ("""(?i)\bevery\b""".r, "ev'ry"),
    ("""(?i)en\b""".r, "'n"),
    ("""(?i)\bif\b""".r, "if'n"),
    ("""(?i)\benl""".r, "'nl"),
    ("""(?i)\beng""".r, "'ng"),
    ("""(?i)ing\b""".r, "in'"),
// ment:mn't
    ("""(?i)\bes""".r, "'s"),
// ^ex:'s
    ("""(?i)\bknow\b""".r, "ken"),
    ("""(?i)\bknows\b""".r, "kens"),
    ("""(?i)\b(small|tiny|little)""".r, "wee"),
    ("""(?i)\b(baby|child|toddler)\b""".r, "bairn"),
    ("""(?i)\b(babies|children)\b""".r, "bairns"),
    ("""(?i)\bdog\b""".r, "dag"),
    ("""(?i)\bout\b""".r, "oot"),
    ("""(?i)\bdown\b""".r, "doon"),

    // from the first page of http://mudcat.org/scots/index.cfm
    ("""(?i)\babove\b""".r, "'boon"),
    ("""(?i)\bbelow\b""".r, "ablow"),
    ("""(?i)\bbeyond\b""".r, "'yont"),
    ("""(?i)\bit was\b""".r, "'twas"),
    ("""(?i)\bamong\b""".r, "'mang"),
    ("""(?i)\bamidst\b""".r, "'midst"),
    ("""(?i)\bsomeone\b""".r, "a bodie"),
    ("""(?i)\b(a lot|often)\b""".r, "a haip"),
    ("""(?i)\beverywhere\b""".r, "a'place"),
    ("""(?i)\balmost\b""".r, "awmaist"),
    ("""(?i)\balready\b""".r, "awreadies"),
    ("""(?i)\b(all right|alright|allright)\b""".r, "awricht"),
    ("""(?i)\beverything else\b""".r, "aa ither thin"),
    ("""(?i)\beverything\b""".r, "aathing"),
    ("""(?i)\beveryone\b""".r, "awbodie"),
    ("""(?i)\b(going on|being done|the matter with)\b""".r, "adae"),
    ("""(?i)\badmit\b""".r, "admeet"),
    ("""(?i)\bcompared with\b""".r, "abi's"),
    ("""(?i)\boff\b""".r, "aff"),
    ("""(?i)\baway from\b""".r, "aff o"),
    ("""(?i)\boften\b""".r, "aft"),
    ("""(?i)\bagainst\b""".r, "agin"),
    ("""(?i)\boff line\b""".r, "agley"),
    ("""(?i)\bahead\b""".r, "aheid"),
    ("""(?i)\bbehind\b""".r, "ahint"),
    ("""(?i)\bperhaps\b""".r, "aiblins"),
    ("""(?i)\beight\b""".r, "aicht"),
    ("""(?i)\bits own\b""".r, "its ain"),
    //end of translations from web page

    ("""(?i)\b(no|not)\b""".r, "nae"),
    ("""(?i)n't_have\b""".r, "n'tve"),
    ("""(?i)\b(is|are)\b""".r, "be"),


// have:haf
// abl:'bl
    ("""(?i)\byou\b""".r, "ye"),
    ("""(?i)\byour""".r, "yer"),
//  noth:nuth
// ^this$:'tis
    ("""(?i)\bhere""".r, "'ere"),
    ("""(?i)\bat_a\b""".r, "atta"),
    ("""(?i)with\b""".r, "wit'"),
    // these next few clean up after the previous one, boo
    ("""(?i)wit' '""".r, "wit '"),
    ("""(?i)wit' t""".r, "wit t"),
    ("""(?i)wit' w""".r, "wit w"),
    ("""(?i)wit' y""".r, "wit y"),
    ("""(?i)\bget a""".r, "git a"),
// ally$:'lly$
// ered$:'red
    ("""(?i)\binto\b""".r, "inta"),
    ("""(?i)\bbefore""".r, "'fore"),
    ("""(?i)\bmy""".r, "me"),
    ("""(?i)\bi think\b""".r, "methinks"),
    ("""(?i)\bnae w""".r, "na w"),
    ("""(?i)\bnae o""".r, "na o"),
    ("""(?i)\bone\b""".r, "'un"),
    ("""(?i)\b'un a""".r, "one a"),
    ("""(?i)\bisn't\b""".r, "ain't"),
    ("""(?i)\bso th\b""".r, "s'th"),
// ned$:n'd
    ("""\bbecause\b""".r, "'cause")
  );

  def replace(string: String, regex: Regex, replacement: String): String =
    regex.replaceAllIn(string, m =>
      if (m.matched == m.matched.toUpperCase) replacement.toUpperCase
      else if (m.matched == m.matched.capitalize) replacement.capitalize
      else replacement )

  def filter(string: String, subs: List[(Regex, String)]): String =
    subs.foldLeft(string)( (str, pair) => replace(str, pair._1, pair._2))

  def scotchify(string: String): String = filter(string, scotch_pairs)

}
