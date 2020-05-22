package lila.security

import org.specs2.mutable.Specification
import org.joda.time.Instant

class FloodTest extends Specification {

  import Flood._

  def isDup = duplicateMessage _

  def m(s: String) = Message(s, Instant.now)

  val str = "Implementation uses dynamic programming (Wagner–Fischer algorithm)"
  val msg = m(str)

  "find duplicate" should {
    "same" in {
      isDup(msg, Nil) must beFalse
      isDup(msg, List(m("foo"))) must beFalse
      isDup(msg, List(msg)) must beTrue
      isDup(msg, List(m("foo"), msg)) must beTrue
      isDup(msg, List(m("foo"), msg, m("bar"))) must beTrue
      isDup(msg, List(m("foo"), m("bar"), msg)) must beFalse
    }
    "levenshtein" in {
      isDup(msg, List(m(s"$str!"))) must beTrue
      isDup(msg, List(m(s"-$str"))) must beTrue
      isDup(msg, List(m(s"$str!!"))) must beTrue
      isDup(msg, List(m(s"$str!!!!"))) must beTrue
      isDup(msg, List(m(str.take(str.length - 1)))) must beTrue
      isDup(msg, List(m(str.take(str.length / 2)))) must beFalse
      isDup(msg, List(m(s"$str$str"))) must beFalse

      isDup(m("hey"), List(m(s"hey!"))) must beTrue
      isDup(m("hey"), List(m(s"hey!!"))) must beFalse
    }
  }
}
