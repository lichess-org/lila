package lila.ublog

import org.specs2.execute.Result
import org.specs2.mutable.Specification
import scalatags.Text.all._

import lila.common.{ config, Markdown }

class UblogMarkupTest extends Specification {

  val m = UblogMarkup

  "backslashUnderscore" >> {
    "fix href" >> {
      m.unescapeUnderscoreInLinks(
        """<p><a rel="nofollow noopener noreferrer" href="https://youtu.be/di6W\_i3NiJA">text</a></p>"""
      ) ===
        """<p><a rel="nofollow noopener noreferrer" href="https://youtu.be/di6W_i3NiJA">text</a></p>"""
    }
    "fix link text" >> {
      m.unescapeUnderscoreInLinks(
        """<p><a rel="nofollow noopener noreferrer" href="#">https://youtu.be/di6W\_i3NiJA</a></p>"""
      ) ===
        """<p><a rel="nofollow noopener noreferrer" href="#">https://youtu.be/di6W_i3NiJA</a></p>"""
    }
    "fix href and link text" >> {
      m.unescapeUnderscoreInLinks(
        """<p><a rel="nofollow noopener noreferrer" href="https://youtu.be/di6W\_i3NiJA">https://youtu.be/di6W\_i3NiJA</a></p>"""
      ) ===
        """<p><a rel="nofollow noopener noreferrer" href="https://youtu.be/di6W_i3NiJA">https://youtu.be/di6W_i3NiJA</a></p>"""
    }
  }
  "fix AtUsername" >> {
    m.unescapeAtUsername(
      Markdown("""@test\_1234""")
    ) ===
      Markdown("""@test_1234""")
  }

  "not break fine AtUsername" >> {
    m.unescapeAtUsername(
      Markdown("""@foo_bar""")
    ) ===
      Markdown("""@foo_bar""")
  }

  "fix mention in quote (which adds backslash escaping)" >> {
    m.unescapeAtUsername(Markdown("""> \(By @neio\)""")) === Markdown("""> \(By @neio\)""")
    m.unescapeAtUsername(Markdown("""> \(By @neio_1\)""")) === Markdown("""> \(By @neio_1\)""")
    m.unescapeAtUsername(Markdown("""> \(By @neio\_1\)""")) === Markdown("""> \(By @neio_1\)""")
  }
}
