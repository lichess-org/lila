package lila.game

import scala.util.Random

import chess.format.Forsyth
import chess.{ Color, Variant, Status }
import com.github.nscala_time.time.Imports._
import org.joda.time.DateTime
import play.api.libs.json._
import play.modules.reactivemongo.json.BSONFormats.toJSON
import play.modules.reactivemongo.json.ImplicitBSONHandlers.JsObjectWriter
import reactivemongo.bson.{ BSONDocument, BSONArray, BSONBinary, BSONInteger }

import lila.common.PimpedJson._
import lila.db.api._
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.ByteArray
import lila.db.Implicits._
import lila.user.User

object GameRepo extends GameRepo {
  protected def gameTube = tube.gameTube
}

trait GameRepo {

  protected implicit def gameTube: lila.db.BsTubeInColl[Game]

  type ID = String

  import Game._
  import Game.{ BSONFields => F }

  def game(gameId: ID): Fu[Option[Game]] = $find byId gameId

  def games(gameIds: Seq[ID]): Fu[List[Game]] = $find byOrderedIds gameIds

  def finished(gameId: ID): Fu[Option[Game]] =
    $find.one($select(gameId) ++ Query.finished)

  def player(gameId: ID, color: Color): Fu[Option[Player]] =
    $find byId gameId map2 { (game: Game) => game player color }

  def player(gameId: ID, playerId: ID): Fu[Option[Player]] =
    $find byId gameId map { gameOption =>
      gameOption flatMap { _ player playerId }
    }

  def player(playerRef: PlayerRef): Fu[Option[Player]] =
    player(playerRef.gameId, playerRef.playerId)

  def pov(gameId: ID, color: Color): Fu[Option[Pov]] =
    $find byId gameId map2 { (game: Game) => Pov(game, game player color) }

  def pov(gameId: ID, color: String): Fu[Option[Pov]] =
    Color(color) ?? (pov(gameId, _))

  def pov(playerRef: PlayerRef): Fu[Option[Pov]] =
    $find byId playerRef.gameId map { gameOption =>
      gameOption flatMap { game =>
        game player playerRef.playerId map { Pov(game, _) }
      }
    }

  def pov(fullId: ID): Fu[Option[Pov]] = pov(PlayerRef(fullId))

  def pov(ref: PovRef): Fu[Option[Pov]] = pov(ref.gameId, ref.color)

  def recentByUser(userId: String): Fu[List[Game]] = $find(
    $query(Query user userId) sort Query.sortCreated
  )

  def chronologicalFinishedByUser(userId: String): Fu[List[Game]] = $find(
    $query(Query.finished ++ Query.rated ++ Query.user(userId)) sort ($sort asc F.createdAt)
  )

  def save(progress: Progress): Funit =
    GameDiff(progress.origin, progress.game) |> {
      case (Nil, Nil) => funit
      case (sets, unsets) => gameTube.coll.update(
        $select(progress.origin.id),
        nonEmptyMod("$set", BSONDocument(sets)) ++ nonEmptyMod("$unset", BSONDocument(unsets))
      ).void
    }

  def nonEmptyMod(mod: String, doc: BSONDocument) =
    if (doc.isEmpty) BSONDocument() else BSONDocument(mod -> doc)

  def setRatingDiffs(id: ID, white: Int, black: Int) =
    $update($select(id), BSONDocument("$set" -> BSONDocument(
      s"${F.whitePlayer}.${Player.BSONFields.ratingDiff}" -> BSONInteger(white),
      s"${F.blackPlayer}.${Player.BSONFields.ratingDiff}" -> BSONInteger(black))))

  def setUsers(id: ID, white: Option[(String, Int)], black: Option[(String, Int)]) =
    if (white.isDefined || black.isDefined) $update($select(id), BSONDocument("$set" -> BSONDocument(
      s"${F.whitePlayer}.${Player.BSONFields.rating}" -> white.map(_._2).map(BSONInteger.apply),
      s"${F.blackPlayer}.${Player.BSONFields.rating}" -> black.map(_._2).map(BSONInteger.apply),
      F.playerUids -> lila.db.BSON.writer.listO(List(~white.map(_._1), ~black.map(_._1)))
    )))
    else funit

