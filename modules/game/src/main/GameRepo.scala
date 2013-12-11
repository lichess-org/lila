package lila.game

import scala.util.Random

import chess.format.Forsyth
import chess.{ Color, Variant, Status }
import com.github.nscala_time.time.Imports._
import org.joda.time.DateTime
import play.api.libs.json._
import play.modules.reactivemongo.json.BSONFormats.toJSON
import play.modules.reactivemongo.json.ImplicitBSONHandlers.JsObjectWriter
import reactivemongo.bson.{ BSONDocument, BSONBinary, BSONInteger }

import lila.common.PimpedJson._
import lila.db.api._
import lila.db.ByteArray
import lila.db.Implicits._
import lila.user.{ User, Confrontation }

object GameRepo extends GameRepo {
  protected def gameTube = tube.gameTube
}

trait GameRepo {

  protected implicit def gameTube: lila.db.BsTubeInColl[Game]

  type ID = String

  import Game._

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

  def chronologicalFinishedByUser(userId: String): Fu[List[Game]] = $find(
    $query(Query.finished ++ Query.rated ++ Query.user(userId)) sort ($sort asc BSONFields.createdAt)
  )

  def save(progress: Progress): Funit =
    GameDiff(progress.origin, progress.game) |> {
      case (Nil, Nil) ⇒ funit
      case (sets, unsets) ⇒ lila.db.api successful {
        gameTube.coll.update(
          $select(progress.origin.id),
          if (unsets.isEmpty) BSONDocument("$set" -> BSONDocument(sets))
          else BSONDocument("$set" -> BSONDocument(sets), "$unset" -> BSONDocument(unsets))
        )
      }
    }

  def setEloDiffs(id: ID, white: Int, black: Int) =
    $update($select(id), BSONDocument("$set" -> BSONDocument(
      s"p0.${Player.BSONFields.eloDiff}" -> BSONInteger(white), 
      s"p1.${Player.BSONFields.eloDiff}" -> BSONInteger(black))))

  def setUser(id: ID, color: Color, user: User) =     
    $update($select(id), BSONDocument("$set" -> BSONDocument(
      s"p${color.fold(0, 1)}.${Player.BSONFields.elo}" -> BSONInteger(user.elo),
      s"${BSONFields.playerUids}.${color.fold(0, 1)}" -> user.id
    )))

  def setTv(id: ID) {
    $update.fieldUnchecked(id, BSONFields.tvAt, $date(DateTime.now))
  }

  def onTv(nb: Int): Fu[List[Game]] = $find($query.all sort $sort.desc("me.tv"), nb)

  def incBookmarks(id: ID, value: Int) =
    $update($select(id), $inc("bm" -> value))

  def finish(id: ID, winnerId: Option[String]) = $update(
    $select(id),
    winnerId.??(wid ⇒ $set(Game.BSONFields.winnerId -> wid)) ++ $unset(
      Game.BSONFields.positionHashes,
      "p0." + Player.BSONFields.lastDrawOffer,
      "p1." + Player.BSONFields.lastDrawOffer,
      "p0." + Player.BSONFields.isOfferingDraw,
      "p1." + Player.BSONFields.isOfferingDraw,
      "p0." + Player.BSONFields.isProposingTakeback,
      "p1." + Player.BSONFields.isProposingTakeback
    )
  )

  def findRandomStandardCheckmate(distribution: Int): Fu[Option[Game]] = $find.one(
    Query.mate ++ Json.obj("v" -> $exists(false)),
    _ sort Query.sortCreated skip (Random nextInt distribution)
  )

  def insertDenormalized(game: Game): Funit = {
    val bson = (gameTube.handler write game) ++ BSONDocument(
      "if" -> game.variant.exotic.option(Forsyth >> game.toChess)
    )
    $insert bson bson
  }

  def denormalizeUids(game: Game): Funit =
    $update.field(game.id, "us", game.players.map(_.userId).flatten.distinct)

  def saveNext(game: Game, nextId: ID): Funit = $update(
    $select(game.id),
    $set(BSONFields.next -> nextId) ++
      $unset("p0." + Player.BSONFields.isOfferingRematch, "p1." + Player.BSONFields.isOfferingRematch)
  )

  def initialFen(gameId: ID): Fu[Option[String]] =
    $primitive.one($select(gameId), "if")(_.asOpt[String])

  def featuredCandidates: Fu[List[Game]] = $find(
    Query.playable ++ Query.clock(true) ++ Query.turnsGt(1) ++ Json.obj(
      BSONFields.createdAt -> $gt($date(DateTime.now - 3.minutes)),
      BSONFields.updatedAt -> $gt($date(DateTime.now - 15.seconds))
    ))

  def count(query: Query.type ⇒ JsObject): Fu[Int] = $count(query(Query))

  def nbPerDay(days: Int): Fu[List[Int]] =
    ((days to 1 by -1).toList map { day ⇒
      val from = DateTime.now.withTimeAtStartOfDay - day.days
      val to = from + 1.day
      $count(Json.obj(BSONFields.createdAt -> ($gte($date(from)) ++ $lt($date(to)))))
    }).sequenceFu

