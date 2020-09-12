package lila.user

import org.specs2.mutable.Specification

class UserTest extends Specification {

  def canSignup(str: User.ID) =
    User.newUsernamePrefix.pattern.matcher(str).matches && User.newUsernameSuffix.pattern
      .matcher(str)
      .matches &&
      User.newUsernameChars.pattern.matcher(str).matches &&
      User.newUsernameLetters.pattern.matcher(str).matches

  "username regex" in {
    import User.couldBeUsername
    "bad prefix: can login" in {
      couldBeUsername("000") must beTrue
      couldBeUsername("0foo") must beTrue
      couldBeUsername("_foo") must beFalse
      couldBeUsername("-foo") must beFalse
    }

    "bad prefix: cannot signup" in {
      canSignup("000") must beFalse
      canSignup("0foo") must beFalse
      canSignup("_foo") must beFalse
      canSignup("-foo") must beFalse
    }

    "bad suffix" in {
      couldBeUsername("a_") must beFalse
      couldBeUsername("a_") must beFalse
    }

    "too many consecutive non-letter chars" in {
      canSignup("a_-a") must beFalse
      canSignup("_-a") must beFalse
      canSignup("a__a") must beFalse
      canSignup("a_-a") must beFalse
      canSignup("a--a") must beFalse
      canSignup("a--_") must beFalse
      couldBeUsername("a--a") must beTrue
      canSignup("a-a") must beTrue
      canSignup("a_a") must beTrue
      canSignup("a333") must beTrue
      canSignup("ksean222") must beTrue
      canSignup("Ksean222") must beTrue
    }

    "OK things" in {
      couldBeUsername("g-foo") must beTrue
      couldBeUsername("G_FOo") must beTrue
      couldBeUsername("g-foo") must beTrue
    }
  }

}
