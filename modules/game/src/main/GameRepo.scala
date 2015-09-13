package lila.game

import scala.util.Random

import chess.format.Forsyth
import chess.{ Color, Status }
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
import lila.user.{ User, UidNb }

object GameRepo {

  import tube.gameTube

  type ID = String

  import BSONHandlers._
  import Game.{ BSONFields => F }

  def game(gameId: ID): Fu[Option[Game]] = $find byId gameId

  def games(gameIds: Seq[ID]): Fu[List[Game]] = $find byOrderedIds gameIds

  def gameOptions(gameIds: Seq[ID]): Fu[Seq[Option[Game]]] =
    $find optionsByOrderedIds gameIds

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

  def remove(id: ID) = $remove($select(id))

  def recentByUser(userId: String): Fu[List[Game]] = $find(
    $query(Query user userId) sort Query.sortCreated
  )

  def recentPovsByUser(user: User, nb: Int): Fu[List[Pov]] = $find(
    $query(Query user user) sort Query.sortCreated, nb
  ) map { _.flatMap(g => Pov(g, user)) }

  def gamesForAssessment(userId: String, nb: Int): Fu[List[Game]] = $find(
    $query(Query.finished
      ++ Query.rated
      ++ Query.user(userId)
      ++ Query.analysed(true)
      ++ Query.turnsMoreThan(20)
      ++ Query.clock(true)) sort ($sort asc F.createdAt), nb
  )

  def unrate(gameId: String) =
    $update($select(gameId), BSONDocument("$unset" -> BSONDocument(
      F.rated -> true,
      s"${F.whitePlayer}.${Player.BSONFields.ratingDiff}" -> true,
      s"${F.blackPlayer}.${Player.BSONFields.ratingDiff}" -> true
    )))

  def goBerserk(pov: Pov): Funit =
    $update($select(pov.gameId), BSONDocument("$set" -> BSONDocument(
      s"${pov.color.fold(F.whitePlayer, F.blackPlayer)}.${Player.BSONFields.berserk}" -> true
    )))

  def save(progress: Progress): Funit =
    GameDiff(progress.origin, progress.game) match {
      case (Nil, Nil) => funit
      case (sets, unsets) => gameTube.coll.update(
        $select(progress.origin.id),
        nonEmptyMod("$set", BSONDocument(sets)) ++ nonEmptyMod("$unset", BSONDocument(unsets))
      ).void
    }

  private def nonEmptyMod(mod: String, doc: BSONDocument) =
    if (doc.isEmpty) BSONDocument() else BSONDocument(mod -> doc)

  def setRatingDiffs(id: ID, white: Int, black: Int) =
    $update($select(id), BSONDocument("$set" -> BSONDocument(
      s"${F.whitePlayer}.${Player.BSONFields.ratingDiff}" -> BSONInteger(white),
      s"${F.blackPlayer}.${Player.BSONFields.ratingDiff}" -> BSONInteger(black))))

  // used by RatingFest
  def setRatingAndDiffs(id: ID, white: (Int, Int), black: (Int, Int)) =
    $update($select(id), BSONDocument("$set" -> BSONDocument(
      s"${F.whitePlayer}.${Player.BSONFields.rating}" -> BSONInteger(white._1),
      s"${F.blackPlayer}.${Player.BSONFields.rating}" -> BSONInteger(black._1),
      s"${F.whitePlayer}.${Player.BSONFields.ratingDiff}" -> BSONInteger(white._2),
      s"${F.blackPlayer}.${Player.BSONFields.ratingDiff}" -> BSONInteger(black._2))))

