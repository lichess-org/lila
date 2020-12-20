package lila.common

import org.specs2.mutable.Specification

class EmailTest extends Specification {

  "normalize" should {
    "handle gmail" in {
      EmailAddress("Hello.World+suffix1+suffix2@gmail.com").normalize must_== NormalizedEmailAddress(
        "helloworld@gmail.com"
      )
      EmailAddress("foo.bar@googlemail.com").normalize must_== NormalizedEmailAddress("foobar@googlemail.com")
    }
    "lowercase emails" in {
      EmailAddress("aBcDeFG@HOTMAIL.cOM").normalize must_== NormalizedEmailAddress("abcdefg@hotmail.com")
    }
  }

}
