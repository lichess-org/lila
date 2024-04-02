package lila.api

import scala.util.matching.Regex

import lila.core.config.NetDomain

class LpvGameRegexTest extends munit.FunSuite:

  val re = LpvGameRegex(NetDomain("boo.org:8080"))

  def chainMatch(text: String, urlRe: Regex, pathRe: Regex): Option[String] = text match
    case urlRe(g1) => pathRe.findFirstMatchIn(g1).map(_.group(2))
    case _         => None

  def forumMatch(text: String, regex: Regex): Option[String] =
    chainMatch(text, re.forumPgnCandidatesRe, regex)

  def forumMatches(text: String, regex: Regex): Boolean =
    forumMatch(text, regex).isDefined

  def blogMatch(text: String, regex: Regex): Option[String] =
    chainMatch(text, re.blogPgnCandidatesRe, regex)

  def blogMatches(text: String, regex: Regex): Boolean =
    blogMatch(text, regex).isDefined

  test("forum links match basic") {
    assert(forumMatches("https://boo.org:8080/1234abcd", re.gamePgnRe))
  }

  test("forum links match color") {
    assert(forumMatches("https://boo.org:8080/abcd1234/white", re.gamePgnRe))
  }

  test("forum links fail relative") {
    assert(!forumMatches("/12345678/black#123", re.gamePgnRe))
  }

  test("forum links match no scheme") {
    assert(forumMatches("boo.org:8080/abcdefgh#123", re.gamePgnRe))
  }

  test("forum links match full game id") {
    assert(forumMatches("boo.org:8080/12345678abcd#12", re.gamePgnRe))
  }

  test("forum links fail wrong host") {
    assert(!forumMatches("boo.org:1234/zyxwvuts", re.gamePgnRe))
  }

  test("forum links fail too long") {
    assert(!forumMatches("boo.org:8080/abcdefghijkl1234", re.gamePgnRe))
  }

  test("forum links fail bad ply") {
    assert(!forumMatches("https://boo.org:8080/abcdefgh#12noes", re.gamePgnRe))
  }

  test("forum links fail bad color") {
    assert(!forumMatches("https://boo.org:8080/12345678/red#22", re.gamePgnRe))
  }

  test("forum links fail tournaments") {
    assert(!forumMatches("https://boo.org:8080/tournament/ABCD1234", re.gamePgnRe))
  }

  test("forum links fail studies") {
    assert(!forumMatches("https://boo.org:8080/study/abcd1234/bcde2345", re.gamePgnRe))
  }

  test("forum links fail chapter regex no chapter") {
    assert(!forumMatches("boo.org:8080/study/abcd1234", re.chapterPgnRe))
  }

  test("forum links fail study regex on study w chapter") {
    assert(!forumMatches("boo.org:8080/study/abcd1234/abcd1234", re.studyPgnRe))
  }

  test("forum links extract studyid") {
    assert(forumMatch("https://boo.org:8080/study/abcd4890", re.studyPgnRe).has("abcd4890"))
  }

  test("forum links extract chapter") {
    assert(forumMatch("https://boo.org:8080/study/abcd1234/bcde2345", re.chapterPgnRe).has("bcde2345"))
  }

  test("forum links fail site header") {
    assert(!forumMatches("[Site=\"https://boo.org:8080/abcd1234\"]", re.gamePgnRe))
  }

  test("forum links fail mid-line") {
    assert(!forumMatches("once upon a time /1234abcd/white#4", re.gamePgnRe))
  }

  test("forum links fail usernames") {
    assert(!forumMatches("/@/thibault", re.gamePgnRe))
  }

  test("forum links fail full id AND color") {
    assert(!forumMatches("/12345678abcd/black", re.gamePgnRe))
  }

  test("forum links extract game id 1") {
    assert(forumMatch("boo.org:8080/1234abcd", re.gamePgnRe).has("1234abcd"))
  }

  test("forum links extract game id 2") {
    assert(
      forumMatch("https://boo.org:8080/abcd1234/black#12", re.gamePgnRe).has("abcd1234")
    )
  }

  test("forum links extract game id 3") {
    assert(forumMatch("boo.org:8080/abcd1234wxyz#44", re.gamePgnRe).has("abcd1234"))
  }

  test("blog links match basic") {
    assert(blogMatches("https://boo.org:8080/abcdefgh", re.gamePgnRe))
  }

  test("blog links match no scheme") {
    assert(blogMatches("boo.org:8080/12345678/white#123", re.gamePgnRe))
  }

  test("blog links match full id") {
    assert(blogMatches("boo.org:8080/1234abcd1234#22", re.gamePgnRe))
  }

  test("blog links fail wrong host") {
    assert(!blogMatches("boo.org:1234/abcdefgh", re.gamePgnRe))
  }

  test("blog links fail relative") {
    assert(!blogMatches("/12345678", re.gamePgnRe))
  }

  test("blog links fail mid-line") {
    assert(!blogMatches("oompa loompa https://boo.org:8080/abcd1234", re.gamePgnRe))
  }

  test("blog links fail site header") {
    assert(!blogMatches("[Site=\"https://boo.org:8080/1234abcd\"]", re.gamePgnRe))
  }

  test("blog links fail tournaments") {
    assert(!blogMatches("https://boo.org:8080/tournament/ABCD1234", re.gamePgnRe))
  }

  test("blog links fail usernames") {
    assert(!blogMatches("boo.org:8080/@/thibault", re.gamePgnRe))
  }

  test("blog links fail wrong length") {
    assert(!blogMatches("boo.org:8080/12345678bad/black", re.gamePgnRe))
  }

  test("blog links extract game id 1") {
    assert(blogMatch("boo.org:8080/zyxwvuts/black", re.gamePgnRe).has("zyxwvuts"))
  }

  test("blog links extract game id 2") {
    assert(blogMatch("boo.org:8080/1234abcd1234#123", re.gamePgnRe).has("1234abcd"))
  }

  test("blog links fail chapter regex no chapter") {
    assert(!blogMatches("boo.org:8080/study/abcd1234", re.chapterPgnRe))
  }

  test("blog links fail study regex on study w chapter") {
    assert(!blogMatches("boo.org:8080/study/abcd1234/abcd1234", re.studyPgnRe))
  }

  test("blog links extract studyid") {
    assert(blogMatch("https://boo.org:8080/study/abcd4890", re.studyPgnRe).has("abcd4890"))
  }

  test("blog links extract chapter") {
    assert(blogMatch("boo.org:8080/study/abcd1234/bcde2345", re.chapterPgnRe).has("bcde2345"))
  }
