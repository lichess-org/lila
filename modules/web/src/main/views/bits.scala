package lila.web
package views

import chess.format.Fen
import play.api.i18n.Lang

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import scalalib.paginator.Paginator
import lila.core.i18n.Translate
import lila.ui.ChessHelper.underscoreFen

final class bits():

  lazy val stage = a(
    href  := "https://lichess.org",
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
        href := routes.Main.externalLink("mastodon", "https://mastodon.online/@lichess"),
        targetBlank,
        noFollow,
        relMe
      )("Mastodon"),
      a(href := routes.Main.externalLink("twitter", "https://twitter.com/lichess"), targetBlank, noFollow)(
        "Twitter"
      ),
      a(href := routes.Main.externalLink("discord", "https://discord.gg/lichess"), targetBlank, noFollow)(
        "Discord"
      ),
      a(
        href := routes.Main.externalLink("youtube", "https://youtube.com/c/LichessDotOrg"),
        targetBlank,
        noFollow
      )("YouTube"),
      a(
        href := routes.Main.externalLink("twitch", "https://www.twitch.tv/lichessdotorg"),
        targetBlank,
        noFollow
      )("Twitch"),
      a(
        href := routes.Main.externalLink("instagram", "https://instagram.com/lichessdotorg"),
        targetBlank,
        noFollow
      )("Instagram")
    )

  def api = raw:
    """<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Security-Policy" content="default-src 'self'; style-src 'unsafe-inline'; script-src https://cdn.jsdelivr.net blob:; child-src blob:; connect-src https://raw.githubusercontent.com; img-src data: https://lichess.org https://lichess1.org;">
    <title>Lichess.org API reference</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <style>body { margin: 0; padding: 0; }</style>
  </head>
  <body>
    <redoc spec-url="https://raw.githubusercontent.com/lichess-org/api/master/doc/specs/lichess-api.yaml"></redoc>
    <script src="https://cdn.jsdelivr.net/npm/redoc@next/bundles/redoc.standalone.js"></script>
  </body>
</html>"""
