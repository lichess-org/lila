package lila.shutup

import org.specs2.mutable._
import org.specs2.specification._

class DetectTest extends Specification {

  "detector" should {
    "find one bad word" in {
      Detect.find("fuck") must_== List("fuck")
      Detect.find("well fuck me") must_== List("fuck")
    }
    "find many bad words" in {
      Detect.find("fuck that shit") must_== List("fuck", "shit")
      Detect.find("Beat them cunting nigger faggots with a bitchin' fuckstick") must_==
        List("cunting", "nigger", "faggots", "fuckstick")
    }
    "find no bad words" in {
      Detect.find("") must_== Nil
      Detect.find("hello there") must_== Nil
      Detect.find("A sonnet is a poetic form which originated in Italy; Giacomo Da Lentini is credited with its invention.") must_== Nil
      Detect.find("computer analysis") must_== Nil
    }
    "compute ratio" in {
      Detect.ratio("fuck that shit") must_== 2d/3
      Detect.ratio("Beat them cunting nigger faggots with a bitchin' fuckstick") must_== 4d/9
      Detect.ratio("hello there") must_== 0
      Detect.ratio("") must_== 0
    }
  }
}
