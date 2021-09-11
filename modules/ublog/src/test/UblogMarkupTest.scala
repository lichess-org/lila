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
}
