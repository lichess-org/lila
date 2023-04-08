package lila.common

import org.specs2.mutable.*

class EmailTest extends Specification {

  "normalize" >> {
    "handle gmail" >> {
      EmailAddress("Hello.World+suffix1+suffix2@gmail.com").normalize === NormalizedEmailAddress(
        "helloworld@gmail.com"
      )
      EmailAddress("foo.bar@googlemail.com").normalize === NormalizedEmailAddress("foobar@googlemail.com")
    }
    "lowercase emails" >> {
      EmailAddress("aBcDeFG@HOTMAIL.cOM").normalize === NormalizedEmailAddress("abcdefg@hotmail.com")
    }
  }

  "from" >> {
    "accept valid addresses" >> {
      EmailAddress.from("Hello.World+suffix1+suffix2@gmail.com") must beSome
      EmailAddress.from("kebab-case@example.com") must beSome
      EmailAddress.from("snake_case@example.com") must beSome
      EmailAddress.from("_leading.underscore@example.com") must beSome
      EmailAddress.from("trailing.dash-@example.com") must beSome
    }
    "reject invalid addresses" >> {
      EmailAddress.from(".leading.dot@example.com") must beNone
      EmailAddress.from("trailing.dot.@example.com") must beNone
      EmailAddress.from("underscore.in@domain_name.com") must beNone
      EmailAddress.from("consecutive..dots@example.com") must beNone
      EmailAddress.from("invalid<character@example.com") must beNone
    }
  }

}
