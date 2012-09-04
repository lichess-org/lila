package lila
package search

import game.DbGame
import chess.OpeningExplorer

import org.joda.time.format.{ DateTimeFormat, DateTimeFormatter }

object Game {

  object fields {
    val status = "status"
    val turns = "turns"
    val rated = "rated"
    val variant = "variant"
    val uids = "uids"
    val elos = "elos"
    val ai = "ai"
    val opening = "opening"
    val date = "date"
    val duration = "dur"
  }
  import fields._

  def mapping = {
    def field(name: String, typ: String, analyzed: Boolean = false, attrs: Map[String, Any] = Map.empty) =
      name -> (Map(
        "type" -> typ, 
        "index" -> analyzed.fold("analyzed", "not_analyzed")
      ) ++ attrs)
    def obj(name: String, properties: Map[String, Any]) =
      name -> Map("type" -> "object", "properties" -> properties)
    Map(
      "properties" -> List(
        field(status, "short"),
        field(turns, "short"),
        field(rated, "boolean"),
        field(variant, "short"),
        field(uids, "string"),
        field(elos, "short"),
        field(ai, "boolean"),
        field(opening, "string"),
        field(date, "date", attrs = Map("format" -> dateFormat)),
        field(duration, "short")
      ).toMap
    )
  }

  def from(game: DbGame) = for {
    createdAt ← game.createdAt
    updatedAt ← game.updatedAt
  } yield game.id -> Map(
    status -> game.status.id,
    turns -> game.turns,
    rated -> game.rated,
    variant -> game.variant.id,
    uids -> game.userIds,
    elos -> game.userElos,
    ai -> game.hasAi,
    date -> (dateFormatter print createdAt),
    duration -> game.estimateTotalTime
    ).combine(OpeningExplorer openingOf game.pgn)((map, o) ⇒
      map + (opening -> o.code.toLowerCase)
    )

  private val dateFormat = "YYYY-MM-dd HH:mm:ss"

  private val dateFormatter: DateTimeFormatter =
    DateTimeFormat forPattern dateFormat
}
