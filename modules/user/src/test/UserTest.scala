package lila.user

import org.specs2.mutable.Specification

class UserTest extends Specification {
  "username regex" in {
    import User.couldBeUsername
    "bad prefix" in {
      couldBeUsername("000") must beFalse
      couldBeUsername("0foo") must beFalse
      couldBeUsername("_foo") must beFalse
      couldBeUsername("-foo") must beFalse
    }

    "bad suffix" in {
      couldBeUsername("a_") must beFalse
      couldBeUsername("a_") must beFalse
    }

    "OK things" in {
      couldBeUsername("g-foo") must beTrue
      couldBeUsername("G_FOo") must beTrue
      couldBeUsername("g-foo") must beTrue
    }
  }

}