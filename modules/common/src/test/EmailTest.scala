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

  "from" should {
    "accept valid addresses" in {
      EmailAddress.from("Hello.World+suffix1+suffix2@gmail.com") must beSome
      EmailAddress.from("kebab-case@example.com") must beSome
      EmailAddress.from("snake_case@example.com") must beSome
      EmailAddress.from("_leading.underscore@example.com") must beSome
      EmailAddress.from("trailing.dash-@example.com") must beSome
    }
    "reject invalid addresses" in {
      EmailAddress.from(".leading.dot@example.com") must beNone
      EmailAddress.from("trailing.dot.@example.com") must beNone
      EmailAddress.from("underscore.in@domain_name.com") must beNone
      EmailAddress.from("consecutive..dots@example.com") must beNone
      EmailAddress.from("invalid<character@example.com") must beNone
    }
  }

}
