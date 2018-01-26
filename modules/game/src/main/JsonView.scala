package lila.game

import play.api.libs.json._

import chess.format.{ FEN, Forsyth }
import chess.variant.Crazyhouse
import chess.{ Color, Clock }

object JsonView {

  def gameJson(game: Game, initialFen: Option[FEN]) = Json.obj(
    "id" -> game.id,
    "variant" -> game.variant,
    "speed" -> game.speed.key,
    "perf" -> PerfPicker.key(game),
    "rated" -> game.rated,
    "initialFen" -> initialFen.fold(chess.format.Forsyth.initial)(_.value),
    "fen" -> (Forsyth >> game.chess),
    "player" -> game.turnColor,
    "turns" -> game.turns,
    "startedAtTurn" -> game.chess.startedAtTurn,
    "source" -> game.source,
    "status" -> game.status,
    "createdAt" -> game.createdAt
  ).add("threefold" -> game.history.threefoldRepetition)
    .add("boosted" -> game.boosted)
    .add("tournamentId" -> game.tournamentId)
    .add("winner" -> game.winnerColor)
    .add("lastMove" -> game.lastMoveKeys)
    .add("check" -> game.situation.checkSquare.map(_.key))
    .add("rematch" -> game.next)

  implicit val statusWrites: OWrites[chess.Status] = OWrites { s =>
    Json.obj(
      "id" -> s.id,
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
      "users" -> c.users,
      "results" -> c.results,
      "nbGames" -> c.nbGames
    )
  }

  implicit val matchupWrites = OWrites[Crosstable.Matchup] { m =>
    Json.obj(
      "users" -> m.users,
      "nbGames" -> m.users.nbGames
    )
  }
  implicit val crosstableWithMatchupWrites = Json.writes[Crosstable.WithMatchup]

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
    Json.obj("nb" -> blurs.nb).add("bits" -> (blurs match {
      case bits: Blurs.Bits => bits.binaryString.some
      case _ => none
    }))
  }

  implicit val variantWriter: OWrites[chess.variant.Variant] = OWrites { v =>
    Json.obj(
      "key" -> v.key,
      "name" -> v.name,
      "short" -> v.shortName
    )
  }

  implicit val clockWriter: OWrites[Clock] = OWrites { c =>
    Json.obj(
      "running" -> c.isRunning,
      "initial" -> c.limitSeconds,
      "increment" -> c.incrementSeconds,
      "white" -> c.remainingTime(Color.White).toSeconds,
      "black" -> c.remainingTime(Color.Black).toSeconds,
      "emerg" -> c.config.emergSeconds
    )
  }

  implicit val correspondenceWriter: OWrites[CorrespondenceClock] = OWrites { c =>
    Json.obj(
      "daysPerTurn" -> c.daysPerTurn,
      "increment" -> c.increment,
      "white" -> c.whiteTime,
      "black" -> c.blackTime
    )
  }

  implicit val openingWriter: OWrites[chess.opening.FullOpening.AtPly] = OWrites { o =>
    Json.obj(
      "eco" -> o.opening.eco,
      "name" -> o.opening.name,
      "ply" -> o.ply
    )
  }

  implicit val divisionWriter: OWrites[chess.Division] = OWrites { o =>
    Json.obj(
      "middle" -> o.middle,
      "end" -> o.end
    )
  }

  implicit val sourceWriter: Writes[Source] = Writes { s => JsString(s.name) }

  implicit val colorWrites: Writes[Color] = Writes { c =>
    JsString(c.name)
  }
}
