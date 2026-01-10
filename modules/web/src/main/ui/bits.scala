package lila.web
package ui

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
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

  val connectLinks: Frag =
    div(cls := "connect-links")(
      a(
        href := routes.Main.externalLink("mastodon"),
        targetBlank,
        noFollow,
        relMe
      )("Mastodon"),
      a(
        href := routes.Main.externalLink("github"),
        targetBlank,
        noFollow
      )("GitHub"),
      a(href := routes.Main.externalLink("discord"), targetBlank, noFollow)("Discord"),
      a(href := routes.Main.externalLink("bluesky"), targetBlank, noFollow)("Bluesky"),
      a(
        href := routes.Main.externalLink("youtube"),
        targetBlank,
        noFollow
      )("YouTube"),
      a(
        href := routes.Main.externalLink("twitch"),
        targetBlank,
        noFollow
      )("Twitch")
    )