  def setUsers(id: ID, white: Option[Player.UserInfo], black: Option[Player.UserInfo]) =
    (white.isDefined || black.isDefined) ?? {
      $update($select(id), BSONDocument("$set" -> BSONDocument(
        s"${F.whitePlayer}.${Player.BSONFields.rating}" -> white.map(_.rating).map(BSONInteger.apply),
        s"${F.blackPlayer}.${Player.BSONFields.rating}" -> black.map(_.rating).map(BSONInteger.apply),
        s"${F.whitePlayer}.${Player.BSONFields.provisional}" -> white.map(_.provisional).filter(identity),
        s"${F.blackPlayer}.${Player.BSONFields.provisional}" -> black.map(_.provisional).filter(identity),
        F.playerUids -> lila.db.BSON.writer.listO(List(~white.map(_.id), ~black.map(_.id))),
        F.playingUids -> List(white.map(_.id), black.map(_.id)).flatten.distinct
      )))
    }

  def urgentGames(user: User): Fu[List[Pov]] =
    $find(Query nowPlaying user.id, 300) map { games =>
      val povs = games flatMap { Pov(_, user) }
      try {
        povs sortWith Pov.priority
      }
      catch {
        case e: IllegalArgumentException =>
          povs sortBy (-_.game.updatedAtOrCreatedAt.getSeconds)
      }
    }

  // gets most urgent game to play
  def mostUrgentGame(user: User): Fu[Option[Pov]] = urgentGames(user) map (_.headOption)

  // gets last recently played move game in progress
  def lastPlayedPlaying(user: User): Fu[Option[Pov]] =
    $find.one($query(Query recentlyPlaying user.id) sort Query.sortUpdatedNoIndex) map {
      _ flatMap { Pov(_, user) }
    }

  def lastPlayed(user: User): Fu[Option[Pov]] =
    $find($query(Query user user.id) sort ($sort desc F.createdAt), 20) map {
      _.sortBy(_.updatedAt).lastOption flatMap { Pov(_, user) }
    }

  def setTv(id: ID) {
    $update.fieldUnchecked(id, F.tvAt, $date(DateTime.now))
  }

  def onTv(nb: Int): Fu[List[Game]] = $find($query(Json.obj(F.tvAt -> $exists(true))) sort $sort.desc(F.tvAt), nb)

  def setAnalysed(id: ID) {
    $update.fieldUnchecked(id, F.analysed, true)
  }

  def isAnalysed(id: ID): Fu[Boolean] =
    $count.exists($select(id) ++ Query.analysed(true))

  def filterAnalysed(ids: Seq[String]): Fu[Set[String]] =
    $primitive(($select byIds ids) ++ Query.analysed(true), "_id")(_.asOpt[String]) map (_.toSet)

  def incBookmarks(id: ID, value: Int) =
    $update($select(id), $incBson(F.bookmarks -> value))

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

  def finish(
    id: ID,
    winnerColor: Option[Color],
    winnerId: Option[String],
    status: Status) = {
    val partialUnsets = BSONDocument(
      F.positionHashes -> true,
      F.playingUids -> true,
      ("p0." + Player.BSONFields.lastDrawOffer) -> true,
      ("p1." + Player.BSONFields.lastDrawOffer) -> true,
      ("p0." + Player.BSONFields.isOfferingDraw) -> true,
      ("p1." + Player.BSONFields.isOfferingDraw) -> true,
      ("p0." + Player.BSONFields.proposeTakebackAt) -> true,
      ("p1." + Player.BSONFields.proposeTakebackAt) -> true)
    // keep the checkAt field when game is aborted,
    // so it gets deleted in 24h
    val unsets =
      if (status >= Status.Mate) partialUnsets ++ BSONDocument(F.checkAt -> true)
      else partialUnsets
    $update(
      $select(id),
      nonEmptyMod("$set", BSONDocument(
        F.winnerId -> winnerId,
        F.winnerColor -> winnerColor.map(_.white)
      )) ++ BSONDocument("$unset" -> unsets)
    )
  }

  def findRandomStandardCheckmate(distribution: Int): Fu[Option[Game]] = $find.one(
    Query.mate ++ Json.obj("v" -> $exists(false)),
    _ sort Query.sortCreated skip (Random nextInt distribution)
  )

