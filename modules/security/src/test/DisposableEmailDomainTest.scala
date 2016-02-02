package lila.security

import org.specs2.mutable.Specification
import play.api.libs.json._

class DisposableEmailDomainTest extends Specification {

  val d = new DisposableEmailDomain("")
  d.setDomains(Json.parse(Fixtures.json))

  "disposable email domain" should {
    "simple" in {
      d("gmail.com") must beFalse
      d("docmail.cz") must beTrue
    }
    "suffix" in {
      d("foo.some.randomgoodemail.org") must beFalse
      d("phpbb.uu.gl") must beTrue
      d("other.phpbb.uu.gl") must beTrue
    }
    "regex" in {
      d("domforfb.tk") must beFalse
      d("domforfbaa.tk") must beFalse
      d("domforfb1.tk") must beTrue
      d("domforfb29.tk") must beTrue
      d("foo.domforfb11.tk") must beTrue
      d("foo2.domforfb11.tk") must beTrue
    }
  }
}
