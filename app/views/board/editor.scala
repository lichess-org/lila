package views.html.board

import play.api.libs.json.{ Json, JsArray }
import controllers.routes

import chess.format.Fen

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.*
import lila.common.String.html.safeJsonValue
import lila.common.Json.given

object editor:

  def apply(
      fen: Option[Fen.Epd],
      positionsJson: JsArray,
      endgamePositionsJson: JsArray
  )(using PageContext) =
    views.html.base.layout(
      title = trans.boardEditor.txt(),
      moreJs = jsModuleInit(
        "editor",
        jsData(fen) ++ Json.obj("positions" -> positionsJson, "endgamePositions" -> endgamePositionsJson)
      ),
      moreCss = cssTag("editor"),
      zoomable = true,
      openGraph = lila.app.ui
        .OpenGraph(
          title = "Chess board editor",
          url = s"$netBaseUrl${routes.Editor.index.url}",
          description = "Load opening positions or create your own chess.Position on a chess board editor"
        )
        .some
    )(
      main(id := "board-editor")(
        div(cls := "board-editor")(
          div(cls := "spare"),
          div(cls := "main-board")(chessgroundBoard),
          div(cls := "spare")
        )
      )
    )

  def jsData(fen: Option[Fen.Epd] = None)(using ctx: Context) =
    Json
      .obj(
        "baseUrl"   -> s"$netBaseUrl${routes.Editor.index}",
        "animation" -> Json.obj("duration" -> ctx.pref.animationMillis),
        "is3d"      -> ctx.pref.is3d,
        "i18n"      -> i18nJsObject(i18nKeys)
      )
      .add("fen" -> fen)

  private val i18nKeys = List(
    trans.setTheBoard,
    trans.boardEditor,
    trans.startPosition,
    trans.clearBoard,
    trans.flipBoard,
    trans.loadPosition,
    trans.popularOpenings,
    trans.endgamePositions,
    trans.castling,
    trans.whiteCastlingKingside,
    trans.blackCastlingKingside,
    trans.whitePlays,
    trans.blackPlays,
    trans.variant,
    trans.continueFromHere,
    trans.playWithTheMachine,
    trans.playWithAFriend,
    trans.analysis,
    trans.toStudy
  )
