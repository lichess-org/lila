package lila.bot

import play.api.i18n.Lang
import play.api.libs.json._

import lila.common.Json.jodaWrites
import lila.game.FairyConversion.Kyoto
import lila.game.JsonView._
import lila.game.{ Game, Pov }

final class BotJsonView(
    lightUserApi: lila.user.LightUserApi,
    rematches: lila.game.Rematches
) {

  def gameFull(game: Game)(implicit lang: Lang): JsObject =
    gameImmutable(game) ++ Json.obj(
      "type"  -> "gameFull",
      "state" -> gameState(game)
    )

  // Everything marked backwards will be removed soon
  def gameImmutable(game: Game)(implicit lang: Lang): JsObject = {
    Json
      .obj(
        "id"      -> game.id,
        "variant" -> game.variant,
        "clock"   -> game.clock.map(_.config),
        "speed"   -> game.speed.key,
        "perf" -> game.perfType.map { p =>
          Json.obj("name" -> p.trans)
        },
        "rated"       -> game.rated,
        "createdAt"   -> game.createdAt,
        "sente"       -> playerJson(game.sentePov),
        "white"       -> playerJson(game.sentePov),                 // backwards support
        "gote"        -> playerJson(game.gotePov),
        "black"       -> playerJson(game.gotePov),                  // backwards support
        "initialSfen" -> game.initialSfen.fold("startpos")(_.value),
        "initialFen"  -> game.initialSfen.fold("startpos")(_.value) // backwards support
      )
      .add(
        "fairyInitialSfen" -> (game.variant.kyotoshogi option game.initialSfen.fold("startpos")(sfen =>
          Kyoto.makeFairySfen(sfen).value
        ))
      )
      .add("tournamentId" -> game.tournamentId)
  }

  def gameState(game: Game): JsObject = {
    Json
      .obj(
        "type"   -> "gameState",
        "moves"  -> game.usis.map(_.usi).mkString(" "),
        "btime"  -> millisOf(game.sentePov),
        "wtime"  -> millisOf(game.gotePov),
        "binc"   -> game.clock.??(_.config.increment.millis),
        "winc"   -> game.clock.??(_.config.increment.millis),
        "byo"    -> game.clock.??(_.config.byoyomi.millis),
        "sdraw"  -> game.sentePlayer.isOfferingDraw,
        "gdraw"  -> game.gotePlayer.isOfferingDraw,
        "status" -> game.status.name
      )
      .add(
        "fairyMoves" -> (game.variant.kyotoshogi option Kyoto
          .makeFairyUsiList(game.usis, game.initialSfen)
          .mkString(" "))
      )
      .add("winner" -> game.winnerColor)
      .add("rematch" -> rematches.of(game.id))
  }

  def chatLine(username: String, text: String, player: Boolean) =
    Json.obj(
      "type"     -> "chatLine",
      "room"     -> (if (player) "player" else "spectator"),
      "username" -> username,
      "text"     -> text
    )

  private def playerJson(pov: Pov) = {
    val light = pov.player.userId flatMap lightUserApi.sync
    Json
      .obj()
      .add("aiLevel" -> pov.player.aiLevel)
      .add("id" -> light.map(_.id))
      .add("name" -> light.map(_.name))
      .add("title" -> light.map(_.title))
      .add("rating" -> pov.player.rating)
      .add("provisional" -> pov.player.provisional)
  }

  private def millisOf(pov: Pov): Int =
    pov.game.clock
      .map(_.currentClockFor(pov.color).time.millis.toInt)
      .orElse(pov.game.correspondenceClock.map(_.remainingTime(pov.color).toInt * 1000))
      .getOrElse(Int.MaxValue)

  implicit private val clockConfigWriter: OWrites[shogi.Clock.Config] = OWrites { c =>
    Json.obj(
      "initial"   -> c.limit.millis,
      "increment" -> c.increment.millis,
      "byoyomi"   -> c.byoyomi.millis,
      "periods"   -> c.periodsTotal
    )
  }
}
