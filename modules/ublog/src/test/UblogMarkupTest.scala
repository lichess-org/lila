package lila.ublog

import org.specs2.execute.Result
import org.specs2.mutable.Specification
import scalatags.Text.all._

import lila.common.{ config, Markdown }

class UblogMarkupTest extends Specification {

  val m = UblogMarkup

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
      Markdown("""@test\_1234""")
    ) must_==
      Markdown("""@test_1234""")
  }

  "not break fine AtUsername" in {
    m.unescapeAtUsername(
      Markdown("""@foo_bar""")
    ) must_==
      Markdown("""@foo_bar""")
  }

  "fix mention in quote (which adds backslash escaping)" in {
    m.unescapeAtUsername(Markdown("""> \(By @neio\)""")) must_== Markdown("""> \(By @neio\)""")
    m.unescapeAtUsername(Markdown("""> \(By @neio_1\)""")) must_== Markdown("""> \(By @neio_1\)""")
    m.unescapeAtUsername(Markdown("""> \(By @neio\_1\)""")) must_== Markdown("""> \(By @neio_1\)""")
  }
}
