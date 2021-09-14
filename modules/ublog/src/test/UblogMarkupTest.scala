package lila.ublog

import org.specs2.execute.Result
import org.specs2.mutable.Specification
import scalatags.Text.all._

import lila.common.config

class UblogMarkupTest extends Specification {

  val m = new UblogMarkup(config.BaseUrl("https://lichess.org"), config.AssetBaseUrl("https://lichess1.org"))

  "backslashUnderscore" should {
    "fix href" in {
      m.unescapeUnderscoreInLinks(
        """<p><a rel="nofollow noopener noreferrer" href="https://youtu.be/di6W\_i3NiJA">text</a></p>"""
      ) must_==
        """<p><a rel="nofollow noopener noreferrer" href="https://youtu.be/di6W_i3NiJA">text</a></p>"""
    }
    "fix link text" in {
      m.unescapeUnderscoreInLinks(
        """<p><a rel="nofollow noopener noreferrer" href="#">https://youtu.be/di6W\_i3NiJA</a></p>"""
      ) must_==
        """<p><a rel="nofollow noopener noreferrer" href="#">https://youtu.be/di6W_i3NiJA</a></p>"""
    }
    "fix href and link text" in {
      m.unescapeUnderscoreInLinks(
        """<p><a rel="nofollow noopener noreferrer" href="https://youtu.be/di6W\_i3NiJA">https://youtu.be/di6W\_i3NiJA</a></p>"""
      ) must_==
        """<p><a rel="nofollow noopener noreferrer" href="https://youtu.be/di6W_i3NiJA">https://youtu.be/di6W_i3NiJA</a></p>"""
    }
  }
  "fix AtUsername" in {
    m.unescapeAtUsername(
      """@test\_1234"""
    ) must_==
      """@test_1234"""
  }

  "not break fine AtUsername" in {
    m.unescapeAtUsername(
      """@foo_bar"""
    ) must_==
      """@foo_bar"""
  }

  "fix mention in quote (which adds backslash escaping)" in {
    m.unescapeAtUsername("""> \(By @neio\)""") must_== """> \(By @neio\)"""
    m.unescapeAtUsername("""> \(By @neio_1\)""") must_== """> \(By @neio_1\)"""
    m.unescapeAtUsername("""> \(By @neio\_1\)""") must_== """> \(By @neio_1\)"""
  }
}
