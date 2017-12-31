package lila.common

import org.specs2.mutable.Specification

class StringTest extends Specification {

  "slugify" should {
    "be safe in html" in {
      String.slugify("hello \" world") must not contain ("\"")
      String.slugify("<<<") must not contain ("<")
    }
  }
}
