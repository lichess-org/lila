package lila.api

import org.specs2.mutable.Specification

class LpvGameRegexTest extends Specification {

  val re = LpvGameRegex("boo.org:8080")

  def group(r: scala.util.matching.Regex, t: String, g: Int) = r.findFirstMatchIn(t).map(_.group(g))

  "forum links" >> {
    "match basic" >> re.linkRenderRe.matches("https://boo.org:8080/1234abcd")

    "match color" >> re.linkRenderRe.matches("https://boo.org:8080/abcd1234/white")

    "match relative" >> re.linkRenderRe.matches("/12345678/black#123")

    "match no scheme" >> re.linkRenderRe.matches("boo.org:8080/abcdefgh#123")

    "match full game id" >> re.linkRenderRe.matches("boo.org:8080/12345678abcd#12")

    "fail wrong host" >> !re.linkRenderRe.matches("boo.org:1234/zyxwvuts")

    "fail too long" >> !re.linkRenderRe.matches("/abcdefghijkl1234")

    "fail bad ply" >> !re.linkRenderRe.matches("/abcdefgh#12noes")

    "fail bad color" >> !re.linkRenderRe.matches("https://boo.org:8080/12345678/red#22")

    "fail tournaments" >> !re.linkRenderRe.matches("https://boo.org:8080/tournament/ABCD1234")

    "fail studies" >> !re.linkRenderRe.matches("https://boo.org:8080/study/abcd1234/bcde2345")

    "fail site header" >> !re.linkRenderRe.matches("[Site=\"https://boo.org:8080/abcd1234\"]")

    "fail mid-line" >> !re.linkRenderRe.matches("once upon a time /1234abcd/white#4")

    "fail usernames" >> !re.linkRenderRe.matches("/@/thibault")

    "fail full id AND color" >> !re.linkRenderRe.matches("/12345678abcd/black")

    "extract game id 1" >> group(re.linkRenderRe, "/1234abcd", 2).contains("1234abcd")

    "extract game id 2" >>
      group(re.linkRenderRe, "https://boo.org:8080/abcd1234/black#12", 2).contains("abcd1234")

    "extract game id 3" >>
      group(re.linkRenderRe, "boo.org:8080/abcd1234wxyz#44", 2).contains("abcd1234")
  }
  "blog links" >> {
    "match basic" >> re.gamePgnsRe.matches("https://boo.org:8080/abcdefgh")

    "match no scheme" >> re.gamePgnsRe.matches("boo.org:8080/12345678/white#123")

    "match full id" >> re.gamePgnsRe.matches("boo.org:8080/1234abcd1234#22")

    "fail wrong host" >> !re.gamePgnsRe.matches("boo.org:1234/abcdefgh")

    "fail relative" >> !re.gamePgnsRe.matches("/12345678")

    "fail mid-line" >> !re.gamePgnsRe.matches("oompa loompa https://boo.org:8080/abcd1234")

    "fail site header" >> !re.gamePgnsRe.matches("[Site=\"https://boo.org:8080/1234abcd\"]")

    "fail tournaments" >> !re.gamePgnsRe.matches("https://boo.org:8080/tournament/ABCD1234")

    "fail usernames" >> !re.gamePgnsRe.matches("boo.org:8080/@/thibault")

    "fail wrong length" >> !re.gamePgnsRe.matches("boo.org:8080/12345678bad/black")

    "extract game id 1" >>
      group(re.gamePgnsRe, "boo.org:8080/zyxwvuts/black", 1).contains("zyxwvuts")

    "extract game id 2" >>
      group(re.gamePgnsRe, "boo.org:8080/1234abcd1234#123", 1).contains("1234abcd")
  }
}
