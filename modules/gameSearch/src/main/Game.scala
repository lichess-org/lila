package lila.gameSearch

import play.api.libs.json._

import chess.{ OpeningExplorer, Status }
import lila.game.{ Game ⇒ GameModel }
import lila.search.ElasticSearch
import lila.search.ElasticSearch._

private[gameSearch] object Game {

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
    val analysed = "an"
  }
  import fields._

  def jsonMapping: JsObject = {

    import Mapping._

    def field(
      name: String,
      typ: String,
      analyzed: Boolean = false,
      attrs: JsObject = JsObject(Nil)): (String, JsObject) =
      name -> (Json.obj(
        "type" -> typ,
        "index" -> analyzed.fold("analyzed", "not_analyzed")
      ) ++ attrs)

    def obj(name: String, properties: JsObject) =
      name -> Json.obj("type" -> "object", "properties" -> properties)

    Json.obj(
      "properties" -> JsObject(List(
        field(status, "short"),
        field(turns, "short"),
        field(rated, "boolean"),
        field(variant, "short"),
        field(uids, "string"),
        field(winner, "string"),
        field(averageElo, "short"),
        field(ai, "short"),
        field(opening, "string"),
        field(date, "date", attrs = Json.obj("format" -> Date.format)),
        field(duration, "short"),
        field(analysed, "boolean")
      ))
    )
  }

  def from(game: GameModel, anal: Boolean): JsObject = Json.obj(
    status -> game.status.is(_.Timeout).fold(Status.Resign, game.status).id,
    turns -> math.ceil(game.turns.toFloat / 2),
    rated -> game.rated,
    variant -> game.variant.id,
    uids -> game.userIds,
    winner -> Json.toJson(game.winner flatMap (_.userId)),
    averageElo -> Json.toJson(game.averageUsersElo),
    ai -> Json.toJson(game.aiLevel),
    date -> (Date.formatter print game.createdAt),
    duration -> game.estimateTotalTime,
    opening -> Json.toJson(OpeningExplorer openingOf game.pgnMoves map (_.code.toLowerCase)),
    analysed -> anal
  )
}
