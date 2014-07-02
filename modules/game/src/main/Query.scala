package lila.game

import chess.{ Color, Status }
import org.joda.time.DateTime
import com.github.nscala_time.time.Imports._
import play.api.libs.json._

import lila.db.api._
import lila.user.User

object Query {

  import Game.{ BSONFields => F }

  val all: JsObject = $select.all

  val rated: JsObject = Json.obj(F.rated -> true)

  def rated(u: String): JsObject = user(u) ++ rated

  def status(s: Status) = Json.obj(F.status -> s.id)

  val started: JsObject = Json.obj(F.status-> $gte(Status.Started.id))

  def started(u: String): JsObject = user(u) ++ started

  val playable = Json.obj(F.status -> $lt(Status.Aborted.id))

  val mate = status(Status.Mate)

  val draw: JsObject = Json.obj(F.status -> $in(Seq(Status.Draw.id, Status.Stalemate.id)))

  def draw(u: String): JsObject = user(u) ++ draw

  val finished = Json.obj(F.status -> $gte(Status.Mate.id))

  val notFinished: JsObject = Json.obj(F.status -> $lte(Status.Started.id))

  def notFinished(u: String): JsObject = user(u) ++ notFinished

  val frozen = Json.obj(F.status -> $gte(Status.Mate.id))

  val imported = Json.obj(s"${F.source}" -> Source.Import.id)

  def pgnImport(pgn: String) = imported ++ Json.obj(s"${F.pgnImport}.pgn" -> pgn)

  def clock(c: Boolean) = Json.obj(F.clock -> $exists(c))

  def user(u: String) = Json.obj(F.playerUids -> u)
  def users(u: Seq[String]) = Json.obj(F.playerUids -> $in(u))

  // use the us index
  def win(u: String) = user(u) ++ Json.obj(F.winnerId -> u)

  def loss(u: String) = user(u) ++
    Json.obj(F.status -> $in(Status.finishedWithWinner map (_.id))) ++
    Json.obj(F.winnerId -> ($ne(u) ++ $exists(true)))

  def opponents(u1: User, u2: User) =
    Json.obj(F.playerUids -> $all(List(u1, u2).sortBy(_.count.game).map(_.id)))

  def turnsGt(nb: Int) = Json.obj(F.turns -> $gt(nb))

  def finishByClock = playable ++ clock(true) ++ Json.obj(
    F.createdAt -> $gt($date(DateTime.now - 3.hour)))

  def abandoned = {
    val date = $date(Game.abandonedDate)
    notFinished ++ $or(Seq(
      Json.obj(F.updatedAt -> $lt(date)),
      Json.obj(F.updatedAt -> $exists(false), F.createdAt -> $lt(date))
    ))
  }

  def unplayed = Json.obj(
    F.turns -> $lt(2),
    F.createdAt -> (
      $lt($date(DateTime.now - 24.hours)) ++
      $gt($date(DateTime.now - 25.hours)))
  )

  val sortCreated = $sort desc F.createdAt
}
