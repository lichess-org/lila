package lila.game

import chess.{ Color, Variant, Status }
import chess.format.Forsyth

import lila.user.User
import lila.db.api._
import lila.db.Implicits._
import tube.gameTube
import lila.common.PimpedJson._

import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers.JsObjectWriter
import play.modules.reactivemongo.json.BSONFormats.toJSON

import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import scala.util.Random

object GameRepo {

  type ID = String

  import Game._, ShortFields._

  def game(gameId: ID): Fu[Option[Game]] = $find byId gameId

  def games(gameIds: Seq[ID]): Fu[List[Game]] = $find byOrderedIds gameIds

  def finished(gameId: ID): Fu[Option[Game]] =
    $find.one($select(gameId) ++ Query.finished)

  def player(gameId: ID, color: Color): Fu[Option[Player]] =
    $find byId gameId map2 { (game: Game) ⇒ game player color }

  def player(gameId: ID, playerId: ID): Fu[Option[Player]] =
    $find byId gameId map { gameOption ⇒
      gameOption flatMap { _ player playerId }
    }

  def player(playerRef: PlayerRef): Fu[Option[Player]] =
    player(playerRef.gameId, playerRef.playerId)

  def pov(gameId: ID, color: Color): Fu[Option[Pov]] =
    $find byId gameId map2 { (game: Game) ⇒ Pov(game, game player color) }

  def pov(gameId: ID, color: String): Fu[Option[Pov]] =
    Color(color) ?? (pov(gameId, _))

  def pov(playerRef: PlayerRef): Fu[Option[Pov]] =
    $find byId playerRef.gameId map { gameOption ⇒
      gameOption flatMap { game ⇒
        game player playerRef.playerId map { Pov(game, _) }
      }
    }

  def pov(fullId: ID): Fu[Option[Pov]] = pov(PlayerRef(fullId))

  def pov(ref: PovRef): Fu[Option[Pov]] = pov(ref.gameId, ref.color)

  def recentByUser(userId: String): Fu[List[Game]] = $find(
    $query(Query user userId) sort Query.sortCreated
  )

  def token(id: ID): Fu[String] =
    $primitive.one($select(id), "tk")(_.asOpt[String]) map (_ | Game.defaultToken)

  def save(progress: Progress): Funit =
    GameDiff(progress.origin.encode, progress.game.encode) |> {
      case (Nil, Nil) ⇒ funit
      case (sets, unsets) ⇒ $update($select(progress.origin.id), unsets.isEmpty.fold(
        $set(sets: _*),
        $set(sets: _*) ++ $unset(unsets: _*)
      ))
    }

  def remove(id: ID) = $remove byId id

  // makes the asumption that player 0 is white!
  // proved to be true on prod DB at March 31 2012
  def setEloDiffs(id: ID, white: Int, black: Int) =
    $update($select(id), $set("p.0.ed" -> white, "p.1.ed" -> black))

  def setUser(id: ID, color: Color, user: User) = {
    val pn = "p." + color.fold(0, 1) + "."
    $update($select(id), $set(Json.obj(
      (pn + "uid") -> user.id,
      (pn + "elo") -> user.elo)))
  }

  def incBookmarks(id: ID, value: Int) =
    $update($select(id), $inc("bm" -> value))

