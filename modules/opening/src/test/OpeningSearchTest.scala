package lila.opening

import org.specs2.mutable.Specification

class OpeningSearchTest extends Specification {

  "search opening name" >> {
    def search(q: String) = OpeningSearch(q, 10)

    "tokenize" >> {
      OpeningSearch.tokenize("foo") === Set("foo")
      OpeningSearch.tokenize("FoO") === Set("foo")
      OpeningSearch.tokenize("FoO bar") === Set("foo", "bar")
      OpeningSearch.tokenize("FòO-_ bar**-king") === Set("foo", "bar", "king")
    }
    "literal" >> {
      search("Sicilian Defense").headOption.map(_.name) must beSome("Sicilian Defense")
    }
    "partial" >> {
      search("Sicilian").headOption.map(_.name) must beSome("Sicilian Defense")
    }
    "normalize" >> {
      search("Sìcîlián").headOption.map(_.name) must beSome("Sicilian Defense")
      search("  --sIcIlIaN**__").headOption.map(_.name) must beSome("Sicilian Defense")
    }
    "progressive" >> {
      search("Sicil").headOption.map(_.name) must beSome("Sicilian Defense")
    }
  }
}
