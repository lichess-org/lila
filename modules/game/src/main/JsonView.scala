package lila.game

import play.api.libs.json._

import chess.format.{ FEN, Forsyth }
import chess.variant.Crazyhouse
import chess.{ Clock, Color }
import lila.common.Json._
import lila.common.LightUser

final class JsonView(rematches: Rematches) {

  import JsonView._

  def apply(game: Game, initialFen: Option[FEN]) =
    Json
      .obj(
        "id"            -> game.id,
        "variant"       -> game.variant,
        "speed"         -> game.speed.key,
        "perf"          -> PerfPicker.key(game),
        "rated"         -> game.rated,
        "fen"           -> (Forsyth >> game.chess),
        "player"        -> game.turnColor,
        "turns"         -> game.turns,
        "startedAtTurn" -> game.chess.startedAtTurn,
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

  def ownerPreview(pov: Pov)(lightUserSync: LightUser.GetterSync) =
    Json
      .obj(
        "fullId"   -> pov.fullId,
        "gameId"   -> pov.gameId,
        "fen"      -> (Forsyth >> pov.game.chess),
        "color"    -> (if (pov.game.variant.racingKings) chess.White else pov.color).name,
        "lastMove" -> ~pov.game.lastMoveKeys,
        "source"   -> pov.game.source,
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
              .playerTextBlocking(pov.opponent, withRating = false)(lightUserSync)
          )
          .add("rating" -> pov.opponent.rating)
          .add("ai" -> pov.opponent.aiLevel),
        "isMyTurn" -> pov.isMyTurn
      )
      .add("secondsLeft" -> pov.remainingSeconds)
      .add("tournamentId" -> pov.game.tournamentId)
      .add("swissId" -> pov.game.tournamentId)
}

object JsonView {

  implicit val statusWrites: OWrites[chess.Status] = OWrites { s =>
    Json.obj(
      "id"   -> s.id,
      "name" -> s.name
    )
  }

  implicit val crosstableResultWrites = Json.writes[Crosstable.Result]

  implicit val crosstableUsersWrites = OWrites[Crosstable.Users] { users =>
    JsObject(users.toList.map { u =>
      u.id -> JsNumber(u.score / 10d)
    })
  }

  implicit val crosstableWrites = OWrites[Crosstable] { c =>
    Json.obj(
      "users"   -> c.users,
      "nbGames" -> c.nbGames
      // "results" -> c.results
    )
  }

  implicit val matchupWrites = OWrites[Crosstable.Matchup] { m =>
    Json.obj(
      "users"   -> m.users,
      "nbGames" -> m.users.nbGames
    )
  }

  def crosstable(ct: Crosstable, matchup: Option[Crosstable.Matchup]) =
    crosstableWrites
      .writes(ct)
      .add("matchup" -> matchup)

  implicit val crazyhousePocketWriter: OWrites[Crazyhouse.Pocket] = OWrites { v =>
    JsObject(
      Crazyhouse.storableRoles.flatMap { role =>
        Some(v.roles.count(role ==)).filter(0 <).map { count =>
          role.name -> JsNumber(count)
        }
      }
    )
  }

  implicit val crazyhouseDataWriter: OWrites[chess.variant.Crazyhouse.Data] = OWrites { v =>
    Json.obj("pockets" -> List(v.pockets.white, v.pockets.black))
  }

  implicit val blursWriter: OWrites[Blurs] = OWrites { blurs =>
    Json.obj(
      "nb"   -> blurs.nb,
      "bits" -> blurs.binaryString
    )
  }

  implicit val variantWriter: OWrites[chess.variant.Variant] = OWrites { v =>
    Json.obj(
      "key"   -> v.key,
      "name"  -> v.name,
      "short" -> v.shortName
    )
  }

  implicit val clockWriter: OWrites[Clock] = OWrites { c =>
    Json.obj(
      "running"   -> c.isRunning,
      "initial"   -> c.limitSeconds,
      "increment" -> c.incrementSeconds,
      "white"     -> c.remainingTime(Color.White).toSeconds,
      "black"     -> c.remainingTime(Color.Black).toSeconds,
      "emerg"     -> c.config.emergSeconds
    )
  }

  implicit val correspondenceWriter: OWrites[CorrespondenceClock] = OWrites { c =>
    Json.obj(
      "daysPerTurn" -> c.daysPerTurn,
      "increment"   -> c.increment,
      "white"       -> c.whiteTime,
      "black"       -> c.blackTime
    )
  }

  implicit val openingWriter: OWrites[chess.opening.FullOpening.AtPly] = OWrites { o =>
    Json.obj(
      "eco"  -> o.opening.eco,
      "name" -> o.opening.name,
      "ply"  -> o.ply
    )
  }

  implicit val divisionWriter: OWrites[chess.Division] = OWrites { o =>
    Json.obj(
      "middle" -> o.middle,
      "end"    -> o.end
    )
  }

  implicit val sourceWriter: Writes[Source] = Writes { s =>
    JsString(s.name)
  }
}
