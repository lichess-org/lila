package views.html.study

import controllers.routes
import chess.format.pgn.PgnStr

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

object embed:

  def apply(s: lila.study.Study, chapter: lila.study.Chapter, pgn: PgnStr)(using EmbedContext) =
    views.html.base.embed(
      title = s"${s.name} ${chapter.name}",
      cssModule = "lpv.embed"
    )(
      div(cls := "is2d")(div(pgn)),
      jsModule("lpv.embed"),
      views.html.analyse.embed.lpvJs(orientation = none)
    )

  def notFound(using EmbedContext) =
    views.html.base.embed(
      title = s"404 - ${trans.study.studyNotFound.txt()}",
      cssModule = "lpv.embed"
    ):
      div(cls := "not-found"):
        h1(trans.study.studyNotFound())
