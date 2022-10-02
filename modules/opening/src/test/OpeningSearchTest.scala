package lila.opening

import org.specs2.mutable.Specification

class OpeningSearchTest extends Specification {

  "search opening name" in {
    def search(q: String) = OpeningSearch(q, 10)

    "literal" in {
      search("Sicilian Defense").headOption.map(_.name) must beSome("Sicilian Defense")
    }
    "partial" in {
      search("Sicilian").headOption.map(_.name) must beSome("Sicilian Defense")
    }
  }
}
