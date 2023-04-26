package lila.api

class LpvGameRegexTest extends munit.FunSuite {

  val re = LpvGameRegex("boo.org:8080")

  def group(r: scala.util.matching.Regex, t: String, g: Int) = r.findFirstMatchIn(t).map(_.group(g))

  test("forum links match basic") {
    assert(re.linkRenderRe.matches("https://boo.org:8080/1234abcd"))
  }

  test("forum links match color") {
    assert(re.linkRenderRe.matches("https://boo.org:8080/abcd1234/white"))
  }

  test("forum links match relative") {
    assert(re.linkRenderRe.matches("/12345678/black#123"))
  }

  test("forum links match no scheme") {
    assert(re.linkRenderRe.matches("boo.org:8080/abcdefgh#123"))
  }

  test("forum links match full game id") {
    assert(re.linkRenderRe.matches("boo.org:8080/12345678abcd#12"))
  }

  test("forum links fail wrong host") {
    assert(!re.linkRenderRe.matches("boo.org:1234/zyxwvuts"))
  }

  test("forum links fail too long") {
    assert(!re.linkRenderRe.matches("/abcdefghijkl1234"))
  }

  test("forum links fail bad ply") {
    assert(!re.linkRenderRe.matches("/abcdefgh#12noes"))
  }

  test("forum links fail bad color") {
    assert(!re.linkRenderRe.matches("https://boo.org:8080/12345678/red#22"))
  }

  test("forum links fail tournaments") {
    assert(!re.linkRenderRe.matches("https://boo.org:8080/tournament/ABCD1234"))
  }

  test("forum links fail studies") {
    assert(!re.linkRenderRe.matches("https://boo.org:8080/study/abcd1234/bcde2345"))
  }

  test("forum links fail site header") {
    assert(!re.linkRenderRe.matches("[Site=\"https://boo.org:8080/abcd1234\"]"))
  }

  test("forum links fail mid-line") {
    assert(!re.linkRenderRe.matches("once upon a time /1234abcd/white#4"))
  }

  test("forum links fail usernames") {
    assert(!re.linkRenderRe.matches("/@/thibault"))
  }

  test("forum links fail full id AND color") {
    assert(!re.linkRenderRe.matches("/12345678abcd/black"))
  }

  test("forum links extract game id 1") {
    assert(group(re.linkRenderRe, "/1234abcd", 2).contains("1234abcd"))
  }

  test("forum links extract game id 2") {
    assert(
      group(re.linkRenderRe, "https://boo.org:8080/abcd1234/black#12", 2).contains("abcd1234")
    )
  }

  test("forum links extract game id 3") {
    assert(group(re.linkRenderRe, "boo.org:8080/abcd1234wxyz#44", 2).contains("abcd1234"))
  }

  test("blog links match basic") {
    assert(re.gamePgnsRe.matches("https://boo.org:8080/abcdefgh"))
  }

  test("blog links match no scheme") {
    assert(re.gamePgnsRe.matches("boo.org:8080/12345678/white#123"))
  }

  test("blog links match full id") {
    assert(re.gamePgnsRe.matches("boo.org:8080/1234abcd1234#22"))
  }

  test("blog links fail wrong host") {
    assert(!re.gamePgnsRe.matches("boo.org:1234/abcdefgh"))
  }

  test("blog links fail relative") {
    assert(!re.gamePgnsRe.matches("/12345678"))
  }

  test("blog links fail mid-line") {
    assert(!re.gamePgnsRe.matches("oompa loompa https://boo.org:8080/abcd1234"))
  }

  test("blog links fail site header") {
    assert(!re.gamePgnsRe.matches("[Site=\"https://boo.org:8080/1234abcd\"]"))
  }

  test("blog links fail tournaments") {
    assert(!re.gamePgnsRe.matches("https://boo.org:8080/tournament/ABCD1234"))
  }

  test("blog links fail usernames") {
    assert(!re.gamePgnsRe.matches("boo.org:8080/@/thibault"))
  }

  test("blog links fail wrong length") {
    assert(!re.gamePgnsRe.matches("boo.org:8080/12345678bad/black"))
  }

  test("blog links extract game id 1") {
    assert(group(re.gamePgnsRe, "blog links boo.org:8080/zyxwvuts/black", 1).contains("zyxwvuts"))
  }

  test("blog links extract game id 2") {
    assert(group(re.gamePgnsRe, "boo.org:8080/1234abcd1234#123", 1).contains("1234abcd"))
  }
}
