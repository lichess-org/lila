package lila.shutup

import org.specs2.mutable._

class AnalyserTest extends Specification {

  private def find(t: String)  = Analyser(t).badWords
  private def dirty(t: String) = Analyser(t).dirty
  private def ratio(t: String) = Analyser(t).ratio

  "detector" >> {
    "find one bad word" >> {
      find("fuck") === List("fuck")
      find("well fuck me") === List("fuck")
    }
    "find many bad words" >> {
      find("fucked that shit") === List("fucked", "shit")
      find("Beat them cunting nigger faggots with a communist dick") ===
        List("cunting", "nigger", "faggots", "dick")
    }
    "find no bad words" >> {
      find("") === Nil
      find("hello there") === Nil
      find(
        "A sonnet is a poetic form which originated in Italy; Giacomo Da Lentini is credited with its invention."
      ) === Nil
      find("computer analysis") === Nil
    }
    "find badly spelled words" >> {
      find("fuk") === List("fuk")
      find("well fuk me") === List("fuk")
      find("foo ashole bar fukd") === List("ashole", "fukd")
    }
    "find variants" >> {
      find("cunt kunt cunting kawa kunting") === List("cunt", "kunt", "cunting", "kunting")
      find("ass as ashole") === List("ass", "ashole")
    }
    "find plurals" >> {
      find("asses cunts kunts cuntings kawas kuntings") === List(
        "asses",
        "cunts",
        "kunts",
        "cuntings",
        "kuntings"
      )
    }
    "find phrases" >> {
      find("I think you suck") === List("you suck")
      find("you should suck my") === List("suck my")
    }
    "50 shades of fuck" >> {
      find("fuck fffuuk fektard feak fak phuk") === List("fuck", "fffuuk", "fektard", "fak", "phuk")
    }
    "compute ratio" >> {
      ratio("fuck that shit") === 2d / 3
      ratio("Beat them cunting nigger faggots with a communist dick") === 12d / 9
      ratio("hello there") === 0d
      ratio("") === 0d
    }
    "chessbot variations" >> {
      find("foo chessbot bar") === List("chessbot")
      find("foo chess-bot bar") === List("chess-bot")
      find("foo chess_bot bar") === List("chess_bot")
      find("foo chess-bot.com bar") === List("chess-bot.com")
      find("foo chess_bot_com bar") === List("chess_bot_com")
      find("foo chessbotcom bar") === List("chessbotcom")
      find("foo http://chess-bot.com bar") === List("chess-bot.com")
    }
    "rat false positives" >> {
      find("test rat is rate some rates what rated") === List("rat")
    }
    "russian chars" >> {
      find("sеx") === List("sex")
    }
    "russian" >> {
      find("сука пизда") === List("сука", "пизда")
    }
  }
  "dirty" >> {
    "prod msg" >> {
      dirty(
        """Hello fucking arab. It's morning here I am getting ready to fuck your smelly mom and sister together today. Just wanna inform you ;)"""
      ) must beTrue
    }
  }
}
