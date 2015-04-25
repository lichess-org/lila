package lila.shutup

import org.specs2.mutable._
import org.specs2.specification._

class DetectTest extends Specification {

  private def find(t: String) = Analyser(t).badWords
  private def ratio(t: String) = Analyser(t).ratio

  "detector" should {
    "find one bad word" in {
      find("fuck") must_== List("fuck")
      find("well fuck me") must_== List("fuck")
    }
    "find many bad words" in {
      find("fuck that shit") must_== List("fuck", "shit")
      find("Beat them cunting nigger faggots with a bitchin' fuckstick") must_==
        List("cunting", "nigger", "faggots", "fuckstick")
    }
    "find no bad words" in {
      find("") must_== Nil
      find("hello there") must_== Nil
      find("A sonnet is a poetic form which originated in Italy; Giacomo Da Lentini is credited with its invention.") must_== Nil
      find("computer analysis") must_== Nil
    }
    "compute ratio" in {
      ratio("fuck that shit") must_== 2d/3
      ratio("Beat them cunting nigger faggots with a bitchin' fuckstick") must_== 4d/9
      ratio("hello there") must_== 0
      ratio("") must_== 0
    }
  }
}
