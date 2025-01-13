package lila.shutup

import scalatags.Text.all.*

/* Some of the texts in this file are horrible.
 * We reject and condemn them. They are here so we can verify
 * that Lichess detects them, to help moderators keeping the site nice.
 */
class HighlightTest extends munit.FunSuite:

  import Analyser.highlightBad as hi

  given munit.Compare[Frag, String] with
    def isEqual(obtained: Frag, expected: String): Boolean = obtained.render == expected

  test("neat"):
    assertEquals(hi("Why hello there!"), "Why hello there!")

  test("russian"):
    assertEquals(hi("сука пизда"), "<bad>сука</bad> <bad>пизда</bad>")

  test("single word"):
    assertEquals(hi("nigger"), "<bad>nigger</bad>")

  test("with punctuation"):
    assertEquals(hi("nigger?"), "<bad>nigger</bad>?")
