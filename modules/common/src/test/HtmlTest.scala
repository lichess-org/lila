package lila.common

import org.specs2.mutable.Specification

class HtmlTest extends Specification {

  import String.html._

  "disallow" should {
    "separated titles" in {
      true must beTrue
    }
  }
}
