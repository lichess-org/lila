package lila.web
package ui

import lila.ui.*
import lila.ui.ScalatagsTemplate.*
import lila.core.i18n.Translate

object bits:

  object splitNumber extends NumberHelper:
    private val NumberFirstRegex = """(\d++)\s(.+)""".r
    private val NumberLastRegex = """\s(\d++)$""".r.unanchored

    def apply(s: Frag)(using ctx: Context)(using Translate): Frag =
      if ctx.blind then s
      else
        val rendered = s.render
        rendered match
          case NumberFirstRegex(number, html) =>
            frag(
              strong((~number.toIntOption).localize),
              br,
              raw(html)
            )
          case NumberLastRegex(n) if rendered.length > n.length + 1 =>
            frag(
              raw(rendered.dropRight(n.length + 1)),
              br,
              strong((~n.toIntOption).localize)
            )
          case h => raw(h.replaceIf('\n', "<br>"))

  lazy val stage = a(
    href := "https://lichess.org",
    style := """
background: #7f1010;
color: #fff;
position: fixed;
bottom: 0;
left: 0;
padding: .5em 1em;
border-top-right-radius: 3px;
z-index: 99;
"""
  ):
    "This is an empty Lichess preview website, go to lichess.org instead"

  val connectLinks: Frag = div(cls := "connect-links")(
    a(
      href := "https://mastodon.online/@lichess",
      targetBlank,
      noFollow,
      relMe
    )("Mastodon"),
    a(
      href := "https://github.com/lichess-org",
      targetBlank,
      noFollow
    )("GitHub"),
    a(href := "https://discord.gg/lichess", targetBlank, noFollow)("Discord"),
    a(href := "https://bsky.app/profile/lichess.org", targetBlank, noFollow)("Bluesky"),
    a(
      href := "https://youtube.com/@LichessDotOrg",
      targetBlank,
      noFollow
    )("YouTube"),
    a(
      href := "https://www.twitch.tv/lichessdotorg",
      targetBlank,
      noFollow
    )("Twitch")
  )

  val logo = raw:
    """<svg class="lichess-logo-svg" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 50 50"><path stroke-linejoin="round" d="M38.956.5c-3.53.418-6.452.902-9.286 2.984C5.534 1.786-.692 18.533.68 29.364 3.493 50.214 31.918 55.785 41.329 41.7c-7.444 7.696-19.276 8.752-28.323 3.084S-.506 27.392 4.683 17.567C9.873 7.742 18.996 4.535 29.03 6.405c2.43-1.418 5.225-3.22 7.655-3.187l-1.694 4.86 12.752 21.37c-.439 5.654-5.459 6.112-5.459 6.112-.574-1.47-1.634-2.942-4.842-6.036-3.207-3.094-17.465-10.177-15.788-16.207-2.001 6.967 10.311 14.152 14.04 17.663 3.73 3.51 5.426 6.04 5.795 6.756 0 0 9.392-2.504 7.838-8.927L37.4 7.171z"/></svg>"""
