package controllers

import chess.format.Fen
import chess.variant.Variant
import play.api.libs.json.*
import views.*

import lila.app.{ given, * }
import lila.common.Json.given

final class Editor(env: Env) extends LilaController(env):

  private lazy val positionsJson =
    JsArray(chess.StartingPosition.all map { p =>
      Json.obj(
        "eco"  -> p.eco,
        "name" -> p.name,
        "fen"  -> p.fen
      )
    })

  private lazy val endgamePositionsJson =
    JsArray(chess.EndgamePosition.positions map { p =>
      Json.obj(
        "name" -> p.name,
        "fen"  -> p.fen
      )
    })

  def index = load("")

  def load(urlFen: String) = Open:
    val fen: Option[Fen.Epd] = lila.common.String
      .decodeUriPath(urlFen)
      .filter(_.nonEmpty)
      .map(Fen.Epd.clean)
    Ok.page:
      html.board.editor(fen, positionsJson, endgamePositionsJson)

  def data = Open:
    JsonOk(html.board.editor.jsData())

  def game(id: GameId) = Open:
    Found(env.game.gameRepo game id): game =>
      Redirect:
        if game.playable
        then routes.Round.watcher(game.id, "white").url
        else editorUrl(get("fen").fold(Fen.write(game.chess))(Fen.Epd.clean), game.variant)

  private[controllers] def editorUrl(fen: Fen.Epd, variant: Variant): String =
    if fen == Fen.initial && variant.standard then routes.Editor.index.url
    else
      val params = variant.exotic so s"?variant=${variant.key}"
      routes.Editor.load(lila.common.String.underscoreFen(fen)).url + params
