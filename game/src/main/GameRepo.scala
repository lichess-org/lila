package lila.game

import chess.{ Color, Variant, Status }
import chess.format.Forsyth

import lila.user.User
import lila.db.Repo
import lila.db.Implicits._

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.core.commands._

import com.roundeights.hasher.Implicits._
import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import scala.util.Random
import scala.concurrent.Future

final class GameRepo(implicit coll: Coll) extends Repo[String, Game](Game.json) {

  type ID = String

  import Game._

  def player(gameId: ID, color: Color): Fu[Option[Player]] =
    find byId gameId map2 { game: Game ⇒ game player color }

  def pov(gameId: ID, color: Color): Fu[Option[Pov]] =
    find byId gameId map2 { game: Game ⇒ Pov(game, game player color) }

  def pov(gameId: ID, color: String): Fu[Option[Pov]] =
    Color(color) zmap (pov(gameId, _))

  def pov(fullId: ID): Fu[Option[Pov]] =
    find byId (fullId take gameIdSize) map { gameOption ⇒
      gameOption flatMap { g ⇒
        g player (fullId drop gameIdSize) map { Pov(g, _) }
      }
    }

  def pov(ref: PovRef): Fu[Option[Pov]] = pov(ref.gameId, ref.color)

  def recentByUser(userId: String): Fu[List[Game]] = find(
    query(Query user userId) sort Query.sortCreated
  )

  def token(id: ID): Fu[String] =
    primitive.one[String](select(id), "tk")(_.asOpt[String]) map (_ | Game.defaultToken)

  def save(progress: Progress): Funit =
    GameDiff(progress.origin.encode, progress.game.encode) |> {
      case (Nil, Nil) ⇒ funit
      case (sets, unsets) ⇒ update(select(progress.origin.id), unsets.isEmpty.fold(
        $set(sets: _*),
        $set(sets: _*) ++ $unset(unsets: _*)
      ))
    }

  // makes the asumption that player 0 is white!
  // proved to be true on prod DB at March 31 2012
  def setEloDiffs(id: ID, white: Int, black: Int) =
    update(select(id), $set("p.0.ed" -> white, "p.1.ed" -> black))

  def setUser(id: ID, color: Color, user: User) = {
    val pn = "p" + color.fold(0, 1)
    update(select(id), $set(
      pn + ".uid" -> Json.toJson(user.id),
      pn + ".elo" -> Json.toJson(user.elo))
    )
  }

  def incBookmarks(id: ID, value: Int) = update(select(id), $inc("bm" -> value))

  def finish(id: ID, winnerId: Option[String]) = update(
    select(id),
    winnerId.zmap(wid ⇒ $set("wid" -> wid)) ++ $unset(
      "c.t",
      "ph",
      "lmt",
      "p.0.previousMoveTs",
      "p.1.previousMoveTs",
      "p.0.lastDrawOffer",
      "p.1.lastDrawOffer",
      "p.0.isOfferingDraw",
      "p.1.isOfferingDraw",
      "p.0.isProposingTakeback",
      "p.1.isProposingTakeback"
    )
  )

  def findRandomStandardCheckmate(distribution: Int): Fu[Option[Game]] = find.one(
    Query.mate ++ Json.obj("v" -> $exists(false)),
    _ sort Query.sortCreated limit 1 skip (Random nextInt distribution)
  )

  def denormalize(game: Game): Funit = {
    val userIds = game.players.map(_.userId).flatten
    Future.sequence(List(
      userIds.nonEmpty ?? update(select(game.id), $set("uids" -> userIds)),
      game.mode.rated ?? update(select(game.id), $set("ra" -> true)),
      game.variant.exotic ?? update(select(game.id), $set("if" -> (Forsyth >> game.toChess)))
    )).void
  }

  def saveNext(game: Game, nextId: ID): Funit = update(
    select(game.id),
    $set("next" -> nextId) ++
      $unset("p.0.isOfferingRematch", "p.1.isOfferingRematch")
  )

  def initialFen(gameId: ID): Fu[Option[String]] =
    primitive.one(select(gameId), "if")(_.asOpt[String])

  def unplayedIds: Fu[List[ID]] = primitive(
    Json.obj("t" -> $lt(2)) ++
      Json.obj("ca" -> ($lt(DateTime.now - 3.day) ++ $gt(DateTime.now - 1.week))),
    "_id"
  )(_.asOpt[ID])

  def candidatesToAutofinish: Fu[List[Game]] =
    find(Query.playable ++ Query.clock(true) ++ Json.obj(
      "ca" -> $gt(DateTime.now - 1.day),
      "ua" -> $lt(DateTime.now - 2.hour)
    ))

  def abandoned(max: Int): Fu[List[Game]] = find(
    Query.notFinished ++ Json.obj("ua" -> $lt(Game.abandonedDate)),
    max)

  val featuredCandidates: Fu[List[Game]] = find(
    Query.playable ++ Query.clock(true) ++ Json.obj(
      "t" -> $gt(1),
      "ca" -> $gt(DateTime.now - 4.minutes),
      "ua" -> $gt(DateTime.now - 15.seconds)
    ))

  def count(query: Query.type ⇒ JsObject): Fu[Int] = count(query(Query))

  def recentGames(limit: Int): Fu[List[Game]] = find(
    query(Query.started ++ Query.turnsGt(1)) sort Query.sortCreated limit limit
  )

  def nbPerDay(days: Int): Fu[List[Int]] =
    Future.traverse((days to 1 by -1).toList) { day ⇒
      val from = DateTime.now.withTimeAtStartOfDay - day.days
      val to = from + 1.day
      count(Json.obj("ca" -> ($gte(from) ++ $lt(to))))
    }

  // def recentAverageElo(minutes: Int): Fu[(Int, Int)] = io {
  //   val result = collection.mapReduce(
  //     mapFunction = """function() { 
  //       emit(!!this.ra, this.p); 
  //     }""",
  //     reduceFunction = """function(rated, values) {
  // var sum = 0, nb = 0;
  // values.forEach(function(game) {
  //   if(typeof game[0] != "undefined") {
  //     game.forEach(function(player) {
  //       if(player.elo) {
  //         sum += player.elo; 
  //         ++nb;
  //       }
  //     });
  //   }
  // });
  // return nb == 0 ? nb : Math.round(sum / nb);
  // }""",
  //     output = MapReduceInlineOutput,
  //     query = Some {
  //       ("ca" $gte (DateTime.now - minutes.minutes)) ++ ("p.elo" $exists true)
  //     }
  //   )
  //   (for {
  //     ratedRow ← result.hasNext option result.next
  //     rated ← ratedRow.getAs[Double]("value")
  //     casualRow ← result.hasNext option result.next
  //     casual ← casualRow.getAs[Double]("value")
  //   } yield rated.toInt -> casual.toInt) | (0, 0)
  // }
}
