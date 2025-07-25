package lila.game

import chess.format.Fen
import chess.{ Clock, Color }
import play.api.libs.json.*

import lila.common.Json.{ *, given }
import lila.core.LightUser
import lila.core.game.{ Blurs, Game, Player, Pov, Source }

final class JsonView(rematches: Rematches):

  import JsonView.given

  def base(game: Game, initialFen: Option[Fen.Full]) =
    Json
      .obj(
        "id" -> game.id,
        "variant" -> game.variant,
        "speed" -> game.speed.key,
        "perf" -> game.perfKey,
        "rated" -> game.rated,
        "fen" -> Fen.write(game.chess),
        "turns" -> game.ply,
        "source" -> game.source,
        "status" -> game.status,
        "createdAt" -> game.createdAt
      )
      .add("startedAtTurn" -> game.chess.startedAtPly.some.filter(_ > 0))
      .add("initialFen" -> initialFen)
      .add("threefold" -> game.history.threefoldRepetition)
      .add("boosted" -> game.boosted)
      .add("tournamentId" -> game.tournamentId)
      .add("swissId" -> game.swissId)
      .add("winner" -> game.winnerColor)
      .add("rematch" -> rematches.getAcceptedId(game.id))
      .add("rules" -> game.metadata.nonEmptyRules)
      .add("drawOffers" -> (!game.drawOffers.isEmpty).option(game.drawOffers.normalizedPlies))

  // adds fields that could be computed by the client instead
  def baseWithChessDenorm(game: Game, initialFen: Option[Fen.Full]) =
    base(game, initialFen) ++ Json
      .obj(
        "player" -> game.turnColor,
        "fen" -> Fen.write(game.chess)
      )
      .add("check" -> game.position.checkSquare.map(_.key))
      .add("lastMove" -> game.lastMoveKeys)

  def apiAiNewGame(pov: Pov, initialFen: Option[Fen.Full]): JsObject =
    baseWithChessDenorm(pov.game, initialFen) ++ Json.obj("fullId" -> pov.fullId)

  def ownerPreview(pov: Pov)(using LightUser.GetterSync) =
    Json
      .obj(
        "fullId" -> pov.fullId,
        "gameId" -> pov.gameId,
        "fen" -> maybeFen(pov),
        "color" -> pov.color,
        "lastMove" -> (pov.game.lastMoveKeys | ""),
        "source" -> pov.game.source,
        "status" -> pov.game.status,
        "variant" -> Json.obj(
          "key" -> pov.game.variant.key,
          "name" -> pov.game.variant.name
        ),
        "speed" -> pov.game.speed.key,
        "perf" -> pov.game.perfKey,
        "rated" -> pov.game.rated,
        "hasMoved" -> pov.hasMoved,
        "opponent" -> Json
          .obj(
            "id" -> pov.opponent.userId,
            "username" -> lila.game.Namer
              .playerTextBlocking(pov.opponent, withRating = false)
          )
          .add("rating" -> pov.opponent.rating)
          .add("ratingDiff" -> pov.opponent.ratingDiff)
          .add("ai" -> pov.opponent.aiLevel),
        "isMyTurn" -> pov.isMyTurn
      )
      .add("secondsLeft" -> pov.remainingSeconds)
      .add("tournamentId" -> pov.game.tournamentId)
      .add("swissId" -> pov.game.swissId)
      // .add("orientation" -> pov.game.variant.racingKings.option(chess.White))
      .add("winner" -> pov.game.winnerColor)
      .add("ratingDiff" -> pov.player.ratingDiff)

  def maybeFen(pov: Pov): Fen.Full =
    if pov.player.blindfold then Fen.Full("8/8/8/8/8/8/8/8") else Fen.write(pov.game.chess)

  def player(p: Player, user: Option[LightUser]) =
    Json
      .obj()
      .add("user", user)
      .add("rating", p.rating)
      .add("ratingDiff", p.ratingDiff)
      .add("name", p.name)
      .add("provisional" -> p.provisional)
      .add("aiLevel" -> p.aiLevel)
      .add("blindfold" -> p.blindfold)

object JsonView:

  given OWrites[chess.Status] = OWrites: s =>
    Json.obj(
      "id" -> s.id,
      "name" -> s.name
    )

  given OWrites[Crosstable.Result] = Json.writes

  given OWrites[Crosstable.Users] = OWrites: users =>
    JsObject(users.toList.map: u =>
      u.id.value -> JsNumber(u.score / 10d))

  given OWrites[Crosstable] = OWrites: c =>
    Json.obj(
      "users" -> c.users,
      "nbGames" -> c.nbGames
      // "results" -> c.results
    )

  given OWrites[Crosstable.Matchup] = OWrites: m =>
    Json.obj(
      "users" -> m.users,
      "nbGames" -> m.users.nbGames
    )

  def crosstable(ct: Crosstable, matchup: Option[Crosstable.Matchup]) =
    Json.toJsObject(ct).add("matchup" -> matchup)

  given OWrites[Blurs] = OWrites: blurs =>
    import lila.game.Blurs.binaryString
    Json.obj(
      "nb" -> blurs.nb,
      "bits" -> blurs.binaryString
    )

  given OWrites[chess.variant.Variant] = OWrites: v =>
    Json.obj(
      "key" -> v.key,
      "name" -> v.name,
      "short" -> v.shortName
    )

  given OWrites[Clock] = OWrites: c =>
    Json.obj(
      "running" -> c.isRunning,
      "initial" -> c.limitSeconds,
      "increment" -> c.incrementSeconds,
      "white" -> c.remainingTime(Color.White).toSeconds,
      "black" -> c.remainingTime(Color.Black).toSeconds,
      "emerg" -> c.config.emergSeconds
    )

  given Writes[Source] = writeAs(_.name)
  given Writes[lila.core.game.GameRule] = writeAs(_.toString)