  def setTv(id: ID) {
    $update.fieldUnchecked(id, F.tvAt, $date(DateTime.now))
  }

  def onTv(nb: Int): Fu[List[Game]] = $find($query.all sort $sort.desc(F.tvAt), nb)

  def incBookmarks(id: ID, value: Int) =
    $update($select(id), $incBson("bm" -> value))

  def setHoldAlert(pov: Pov, mean: Int, sd: Int) = {
    import Player.holdAlertBSONHandler
    $update(
      $select(pov.gameId),
      BSONDocument(
        "$set" -> BSONDocument(
          s"p${pov.color.fold(0, 1)}.${Player.BSONFields.holdAlert}" ->
            Player.HoldAlert(ply = pov.game.turns, mean = mean, sd = sd)
        )
      )
    ).void
  }

  def finish(id: ID, winnerColor: Option[Color], winnerId: Option[String]) = $update(
    $select(id),
    nonEmptyMod("$set", BSONDocument(
      F.winnerId -> winnerId,
      F.winnerColor -> winnerColor.map(_.white)
    )) ++ nonEmptyMod("$unset", BSONDocument(
      F.positionHashes -> true,
      ("p0." + Player.BSONFields.lastDrawOffer) -> true,
      ("p1." + Player.BSONFields.lastDrawOffer) -> true,
      ("p0." + Player.BSONFields.isOfferingDraw) -> true,
      ("p1." + Player.BSONFields.isOfferingDraw) -> true,
      ("p0." + Player.BSONFields.isProposingTakeback) -> true,
      ("p1." + Player.BSONFields.isProposingTakeback) -> true
    ))
  )

  def findRandomStandardCheckmate(distribution: Int): Fu[Option[Game]] = $find.one(
    Query.mate ++ Json.obj("v" -> $exists(false)),
    _ sort Query.sortCreated skip (Random nextInt distribution)
  )

  def insertDenormalized(game: Game, ratedCheck: Boolean = true): Funit = {
    val g2 = if (ratedCheck && game.rated && game.userIds.distinct.size != 2)
      game.copy(mode = chess.Mode.Casual)
    else game
    val bson = (gameTube.handler write g2) ++ BSONDocument(
      "if" -> g2.variant.exotic.option(Forsyth >> g2.toChess)
    )
    $insert bson bson
  }

  def saveNext(game: Game, nextId: ID): Funit = $update(
    $select(game.id),
    $set(F.next -> nextId) ++
      $unset("p0." + Player.BSONFields.isOfferingRematch, "p1." + Player.BSONFields.isOfferingRematch)
  )

  def initialFen(gameId: ID): Fu[Option[String]] =
    $primitive.one($select(gameId), "if")(_.asOpt[String])

  def featuredCandidates: Fu[List[Game]] = $find(
    Query.playable ++ Query.clock(true) ++ Query.turnsGt(1) ++ Json.obj(
      F.createdAt -> $gt($date(DateTime.now - 3.minutes)),
      F.updatedAt -> $gt($date(DateTime.now - 15.seconds))
    ))

  def count(query: Query.type => JsObject): Fu[Int] = $count(query(Query))

  def nbPerDay(days: Int): Fu[List[Int]] =
    ((days to 1 by -1).toList map { day =>
      val from = DateTime.now.withTimeAtStartOfDay - day.days
      val to = from + 1.day
      $count(Json.obj(F.createdAt -> ($gte($date(from)) ++ $lt($date(to)))))
    }).sequenceFu

  def nowPlaying(userId: String): Fu[Option[Game]] =
    $find.one(Query.status(Status.Started) ++ Query.user(userId) ++ Json.obj(
      F.createdAt -> $gt($date(DateTime.now - 30.minutes))
    ))

  def isNowPlaying(userId: String): Fu[Boolean] = nowPlaying(userId) map (_.isDefined)

