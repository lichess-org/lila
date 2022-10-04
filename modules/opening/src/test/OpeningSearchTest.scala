package lila.opening

import org.specs2.mutable.Specification

class OpeningSearchTest extends Specification {

  "search opening name" in {
    def search(q: String) = OpeningSearch(q, 10)

    "tokenize" in {
      OpeningSearch.tokenize("foo") must_== Set("foo")
      OpeningSearch.tokenize("FoO") must_== Set("foo")
      OpeningSearch.tokenize("FoO bar") must_== Set("foo", "bar")
      OpeningSearch.tokenize("FòO-_ bar**-king") must_== Set("foo", "bar", "king")
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
    "progressive" in {
      search("Sicil").headOption.map(_.name) must beSome("Sicilian Defense")
    }
  }
}
