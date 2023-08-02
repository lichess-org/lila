package lila.api

import lila.common.config.NetDomain

class LpvGameRegexTest extends munit.FunSuite:

  val re = LpvGameRegex(NetDomain("boo.org:8080"))

  val pass = """ here's a post:
https://boo.org:8080/1234abcd
https://boo.org:8080/1234abcd1234
https://boo.org:8080/1234abcd#123
https://boo.org:8080/1234abcd/white
https://boo.org:8080/1234abcd/black#123
boo.org:8080/1234abcd
boo.org:8080/1234abcd1234
boo.org:8080/1234abcd#123
boo.org:8080/1234abcd/white
boo.org:8080/1234abcd/black#123
"""
  val fail = """ here's another:
boo.org:1234/zyxwvuts
boo.org:8080/abcdefghijkl1234
boo.org:8080/abcdefgh#12noes
https://boo.org:8080/12345678/red#22
https://boo.org:8080/tournament/ABCD1234    
https://boo.org:8080/study/abcd1234/bcde2345
[Site="https://boo.org:8080/abcd1234"]
once upon a time boo.org:8080/1234abcd/white#4
https://boo.org:8080/@/thibault
 https://boo.org:8080/1234abcd
"""
  test("forum links match") {
    re.forumPgnCandidatesRe.findAllMatchIn(pass).foreach { m =>
      assert(re.gamePgnRe.matches(m.group(1)), m.group(1))
    }
  }
  test("forum links fail") {
    re.forumPgnCandidatesRe.findAllMatchIn(fail).foreach { m =>
      assert(!re.gamePgnRe.matches(m.group(1)), m.group(1))
    }
  }
