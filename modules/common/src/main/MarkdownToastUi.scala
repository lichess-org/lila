package lila.common

import java.util.regex.Matcher

object MarkdownToastUi:

  object latex:
    // $$something$$ breaks the TUI editor WYSIWYG
    private val regex = """\${2,}+([^\$]++)\${2,}+""".r
    def removeFrom(markdown: Markdown) = markdown.map(regex.replaceAllIn(_, """\$\$ $1 \$\$"""))

  // put images into a container for styling
  def imageParagraph(markup: Html) =
    markup.map(_.replace("""<p><img src=""", """<p class="img-container"><img src="""))

  private def unescape(txt: String) = txt.replace("""\_""", "_")

  // https://github.com/lichess-org/lila/issues/9767
  // toastui editor escapes `_` as `\_` and it breaks autolinks
  object unescapeUnderscoreInLinks:
    private val hrefRegex = """href="([^"]++)"""".r
    private val contentRegex = """>([^<]++)</a>""".r
    def apply(markup: Html) = Html:
      contentRegex.replaceAllIn(
        hrefRegex
          .replaceAllIn(markup.value, m => s"""href="${Matcher.quoteReplacement(unescape(m.group(1)))}""""),
        m => s""">${Matcher.quoteReplacement(unescape(m.group(1)))}</a>"""
      )

  // toastui editor escapes `_` as `\_` and it breaks @username
  object unescapeAtUsername:
    // Same as `atUsernameRegex` in `RawHtmlTest.scala` but it also matches the '\' character.
    // Can't end with '\', which would be escaping something after the username, like '\)'
    private val atUsernameRegexEscaped = """@(?<![\w@#/]@)([\w\\-]{1,29}\w)(?![@\w-]|\.\w)""".r
    def apply(m: Markdown) = m.map(atUsernameRegexEscaped.replaceAllIn(_, a => s"@${unescape(a.group(1))}"))
