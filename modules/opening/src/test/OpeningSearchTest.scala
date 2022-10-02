package lila.opening

import org.specs2.mutable.Specification

class OpeningSearchTest extends Specification {

  "search opening name" in {
    import OpeningSearch.{ apply => search }

    "literal" in {
      search("Sicilian Defense").headOption.map(_.name) must beSome("Sicilian Defense")
    }
    "partial" in {
      search("Sicilian").headOption.map(_.name) must beSome("Sicilian Defense")
    }
  }
}
