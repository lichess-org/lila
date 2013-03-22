package lila.game

import chess.{ Color, Variant, Status }
import chess.format.Forsyth

import lila.user.User
import lila.db.{ Repo, DbApi }
import lila.db.Implicits._

import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._

import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.core.commands._

import play.modules.reactivemongo.Implicits._
import play.modules.reactivemongo.MongoJSONHelpers._

import com.roundeights.hasher.Implicits._
import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import scala.util.Random

final class GameRepo(coll: ReactiveColl) extends Repo[String, Game](coll, Game.json) {

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

  // def pov(ref: PovRef): Fu[Option[Pov]] = pov(ref.gameId, ref.color)

  // def token(id: ID): Fu[String] = io {
  //   primitiveProjection[String](idSelector(id), "tk") | Game.defaultToken
  // }

  // def save(game: Game): Fu[Unit] = io {
  //   update(idSelector(game), _grater asDBObject game.encode)
  // }

  // def save(progress: Progress): Fu[Unit] =
  //   GameDiff(progress.origin.encode, progress.game.encode) |> {
  //     case (Nil, Nil) ⇒ io()
  //     case (sets, unsets) ⇒ {
  //       val fullSets = ("ua" -> new Date) :: sets
  //       val ops = unsets.isEmpty.fold(
  //         $set(fullSets),
  //         $set(fullSets) ++ $unset(unsets)
  //       )
  //       val wc = WriteConcern.None
  //       io { collection.update(idSelector(progress.origin), ops, concern = wc) }
  //     }
  //   }

  // def insert(game: Game): Fu[Option[String]] = io {
  //   insert(game.encode)
  // }

  // // makes the asumption that player 0 is white!
  // // proved to be true on prod DB at March 31 2012
  // def setEloDiffs(id: ID, white: Int, black: Int) = io {
  //   update(idSelector(id), $set(Seq("p.0.ed" -> white, "p.1.ed" -> black)))
  // }

  // def setUser(id: ID, color: Color, user: User) = io {
  //   val pn = "p.%d".format(color.fold(0, 1))
  //   update(idSelector(id), $set(Seq(pn + ".uid" -> user.id, pn + ".elo" -> user.elo)))
  // }

  // def incBookmarks(id: ID, value: Int) = io {
  //   update(idSelector(id), $inc("bm" -> value))
  // }

  // def finish(id: ID, winnerId: Option[String]) = io {
  //   update(
  //     idSelector(id),
  //     (winnerId.fold($set(Seq.empty)) { userId ⇒ $set(Seq("wid" -> userId)) }) ++ $unset(Seq(
  //       "c.t",
  //       "ph",
  //       "lmt",
  //       "p.0.previousMoveTs",
  //       "p.1.previousMoveTs",
  //       "p.0.lastDrawOffer",
  //       "p.1.lastDrawOffer",
  //       "p.0.isOfferingDraw",
  //       "p.1.isOfferingDraw",
  //       "p.0.isProposingTakeback",
  //       "p.1.isProposingTakeback"
  //     ))
  //   )
  // }

  // def findRandomStandardCheckmate(distribution: Int): Fu[Option[Game]] = io {
  //   find(Query.mate ++ ("v" $exists false))
  //     .sort(Query.sortCreated)
  //     .limit(1)
  //     .skip(Random nextInt distribution)
  //     .toList.map(_.decode).flatten.headOption
  // }

  // def denormalize(game: Game): Fu[Unit] = io {
  //   val userIds = game.players.map(_.userId).flatten
  //   if (userIds.nonEmpty) update(idSelector(game), $set(Seq("uids" -> userIds)))
  //   if (game.mode.rated) update(idSelector(game), $set(Seq("ra" -> true)))
  //   if (game.variant.exotic) update(idSelector(game), $set(Seq("if" -> (Forsyth >> game.toChess))))
  // }

  // def saveNext(game: Game, nextId: ID): Fu[Unit] = io {
  //   update(
  //     idSelector(game),
  //     $set(Seq("next" -> nextId)) ++
  //       $unset(Seq("p.0.isOfferingRematch", "p.1.isOfferingRematch"))
  //   )
  // }

  // def initialFen(gameId: ID): Fu[Option[String]] = io {
  //   primitiveProjection[String](idSelector(gameId), "if")
  // }

  // val unplayedIds: Fu[List[ID]] = io {
  //   primitiveProjections[ID](
  //     ("t" $lt 2) ++ ("ca" $lt (DateTime.now - 3.day) $gt (DateTime.now - 1.week)),
  //     "_id"
  //   )
  // }

  // // bookmarks and pgns should also be removed
  // def remove(id: ID): Fu[Unit] = io {
  //   remove(idSelector(id))
  // }

  // // bookmarks and pgns should also be removed
  // def removeIds(ids: List[ID]): Fu[Unit] = io {
  //   remove("_id" $in ids)
  // }

  // val candidatesToAutofinish: Fu[List[Game]] = io {
  //   find(Query.playable ++
  //     Query.clock(true) ++
  //     ("ca" $gt (DateTime.now - 1.day)) ++ // index
  //     ("ua" $lt (DateTime.now - 2.hour))
  //   ).toList.map(_.decode).flatten
  // }

  // def abandoned(max: Int): Fu[List[Game]] = io {
  //   find(
  //     Query.notFinished ++ ("ua" $lt Game.abandonedDate)
  //   ).limit(max).toList.map(_.decode).flatten
  // }

  // val featuredCandidates: Fu[List[Game]] = io {
  //   find(Query.playable ++
  //     Query.clock(true) ++
  //     ("t" $gt 1) ++
  //     ("ca" $gt (DateTime.now - 4.minutes)) ++
  //     ("ua" $gt (DateTime.now - 15.seconds))
  //   ).toList.map(_.decode).flatten
  // }

  // def count(query: DBObject): Fu[Int] = io {
  //   super.count(query).toInt
  // }

  // def count(query: Query.type ⇒ DBObject): Fu[Int] = count(query(Query))

  // def exists(id: ID) = count(idSelector(id)) map (_ > 0)

  // def recentGames(limit: Int): Fu[List[Game]] = io {
  //   find(Query.started ++ Query.turnsGt(1))
  //     .sort(Query.sortCreated)
  //     .limit(limit)
  //     .toList.map(_.decode).flatten
  // }

  // def games(ids: List[ID]): Fu[List[Game]] = io {
  //   find("_id" $in ids).toList.map(_.decode).flatten
  // } map { gs ⇒
  //   val gsMap = gs.map(g ⇒ g.id -> g).toMap
  //   ids.map(gsMap.get).flatten
  // }

  // def nbPerDay(days: Int): Fu[List[Int]] = ((days to 1 by -1).toList map { day ⇒
  //   val from = DateTime.now.withTimeAtStartOfDay - day.days
  //   val to = from + 1.day
  //   count(("ca" $gte from $lt to))
  // }).sequence

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

  // private def idSelector(game: Game): DBObject = idSelector(game.id)
  // private def idSelector(id: ID): DBObject = DBObject("_id" -> id)
}