  def insertDenormalized(g: Game, ratedCheck: Boolean = true): Funit = {
    val g2 = if (ratedCheck && g.rated && g.userIds.distinct.size != 2)
      g.copy(mode = chess.Mode.Casual)
    else g
    val userIds = g2.userIds.distinct
    val fen = (!g2.variant.standardInitialPosition)
      .option(Forsyth >> g2.toChess)
      .filter(Forsyth.initial !=)
    val bson = (gameTube.handler write g2) ++ BSONDocument(
      F.initialFen -> fen,
      F.checkAt -> (!g2.isPgnImport).option(DateTime.now.plusHours(g2.hasClock.fold(1, 24))),
      F.playingUids -> (g2.started && userIds.nonEmpty).option(userIds)
    )
    $insert bson bson
  }

  def removeChallengesOf(userId: String) =
    $remove(Query.created ++ Query.friend ++ Query.user(userId))

  def setCheckAt(g: Game, at: DateTime) =
    $update($select(g.id), BSONDocument("$set" -> BSONDocument(F.checkAt -> at)))

  def unsetCheckAt(g: Game) =
    $update($select(g.id), BSONDocument("$unset" -> BSONDocument(F.checkAt -> true)))

  // used to make a compound sparse index
  def setImportCreatedAt(g: Game) =
    $update($select(g.id), BSONDocument(
      "$set" -> BSONDocument("pgni.ca" -> g.createdAt)
    ))

  def saveNext(game: Game, nextId: ID): Funit = $update(
    $select(game.id),
    $set(F.next -> nextId) ++
      $unset("p0." + Player.BSONFields.isOfferingRematch, "p1." + Player.BSONFields.isOfferingRematch)
  )

  def initialFen(gameId: ID): Fu[Option[String]] =
    $primitive.one($select(gameId), F.initialFen)(_.asOpt[String]) flatMap {
      case None => fuccess(none)
      case Some(fen) => Forsyth fixCastles fen match {
        case None                        => $update($select(gameId), $unset(F.initialFen)) inject none
        case Some(fixed) if fen == fixed => fuccess(fixed.some)
        case Some(fixed)                 => $update.field(gameId, F.initialFen, fixed) inject fixed.some
      }
    }

  def initialFen(game: Game): Fu[Option[String]] =
    if (game.imported || !game.variant.standardInitialPosition) initialFen(game.id)
    else fuccess(none)

  def featuredCandidates: Fu[List[Game]] = $find(
    Query.playable ++ Query.clock(true) ++ Json.obj(
      F.createdAt -> $gt($date(DateTime.now minusMinutes 5)),
      F.updatedAt -> $gt($date(DateTime.now minusSeconds 30))
    ) ++ $or(Seq(
        Json.obj(s"${F.whitePlayer}.${Player.BSONFields.rating}" -> $gt(1300)),
        Json.obj(s"${F.blackPlayer}.${Player.BSONFields.rating}" -> $gt(1300))
      ))
  )

  def count(query: Query.type => JsObject): Fu[Int] = $count(query(Query))

  def nbPerDay(days: Int): Fu[List[Int]] =
    ((days to 1 by -1).toList map { day =>
      val from = DateTime.now.withTimeAtStartOfDay minusDays day
      val to = from plusDays 1
      $count(Json.obj(F.createdAt -> ($gte($date(from)) ++ $lt($date(to)))))
    }).sequenceFu

