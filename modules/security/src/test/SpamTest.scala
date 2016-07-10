package lila.security

import org.specs2.mutable.Specification
import play.api.libs.json._

class SpamTest extends Specification {

  import Spam._

  val foobar = """foo bar"""
  val _vc = """almost as cool as lichess.  \r\n\r\nhttps://www.velocitychess.com/ref/2573698"""
  val _cb = s"""with $cb cheat. http://$cb.com/ http://$cb.com/bot/ChessBot.RAR"""
  val _in = s"""his webpage: http://$in/2014/04/23/lichess-org-chess-bot/ http://$in/pages/lichess-bot/#lichess-bot"""

  "spam" should {
    "detect" in {
      detect(foobar) must beFalse
      detect(_vc) must beTrue
      detect(_cb) must beTrue
      detect(_in) must beTrue
    }
    "replace" in {
      replace(foobar) must_== foobar
      replace(_vc) must_== """almost as cool as lichess.  \r\n\r\nhttps://www.velocitychess.com"""
      replace(_cb) must_== s"""with $tosUrl cheat. $tosUrl $tosUrl"""
      replace(_in) must_== s"""his webpage: $tosUrl $tosUrl"""
    }
  }
}