  def bestOpponents(userId: String, limit: Int): Fu[List[(String, Int)]] = {
    import reactivemongo.bson._
    import reactivemongo.core.commands._
    val command = Aggregate(gameTube.coll.name, Seq(
      Match(BSONDocument("us" -> userId)),
      Match(BSONDocument("us" -> BSONDocument("$size" -> 2))),
      Unwind("us"),
      Match(BSONDocument("us" -> BSONDocument("$ne" -> userId))),
      GroupField("us")("gs" -> SumValue(1)),
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

  def random: Fu[Option[Game]] = $find.one(
    Json.obj("us" -> $exists(true)),
    _ sort Query.sortCreated skip (Random nextInt 1000)
  )

  def findMirror(game: Game): Fu[Option[Game]] = $find.one(
    Query.users(game.userIds) ++ Query.status(Status.Started) ++ Json.obj(
      BSONFields.turns -> game.turns,
      BSONFields.id -> $ne(game.id),
      BSONFields.binaryPieces -> game.binaryPieces,
      BSONFields.createdAt -> $gt($date(DateTime.now - 1.hour)),
      BSONFields.updatedAt -> $gt($date(DateTime.now - 5.minutes))
    ))

  def getPgn(id: ID): Fu[PgnMoves] = getOptionPgn(id) map (~_)

  def getNonEmptyPgn(id: ID): Fu[Option[PgnMoves]] =
    getOptionPgn(id) map (_ filter (_.nonEmpty))

  def getOptionPgn(id: ID): Fu[Option[PgnMoves]] =
    gameTube.coll.find(
      $select(id), Json.obj(
        BSONFields.id -> false,
        BSONFields.binaryPgn -> true
      )
    ).one[BSONDocument] map { _ flatMap extractPgnMoves }

  def associatePgn(ids: Seq[ID]): Fu[Map[String, PgnMoves]] =
    gameTube.coll.find($select byIds ids)
      .cursor[BSONDocument]
      .collect[List]() map2 { (obj: BSONDocument) ⇒
        extractPgnMoves(obj) flatMap { moves ⇒
          obj.getAs[String]("_id") map (_ -> moves)
        }
      } map (_.flatten.toMap)

  def getOneRandomPgn(distrib: Int): Fu[PgnMoves] = {
    gameTube.coll.find($select.all) skip scala.util.Random.nextInt(distrib)
  }.cursor[BSONDocument].collect[List](1) map {
    _.headOption flatMap extractPgnMoves
  } map (~_)

  def recentAverageElo(minutes: Int): Fu[(Int, Int)] = {
    val command = MapReduce(
      collectionName = gameTube.coll.name,
      mapFunction = """function() { 
        emit(!!this.ra, [this.p0, this.p1]); 
      }""",
      reduceFunction = """function(rated, values) {
  var sum = 0, nb = 0;
  values.forEach(function(game) {
    if(typeof game[0] != "undefined") {
      game.forEach(function(player) {
        if(player.e) {
          sum += player.e; 
          ++nb;
        }
      });
    }
  });
  return nb == 0 ? nb : Math.round(sum / nb);
  }""",
      query = Some(JsObjectWriter write {
        Json.obj(
          BSONFields.createdAt -> $gte($date(DateTime.now - minutes.minutes))
        ) ++ $or(List(Json.obj("p0.e" -> $exists(true)), Json.obj("p1.e" -> $exists(true))))
      })
    )
    gameTube.coll.db.command(command) map { res ⇒
      toJSON(res).arr("results").flatMap { r ⇒
        (r(0) int "value") |@| (r(1) int "value") tupled
      }
    } map (~_)
  }

  private def extractPgnMoves(doc: BSONDocument) =
    doc.getAs[BSONBinary](BSONFields.binaryPgn) map { bin ⇒
      BinaryFormat.pgn read { ByteArray.ByteArrayBSONHandler read bin }
    }

  // gets 2 users (id, nbGames)
  // returns user1 wins, draws, losses
  // the 2 userIds SHOULD be sorted by game count desc
  // this method is cached in lila.game.Cached
  private[game] def confrontation(users: ((String, Int), (String, Int))): Fu[Confrontation] = users match {
    case (user1, user2) ⇒ {
      import reactivemongo.bson._
      import reactivemongo.core.commands._
      val userIds = List(user1, user2).sortBy(_._2).map(_._1)
      val command = Aggregate(gameTube.coll.name, Seq(
        Match(BSONDocument(
          "us" -> BSONDocument("$all" -> userIds),
          "s" -> BSONDocument("$gte" -> chess.Status.Mate.id)
        )),
        GroupField(Game.BSONFields.winnerId)("nb" -> SumValue(1))
      ))
      gameTube.coll.db.command(command) map { stream ⇒
        val res = (stream.toList map { obj ⇒
          toJSON(obj).asOpt[JsObject] flatMap { o ⇒
            o int "nb" map { nb ⇒
              ~(o str "_id") -> nb
            }
          }
        }).flatten.toMap
        Confrontation(
          user1._1, user2._1,
          ~(res get user1._1),
          ~(res get ""),
          ~(res get user2._1)
        )
      }
    }
  }
}