  def bestOpponents(userId: String, limit: Int): Fu[List[(String, Int)]] = {
    val col = gameTube.coll
    import col.BatchCommands.AggregationFramework, AggregationFramework.{
      Descending,
      GroupField,
      Limit,
      Match,
      Sort,
      SumValue,
      Unwind
    }

    col.aggregate(Match(BSONDocument(F.playerUids -> userId)), List(
      Match(BSONDocument(F.playerUids -> BSONDocument("$size" -> 2))),
      Sort(Descending(F.createdAt)),
      Limit(1000), // only look in the last 1000 games
      Unwind(F.playerUids),
      Match(BSONDocument(F.playerUids -> BSONDocument("$ne" -> userId))),
      GroupField(F.playerUids)("gs" -> SumValue(1)),
      Sort(Descending("gs")),
      Limit(limit))).map(_.documents.map { obj =>
      toJSON(obj).asOpt[JsObject] flatMap { o =>
        o str "_id" flatMap { id =>
          o int "gs" map { id -> _ }
        }
      }
    }.flatten)
  }

  def random: Fu[Option[Game]] = $find.one(
    $query.all sort Query.sortCreated skip (Random nextInt 1000))

  def random(nb: Int): Fu[List[Game]] = $find(
    $query.all sort Query.sortCreated skip (Random nextInt 1000), nb)

  def findMirror(game: Game): Fu[Option[Game]] = $find.one($query(
    BSONDocument(
      F.id -> BSONDocument("$ne" -> game.id),
      F.playerUids -> BSONDocument("$in" -> game.userIds),
      F.status -> Status.Started.id,
      F.createdAt -> BSONDocument("$gt" -> (DateTime.now minusMinutes 15)),
      F.updatedAt -> BSONDocument("$gt" -> (DateTime.now minusMinutes 5)),
      "$or" -> BSONArray(
        BSONDocument(s"${F.whitePlayer}.ai" -> BSONDocument("$exists" -> true)),
        BSONDocument(s"${F.blackPlayer}.ai" -> BSONDocument("$exists" -> true))
      ),
      F.binaryPieces -> game.binaryPieces
    )
  ))

  def findPgnImport(pgn: String): Fu[Option[Game]] =
    gameTube.coll.find(
      BSONDocument(s"${F.pgnImport}.h" -> PgnImport.hash(pgn))
    ).one[Game]

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
      .cursor[BSONDocument]()
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

  def countRecentGamesBetween(u1: String, u2: String, since: DateTime): Fu[Int] = {
    $count(Json.obj(
      F.playerUids -> Json.obj("$all" -> List(u1, u2)),
      F.createdAt -> Json.obj("$gt" -> $date(since))
    ))
  }

  def getUserIds(id: ID): Fu[List[String]] =
    gameTube.coll.find(
      $select(id), BSONDocument(
        F.id -> false,
        F.playerUids -> true
      )
    ).one[BSONDocument] map { ~_.flatMap(_.getAs[List[String]](F.playerUids)) }

  def activePlayersSince(since: DateTime, max: Int): Fu[List[UidNb]] = {
    val col = gameTube.coll
    import col.BatchCommands.AggregationFramework, AggregationFramework.{
      Descending,
      GroupField,
      Limit,
      Match,
      Sort,
      SumValue,
      Unwind
    }

    col.aggregate(Match(BSONDocument(
      F.createdAt -> BSONDocument("$gt" -> since),
      F.status -> BSONDocument("$gte" -> chess.Status.Mate.id),
      s"${F.playerUids}.0" -> BSONDocument("$exists" -> true)
    )), List(Unwind(F.playerUids),
      Match(BSONDocument(
        F.playerUids -> BSONDocument("$ne" -> "")
      )),
      GroupField(F.playerUids)("nb" -> SumValue(1)),
      Sort(Descending("nb")),
      Limit(max))).map(_.documents.map { obj =>
      toJSON(obj).asOpt[JsObject] flatMap { o =>
        o int "nb" map { nb =>
          UidNb(~(o str "_id"), nb)
        }
      }
    }.flatten)
  }

  private def extractPgnMoves(doc: BSONDocument) =
    doc.getAs[BSONBinary](F.binaryPgn) map { bin =>
      BinaryFormat.pgn read { ByteArray.ByteArrayBSONHandler read bin }
    }
}
