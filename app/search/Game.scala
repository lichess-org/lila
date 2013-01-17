package lila
package search

import ElasticSearch._
import game.DbGame
import chess.{ OpeningExplorer, Status }

private[search] object Game {

  object fields {
    val status = "st"
    val turns = "tu"
    val rated = "ra"
    val variant = "va"
    val uids = "ui"
    val winner = "wi"
    val averageElo = "el"
    val ai = "ai"
    val opening = "op"
    val date = "da"
    val duration = "du"
  }
  import fields._

  def mapping = {
    import Mapping._
    Map(
      "properties" -> List(
        field(status, "short"),
        field(turns, "short"),
        field(rated, "boolean"),
        field(variant, "short"),
        field(uids, "string"),
        field(winner, "string"),
        field(averageElo, "short"),
        field(ai, "short"),
        field(opening, "string"),
        field(date, "date", attrs = Map("format" -> Date.format)),
        field(duration, "short")
      ).toMap
    )
  }

  def from(pgn: String)(game: DbGame) = game.id -> (List(
    status -> game.status.is(_.Timeout).fold(Status.Resign, game.status).id.some,
    turns -> Some(math.ceil(game.turns.toFloat / 2)),
    rated -> game.rated.some,
    variant -> game.variant.id.some,
    uids -> (game.userIds.toNel map (_.list)),
    winner -> (game.winner flatMap (_.userId)),
    averageElo -> game.averageUsersElo,
    ai -> game.aiLevel,
    date -> (Date.formatter print game.createdAt).some,
    duration -> game.estimateTotalTime.some,
    opening -> (OpeningExplorer openingOf pgn map (_.code.toLowerCase))
  ) collect {
      case (x, Some(y)) ⇒ x -> y
    }).toMap
}
