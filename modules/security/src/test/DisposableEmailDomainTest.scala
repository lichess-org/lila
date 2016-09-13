package lila.security

import org.specs2.mutable.Specification
import play.api.libs.json._

class DisposableEmailDomainTest extends Specification {

  val d = new DisposableEmailDomain("", None)
  d.setDomains(d.textToDomains(Fixtures.text))

  "disposable email domain" should {
    "simple" in {
      d("gmail.com") must beFalse
      d("hotmail.com") must beFalse
      d("live.com") must beFalse
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
      d("10minutemail.com") must beTrue
      d("12hourmail.de") must beTrue
      d("30minutesmail.co") must beTrue
      d("centermail.net") must beTrue
      d("center-mail.info") must beTrue
      d("discard.com") must beTrue
      d("discardmail.com") must beTrue
      d("discartmail.com") must beTrue
      d("guerillamail.com") must beTrue
      d("guerrillamail.com") must beTrue
      d("jetable.fr.nf") must beTrue
      d("notjetable.fr") must beFalse
    }
  }
}
