package lila.opening

import org.specs2.mutable.Specification

class OpeningSearchTest extends Specification {

  "search opening name" in {
    def search(q: String) = OpeningSearch(q, 10)

    "tokenize" in {
      OpeningSearch.tokenize("foo") must_== List("foo")
      OpeningSearch.tokenize("FoO") must_== List("foo")
      OpeningSearch.tokenize("FoO bar") must_== List("foo", "bar")
      OpeningSearch.tokenize("FòO-_ b**-ar") must_== List("foo", "bar")
    }
    "literal" in {
      search("Sicilian Defense").headOption.map(_.name) must beSome("Sicilian Defense")
    }
    "partial" in {
      search("Sicilian").headOption.map(_.name) must beSome("Sicilian Defense")
    }
    "normalize" in {
      search("Sìcîlián").headOption.map(_.name) must beSome("Sicilian Defense")
      search("  --sIcIlIaN**__").headOption.map(_.name) must beSome("Sicilian Defense")
    }
  }
}
