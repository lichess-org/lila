package views.html.study

import chess.format.pgn.PgnStr
import controllers.routes
import play.api.libs.json.Json

import lila.app.templating.Environment.{ *, given }
import lila.web.ui.ScalatagsTemplate.{ *, given }

object embed:

  def apply(s: lila.study.Study, chapter: lila.study.Chapter, pgn: PgnStr)(using ctx: EmbedContext) =
    import views.html.analyse.embed.*
    val canGetPgn = s.settings.shareable == lila.study.Settings.UserSelection.Everyone
    views.html.base.embed(
      title = s"${s.name} ${chapter.name}",
      cssModule = "lpv.embed"
    )(
      div(cls := "is2d")(div(pgn)),
      jsTag("site.lpv.embed"),
      lpvJs:
        lpvConfig(orientation = none, getPgn = canGetPgn) ++ Json
          .obj()
          .add(
            "gamebook" -> chapter.isGamebook
              .option(Json.obj("url" -> routes.Study.chapter(s.id, chapter.id).url))
          )
    )

  def notFound(using EmbedContext) =
    views.html.base.embed(
      title = s"404 - ${trans.study.studyNotFound.txt()}",
      cssModule = "lpv.embed"
    ):
      div(cls := "not-found"):
        h1(trans.study.studyNotFound())
