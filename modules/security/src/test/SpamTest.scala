package lila.security

import org.specs2.mutable.Specification

class SpamTest extends Specification {

  import Spam._

  val foobar = """foo bar"""
  val _vc = """almost as cool as lichess.  \r\n\r\nhttps://www.velocitychess.com/ref/2573698"""
  val _c2 = """https://chess24.com?ref=masterpart"""
  val _cb = s"""with $cb cheat. http://$cb.com/ http://$cb.com/bot/ChessBot.RAR"""

  "spam" should {
    "detect" in {
      detect(foobar) must beFalse
      detect(_vc) must beTrue
      detect(_c2) must beTrue
      detect(_cb) must beTrue
    }
    "replace" in {
      replace(foobar) must_== foobar
      replace(_vc) must_== """almost as cool as lichess.  \r\n\r\nhttps://www.velocitychess.com"""
      replace(_c2) must_== """https://chess24.com"""
      replace(_cb) must_== s"""with $tosUrl cheat. $tosUrl $tosUrl"""
    }
  }
}
