package lila.game

import chess.{ Color, Status }
import org.joda.time.DateTime
import play.api.libs.json._

import lila.db.api._
import lila.user.User

object Query {

  import Game.{ BSONFields => F }

  val all: JsObject = $select.all

  val rated: JsObject = Json.obj(F.rated -> true)

  def rated(u: String): JsObject = user(u) ++ rated

  def status(s: Status) = Json.obj(F.status -> s.id)

  val started: JsObject = Json.obj(F.status -> $gte(Status.Started.id))

  def started(u: String): JsObject = user(u) ++ started

  val playable = Json.obj(F.status -> $lt(Status.Aborted.id))

  val mate = status(Status.Mate)

  val draw: JsObject = Json.obj(F.status -> $in(Seq(Status.Draw.id, Status.Stalemate.id)))

  def draw(u: String): JsObject = user(u) ++ draw

  val finished = Json.obj(F.status -> $gte(Status.Mate.id))

  val notFinished: JsObject = Json.obj(F.status -> $lte(Status.Started.id))

  val frozen = Json.obj(F.status -> $gte(Status.Mate.id))

  val imported: JsObject = Json.obj(s"${F.source}" -> Source.Import.id)

  def imported(u: String): JsObject = Json.obj(s"${F.pgnImport}.user" -> u)

  def pgnImport(pgn: String) = imported ++ Json.obj(s"${F.pgnImport}.pgn" -> pgn)

  def clock(c: Boolean) = Json.obj(F.clock -> $exists(c))

  def user(u: String) = Json.obj(F.playerUids -> u)
  def users(u: Seq[String]) = Json.obj(F.playerUids -> $in(u))

  val noAi = Json.obj(
    "p0.ai" -> $exists(false),
    "p1.ai" -> $exists(false))

  def nowPlaying(u: String) = Json.obj(F.playingUids -> u)

  def recentlyPlayingWithClock(u: String) =
    nowPlaying(u) ++ clock(true) ++ Json.obj(
      F.updatedAt -> $gt($date(DateTime.now minusMinutes 5))
    )

  // use the us index
  def win(u: String) = user(u) ++ Json.obj(F.winnerId -> u)

  def loss(u: String) = user(u) ++
    Json.obj(F.status -> $in(Status.finishedWithWinner map (_.id))) ++
    Json.obj(F.winnerId -> ($ne(u) ++ $exists(true)))

  def opponents(u1: User, u2: User) =
    Json.obj(F.playerUids -> $all(List(u1, u2).sortBy(_.count.game).map(_.id)))

  def turnsGt(nb: Int) = Json.obj(F.turns -> $gt(nb))

  def checkable = Json.obj(F.checkAt -> $lt($date(DateTime.now)))

  val sortCreated = $sort desc F.createdAt
  val sortUpdatedNoIndex = $sort desc F.updatedAt
}
