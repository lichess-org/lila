package lila.common

class MarkdownToastUiTest extends munit.FunSuite:

  val m = MarkdownToastUi

  test("backslashUnderscore fix href") {
    assertEquals(
      m.unescapeUnderscoreInLinks(
        Html("""<p><a rel="nofollow noopener noreferrer" href="https://youtu.be/di6W\_i3NiJA">text</a></p>""")
      ),
      Html("""<p><a rel="nofollow noopener noreferrer" href="https://youtu.be/di6W_i3NiJA">text</a></p>""")
    )
  }
  test("backslashUnderscore fix link text") {
    assertEquals(
      m.unescapeUnderscoreInLinks(
        Html("""<p><a rel="nofollow noopener noreferrer" href="#">https://youtu.be/di6W\_i3NiJA</a></p>""")
      ),
      Html("""<p><a rel="nofollow noopener noreferrer" href="#">https://youtu.be/di6W_i3NiJA</a></p>""")
    )
  }
  test("backslashUnderscore fix href and link text") {
    assertEquals(
      m.unescapeUnderscoreInLinks(
        Html(
          """<p><a rel="nofollow noopener noreferrer" href="https://youtu.be/di6W\_i3NiJA">https://youtu.be/di6W\_i3NiJA</a></p>"""
        )
      ),
      Html(
        """<p><a rel="nofollow noopener noreferrer" href="https://youtu.be/di6W_i3NiJA">https://youtu.be/di6W_i3NiJA</a></p>"""
      )
    )
  }
  test("fix AtUsername") {
    assertEquals(m.unescapeAtUsername(Markdown("""@test\_1234""")), Markdown("""@test_1234"""))
  }

  test("not break fine AtUsername") {
    assertEquals(m.unescapeAtUsername(Markdown("""@foo_bar""")), Markdown("""@foo_bar"""))
  }

  test("fix mention in quote (which adds backslash escaping)") {
    assertEquals(m.unescapeAtUsername(Markdown("""> \(By @neio\)""")), Markdown("""> \(By @neio\)"""))
    assertEquals(m.unescapeAtUsername(Markdown("""> \(By @neio_1\)""")), Markdown("""> \(By @neio_1\)"""))
    assertEquals(m.unescapeAtUsername(Markdown("""> \(By @neio\_1\)""")), Markdown("""> \(By @neio_1\)"""))
  }