  def finish(id: ID, winnerId: Option[String]) = $update(
    $select(id),
    winnerId.??(wid ⇒ $set("wid" -> wid)) ++ $unset(
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

  def findRandomStandardCheckmate(distribution: Int): Fu[Option[Game]] = $find.one(
    Query.mate ++ Json.obj("v" -> $exists(false)),
    _ sort Query.sortCreated skip (Random nextInt distribution)
  )

  def insertDenormalized(game: Game): Funit = (gameTube toMongo game).fold(
    e ⇒ fufail(e.toString),
    js ⇒ {
      val userIds = game.players.map(_.userId).flatten.distinct
      $insert(List(
        userIds.nonEmpty option ("uids" -> Json.toJson(userIds)),
        game.variant.exotic option ("if" -> JsString(Forsyth >> game.toChess))
      ).flatten.foldLeft(js)(_ + _))
    }
  )

  def denormalizeUids(game: Game): Funit =
    $update.field(game.id, "uids", game.players.map(_.userId).flatten.distinct)

  def saveNext(game: Game, nextId: ID): Funit = $update(
    $select(game.id),
    $set("next" -> nextId) ++
      $unset("p.0.isOfferingRematch", "p.1.isOfferingRematch")
  )

  def initialFen(gameId: ID): Fu[Option[String]] =
    $primitive.one($select(gameId), "if")(_.asOpt[String])

  def unplayedIds: Fu[List[ID]] = $primitive(
    Json.obj("t" -> $lt(2)) ++
      Json.obj(createdAt -> ($lt($date(DateTime.now - 3.day)) ++ $gt($date(DateTime.now - 1.week)))),
    "_id"
  )(_.asOpt[ID])

  def featuredCandidates: Fu[List[Game]] = $find(
    Query.playable ++ Query.clock(true) ++ Json.obj(
      "t" -> $gt(1),
      createdAt -> $gt($date(DateTime.now - 4.minutes)),
      updatedAt -> $gt($date(DateTime.now - 15.seconds))
    ))

  def count(query: Query.type ⇒ JsObject): Fu[Int] = $count(query(Query))

  def recentGames(limit: Int): Fu[List[Game]] = $find(
    $query(Query.started ++ Query.turnsGt(1)) sort Query.sortCreated, limit
  )

  def nbPerDay(days: Int): Fu[List[Int]] =
    ((days to 1 by -1).toList map { day ⇒
      val from = DateTime.now.withTimeAtStartOfDay - day.days
      val to = from + 1.day
      $count(Json.obj(createdAt -> ($gte($date(from)) ++ $lt($date(to)))))
    }).sequence

  def recentAverageElo(minutes: Int): Fu[(Int, Int)] = {
    val command = MapReduce(
      collectionName = gameTube.coll.name,
      mapFunction = """function() { 
        emit(!!this.ra, this.p); 
      }""",
      reduceFunction = """function(rated, values) {
  var sum = 0, nb = 0;
  values.forEach(function(game) {
    if(typeof game[0] != "undefined") {
      game.forEach(function(player) {
        if(player.elo) {
          sum += player.elo; 
          ++nb;
        }
      });
    }
  });
  return nb == 0 ? nb : Math.round(sum / nb);
  }""",
      query = Some(JsObjectWriter write Json.obj(
        createdAt -> $gte($date(DateTime.now - minutes.minutes)),
        "p.elo" -> $exists(true)
      ))
    )
    gameTube.coll.db.command(command) map { res ⇒
      toJSON(res).arr("results").flatMap { r ⇒
        (r(0) int "value") |@| (r(1) int "value") tupled
      }
    } map (~_)
  }

  // get userId opponents sorted by their number of games
  // def bestOpponents(userId: String): Fu[List[String]] = {
  //   val command = MapReduce(
  //     collectionName = gameTube.coll.name,
  //     mapFunction = """function() {
  // for(i in this.uids) { 
  //   if(this.uids[i] != '%s') emit(this.uids[i], 1) 
  // }
  // }""" format userId,
  //     reduceFunction = """function(key, values) { return Array.sum(values); }""",
  //     query = Some(JsObjectWriter write Json.obj("uids" -> userId))
  //   )
  //   gameTube.coll.db.command(command) map { obj ⇒
  //     val pairs = toJSON(obj).arr("results") ?? { res ⇒
  //       (res.value.toList map { o ⇒
  //         o str "_id" flatMap { id ⇒
  //           o int "value" map { id -> _ }
  //         }
  //       }).flatten
  //     } 
  //     pairs sortBy (_._2) map (_._1)
  //   }
  // }
  def bestOpponents(userId: String, limit: Int): Fu[List[(String, Int)]] = {
    import reactivemongo.bson._
    import reactivemongo.core.commands._
    // db.game4.aggregate(
    // {$match: {uids:'controlaltdelete'}}, 
    // {$match: {uids:{$size:2}}}, 
    // {$unwind:'$uids'},
    // {$match: {uids:{$ne:'controlaltdelete'}}},
    // {$group:{_id:'$uids',games: { $sum: 1 }}},
    // {$sort: {'games': -1}},
    // {$limit:10},
    // {$project:{_id:1}})
    val command = Aggregate(gameTube.coll.name, Seq(
      Match(BSONDocument("uids" -> userId)),
      Match(BSONDocument("uids" -> BSONDocument("$size" -> BSONInteger(2)))),
      Unwind("uids"),
      Match(BSONDocument("uids" -> BSONDocument("$ne" -> BSONString(userId)))),
      GroupField("uids")("gs" -> SumValue(1)),
      Sort(Seq(Descending("gs"))),
      Limit(limit)
    ))
    gameTube.coll.db.command(command) map { stream ⇒
      (stream.toList map { obj ⇒
        toJSON(obj).asOpt[JsObject] flatMap { o ⇒
          o str "_id" flatMap { id ⇒
            o int "gs" map { id -> _ }
          }
        }
      }).flatten
    }
  }
}