  def bestOpponents(userId: String, limit: Int): Fu[List[(String, Int)]] = {
    import reactivemongo.bson._
    import reactivemongo.core.commands._
    val command = Aggregate(gameTube.coll.name, Seq(
      Match(BSONDocument(F.playerUids -> userId)),
      Match(BSONDocument(F.playerUids -> BSONDocument("$size" -> 2))),
      Unwind(F.playerUids),
      Match(BSONDocument(F.playerUids -> BSONDocument("$ne" -> userId))),
      GroupField(F.playerUids)("gs" -> SumValue(1)),
      Sort(Seq(Descending("gs"))),
      Limit(limit)
    ))
    gameTube.coll.db.command(command) map { stream =>
      (stream.toList map { obj =>
        toJSON(obj).asOpt[JsObject] flatMap { o =>
          o str "_id" flatMap { id =>
            o int "gs" map { id -> _ }
          }
        }
      }).flatten
    }
  }

  def random: Fu[Option[Game]] = $find.one(
    $query.all sort Query.sortCreated skip (Random nextInt 100)
  )

  def findMirror(game: Game): Fu[Option[Game]] = $find.one($query(
    BSONDocument(
      F.id -> BSONDocument("$ne" -> game.id),
      F.playerUids -> BSONDocument("$in" -> game.userIds),
      F.status -> Status.Started.id,
      F.createdAt -> BSONDocument("$gt" -> (DateTime.now - 15.minutes)),
      F.updatedAt -> BSONDocument("$gt" -> (DateTime.now - 5.minutes)),
      "$or" -> BSONArray(
        BSONDocument(s"${F.whitePlayer}.ai" -> BSONDocument("$exists" -> true)),
        BSONDocument(s"${F.blackPlayer}.ai" -> BSONDocument("$exists" -> true))
      ),
      F.binaryPieces -> game.binaryPieces
    )
  ))

  def getPgn(id: ID): Fu[PgnMoves] = getOptionPgn(id) map (~_)

  def getNonEmptyPgn(id: ID): Fu[Option[PgnMoves]] =
    getOptionPgn(id) map (_ filter (_.nonEmpty))

  def getOptionPgn(id: ID): Fu[Option[PgnMoves]] =
    gameTube.coll.find(
      $select(id), Json.obj(
        F.id -> false,
        F.binaryPgn -> true
      )
    ).one[BSONDocument] map { _ flatMap extractPgnMoves }

  def associatePgn(ids: Seq[ID]): Fu[Map[String, PgnMoves]] =
    gameTube.coll.find($select byIds ids)
      .cursor[BSONDocument]
      .collect[List]() map2 { (obj: BSONDocument) =>
        extractPgnMoves(obj) flatMap { moves =>
          obj.getAs[String]("_id") map (_ -> moves)
        }
      } map (_.flatten.toMap)

  def lastGameBetween(u1: String, u2: String, since: DateTime): Fu[Option[Game]] = {
    $find.one(Json.obj(
      F.playerUids -> Json.obj("$all" -> List(u1, u2)),
      F.createdAt -> Json.obj("$gt" -> $date(since))
    ))
  }

  def activePlayersSince(since: DateTime, max: Int): Fu[List[(String, Int)]] = {
    import reactivemongo.bson._
    import reactivemongo.core.commands._
    import lila.db.BSON.BSONJodaDateTimeHandler
    val command = Aggregate(gameTube.coll.name, Seq(
      Match(BSONDocument(
        F.createdAt -> BSONDocument("$gt" -> since),
        F.status -> BSONDocument("$gte" -> chess.Status.Mate.id),
        s"${F.playerUids}.0" -> BSONDocument("$exists" -> true)
      )),
      Unwind(F.playerUids),
      Match(BSONDocument(
        F.playerUids -> BSONDocument("$ne" -> "")
      )),
      GroupField(F.playerUids)("nb" -> SumValue(1)),
      Sort(Seq(Descending("nb"))),
      Limit(max)
    ))
    gameTube.coll.db.command(command) map { stream =>
      (stream.toList map { obj =>
        toJSON(obj).asOpt[JsObject] flatMap { o =>
          o int "nb" map { nb =>
            ~(o str "_id") -> nb
          }
        }
      }).flatten
    }
  }

  private def extractPgnMoves(doc: BSONDocument) =
    doc.getAs[BSONBinary](F.binaryPgn) map { bin =>
      BinaryFormat.pgn read { ByteArray.ByteArrayBSONHandler read bin }
    }
}
