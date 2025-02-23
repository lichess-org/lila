package lila.security

import java.time.Instant

class FloodTest extends munit.FunSuite:

  import Flood.*

  private def isDup = duplicateMessage

  private def m(s: String) = Message(s, Instant.now)

  private val str = "Implementation uses dynamic programming (Wagner–Fischer algorithm)"
  private val msg = m(str)

  test("same"):
    assert(!isDup(msg, Nil))
    assert(!isDup(msg, List(m("foo"))))
    assert(isDup(msg, List(msg)))
    assert(isDup(msg, List(m("foo"), msg)))
    assert(isDup(msg, List(m("foo"), msg, m("bar"))))
    assert(!isDup(msg, List(m("foo"), m("bar"), msg)))
  test("levenshtein"):
    assert(isDup(msg, List(m(s"$str!"))))
    assert(isDup(msg, List(m(s"-$str"))))
    assert(isDup(msg, List(m(s"$str!!"))))
    assert(isDup(msg, List(m(s"$str!!!!"))))
    assert(isDup(msg, List(m(str.take(str.length - 1)))))
    assert(!isDup(msg, List(m(str.take(str.length / 2)))))
    assert(!isDup(msg, List(m(s"$str$str"))))

    assert(isDup(m("hey"), List(m(s"hey!"))))
    assert(!isDup(m("hey"), List(m(s"hey!!"))))
