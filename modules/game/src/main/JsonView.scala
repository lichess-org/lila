package lila.game

import play.api.libs.json.*

import chess.format.Fen
import chess.variant.Crazyhouse
import chess.{ Clock, Color }
import lila.common.Json.{ *, given }
import lila.common.LightUser

final class JsonView(rematches: Rematches):

  import JsonView.given

  def base(game: Game, initialFen: Option[Fen.Epd]) =
    Json
      .obj(
        "id"            -> game.id,
        "variant"       -> game.variant,
        "speed"         -> game.speed.key,
        "perf"          -> PerfPicker.key(game),
        "rated"         -> game.rated,
        "fen"           -> (Fen write game.chess),
        "player"        -> game.turnColor,
        "turns"         -> game.ply,
        "startedAtTurn" -> game.chess.startedAtPly,
        "source"        -> game.source,
        "status"        -> game.status,
        "createdAt"     -> game.createdAt
      )
      .add("initialFen" -> initialFen)
      .add("threefold" -> game.history.threefoldRepetition)
      .add("boosted" -> game.boosted)
      .add("tournamentId" -> game.tournamentId)
      .add("swissId" -> game.swissId)
      .add("winner" -> game.winnerColor)
      .add("lastMove" -> game.lastMoveKeys)
      .add("check" -> game.situation.checkSquare.map(_.key))
      .add("rematch" -> rematches.getAcceptedId(game.id))
      .add("drawOffers" -> (!game.drawOffers.isEmpty).option(game.drawOffers.normalizedPlies))
      .add("rules" -> game.metadata.nonEmptyRules)

  def ownerPreview(pov: Pov)(using LightUser.GetterSync) =
    Json
      .obj(
        "fullId"   -> pov.fullId,
        "gameId"   -> pov.gameId,
        "fen"      -> (Fen write pov.game.chess),
        "color"    -> pov.color.name,
        "lastMove" -> (pov.game.lastMoveKeys | ""),
        "source"   -> pov.game.source,
        "status"   -> pov.game.status,
        "variant" -> Json.obj(
          "key"  -> pov.game.variant.key,
          "name" -> pov.game.variant.name
        ),
        "speed"    -> pov.game.speed.key,
        "perf"     -> lila.game.PerfPicker.key(pov.game),
        "rated"    -> pov.game.rated,
        "hasMoved" -> pov.hasMoved,
        "opponent" -> Json
          .obj(
            "id" -> pov.opponent.userId,
            "username" -> lila.game.Namer
              .playerTextBlocking(pov.opponent, withRating = false)
          )
          .add("rating" -> pov.opponent.rating)
          .add("ai" -> pov.opponent.aiLevel),
        "isMyTurn" -> pov.isMyTurn
      )
      .add("secondsLeft" -> pov.remainingSeconds)
      .add("tournamentId" -> pov.game.tournamentId)
      .add("swissId" -> pov.game.swissId)
      .add("orientation" -> pov.game.variant.racingKings.option(chess.White))
      .add("winner" -> pov.game.winnerColor)

  def player(p: Player, user: Option[LightUser]) =
    Json
      .obj()
      .add("user", user)
      .add("rating", p.rating)
      .add("ratingDiff", p.ratingDiff)
      .add("name", p.name)
      .add("provisional" -> p.provisional)
      .add("aiLevel" -> p.aiLevel)

object JsonView:

  given OWrites[chess.Status] = OWrites { s =>
    Json.obj(
      "id"   -> s.id,
      "name" -> s.name
    )
  }

  given OWrites[Crosstable.Result] = Json.writes

  given OWrites[Crosstable.Users] with
    def writes(users: Crosstable.Users) =
      JsObject(users.toList.map { u =>
        u.id.value -> JsNumber(u.score / 10d)
      })

  given OWrites[Crosstable] with
    def writes(c: Crosstable) =
      Json.obj(
        "users"   -> c.users,
        "nbGames" -> c.nbGames
        // "results" -> c.results
      )

  given OWrites[Crosstable.Matchup] with
    def writes(m: Crosstable.Matchup) = Json.obj(
      "users"   -> m.users,
      "nbGames" -> m.users.nbGames
    )

  def crosstable(ct: Crosstable, matchup: Option[Crosstable.Matchup]) =
    Json.toJsObject(ct).add("matchup" -> matchup)

  given OWrites[Crazyhouse.Pocket] = OWrites { v =>
    JsObject(
      v.values.collect {
        case (role, nb) if nb > 0 => role.name -> JsNumber(nb)
      }
    )
  }

  given OWrites[chess.variant.Crazyhouse.Data] = OWrites { v =>
    Json.obj("pockets" -> List(v.pockets.white, v.pockets.black))
  }

  given OWrites[Blurs] = OWrites { blurs =>
    Json.obj(
      "nb"   -> blurs.nb,
      "bits" -> blurs.binaryString
    )
  }

  given OWrites[chess.variant.Variant] = OWrites { v =>
    Json.obj(
      "key"   -> v.key,
      "name"  -> v.name,
      "short" -> v.shortName
    )
  }

  given OWrites[Clock] = OWrites { c =>
    Json.obj(
      "running"   -> c.isRunning,
      "initial"   -> c.limitSeconds,
      "increment" -> c.incrementSeconds,
      "white"     -> c.remainingTime(Color.White).toSeconds,
      "black"     -> c.remainingTime(Color.Black).toSeconds,
      "emerg"     -> c.config.emergSeconds
    )
  }

  given OWrites[CorrespondenceClock] = OWrites { c =>
    Json.obj(
      "daysPerTurn" -> c.daysPerTurn,
      "increment"   -> c.increment,
      "white"       -> c.whiteTime,
      "black"       -> c.blackTime
    )
  }

  given OWrites[chess.opening.Opening.AtPly] = OWrites { o =>
    Json.obj(
      "eco"  -> o.opening.eco,
      "name" -> o.opening.name,
      "ply"  -> o.ply
    )
  }

  given OWrites[chess.Division] = OWrites { o =>
    Json.obj(
      "middle" -> o.middle,
      "end"    -> o.end
    )
  }

  given Writes[Source] = Writes { s =>
    JsString(s.name)
  }
  given Writes[GameRule] = Writes { r =>
    JsString(r.key)
  }
