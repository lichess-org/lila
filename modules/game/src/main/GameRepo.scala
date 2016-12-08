package lila.game

import scala.util.Random

import chess.format.{ Forsyth, FEN }
import chess.{ Color, Status }
import org.joda.time.DateTime
import reactivemongo.api.commands.GetLastError
import reactivemongo.api.{ CursorProducer, ReadPreference }
import reactivemongo.bson.BSONBinary

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.ByteArray
import lila.db.dsl._
import lila.user.{ User, UidNb }

object GameRepo {

  // dirty
  val coll = Env.current.gameColl

  type ID = String

  import BSONHandlers._
  import Game.{ BSONFields => F }

  def game(gameId: ID): Fu[Option[Game]] = coll.byId[Game](gameId)

  def gamesFromPrimary(gameIds: Seq[ID]): Fu[List[Game]] = coll.byOrderedIds[Game](gameIds)(_.id)

  def gamesFromSecondary(gameIds: Seq[ID]): Fu[List[Game]] =
    coll.byOrderedIds[Game](gameIds, readPreference = ReadPreference.secondaryPreferred)(_.id)

  def gameOptions(gameIds: Seq[ID]): Fu[Seq[Option[Game]]] =
    coll.optionsByOrderedIds[Game](gameIds)(_.id)

  def finished(gameId: ID): Fu[Option[Game]] =
    coll.uno[Game]($id(gameId) ++ Query.finished)

  def player(gameId: ID, color: Color): Fu[Option[Player]] =
    game(gameId) map2 { (game: Game) => game player color }

  def player(gameId: ID, playerId: ID): Fu[Option[Player]] =
    game(gameId) map { gameOption =>
      gameOption flatMap { _ player playerId }
    }

  def player(playerRef: PlayerRef): Fu[Option[Player]] =
    player(playerRef.gameId, playerRef.playerId)

  def pov(gameId: ID, color: Color): Fu[Option[Pov]] =
    game(gameId) map2 { (game: Game) => Pov(game, game player color) }

  def pov(gameId: ID, color: String): Fu[Option[Pov]] =
    Color(color) ?? (pov(gameId, _))

  def pov(playerRef: PlayerRef): Fu[Option[Pov]] =
    coll.byId[Game](playerRef.gameId) map { gameOption =>
      gameOption flatMap { game =>
        game player playerRef.playerId map { Pov(game, _) }
      }
    }

  def pov(fullId: ID): Fu[Option[Pov]] = pov(PlayerRef(fullId))

  def pov(ref: PovRef): Fu[Option[Pov]] = pov(ref.gameId, ref.color)

  def remove(id: ID) = coll.remove($id(id)).void

  def userPovsByGameIds(gameIds: List[String], user: User): Fu[List[Pov]] =
    coll.byOrderedIds[Game](gameIds)(_.id) map { _.flatMap(g => Pov(g, user)) }

  def recentPovsByUser(user: User, nb: Int): Fu[List[Pov]] =
    coll.find(Query user user).sort(Query.sortCreated).cursor[Game]().gather[List](nb)
      .map { _.flatMap(g => Pov(g, user)) }

  def gamesForAssessment(userId: String, nb: Int): Fu[List[Game]] = coll.find(
    Query.finished
      ++ Query.rated
      ++ Query.user(userId)
      ++ Query.analysed(true)
      ++ Query.turnsMoreThan(20)
      ++ Query.clock(true))
    .sort($sort asc F.createdAt)
    .cursor[Game](ReadPreference.secondaryPreferred)
    .gather[List](nb)

  def cursor(
    selector: Bdoc,
    readPreference: ReadPreference = ReadPreference.secondaryPreferred)(
    implicit cp: CursorProducer[Game]) =
    coll.find(selector).cursor[Game](readPreference)

  def sortedCursor(
    selector: Bdoc,
    sort: Bdoc,
    readPreference: ReadPreference = ReadPreference.secondaryPreferred)(
    implicit cp: CursorProducer[Game]) =
    coll.find(selector).sort(sort).cursor[Game](readPreference)

  def unrate(gameId: String) =
    coll.update($id(gameId), $doc("$unset" -> $doc(
      F.rated -> true,
      s"${F.whitePlayer}.${Player.BSONFields.ratingDiff}" -> true,
      s"${F.blackPlayer}.${Player.BSONFields.ratingDiff}" -> true
    )))

  def goBerserk(pov: Pov): Funit =
    coll.update($id(pov.gameId), $set(
      s"${pov.color.fold(F.whitePlayer, F.blackPlayer)}.${Player.BSONFields.berserk}" -> true
    )).void

  def save(progress: Progress): Funit =
    GameDiff(progress.origin, progress.game) match {
      case (Nil, Nil) => funit
      case (sets, unsets) => coll.update(
        $id(progress.origin.id),
        nonEmptyMod("$set", $doc(sets)) ++ nonEmptyMod("$unset", $doc(unsets))
      ).void
    }

  private def nonEmptyMod(mod: String, doc: Bdoc) =
    if (doc.isEmpty) $empty else $doc(mod -> doc)

  def setRatingDiffs(id: ID, white: Int, black: Int) =
    coll.update($id(id), $set(
      s"${F.whitePlayer}.${Player.BSONFields.ratingDiff}" -> white,
      s"${F.blackPlayer}.${Player.BSONFields.ratingDiff}" -> black))

  // used by RatingFest
  def setRatingAndDiffs(id: ID, white: (Int, Int), black: (Int, Int)) =
    coll.update($id(id), $set(
      s"${F.whitePlayer}.${Player.BSONFields.rating}" -> white._1,
      s"${F.blackPlayer}.${Player.BSONFields.rating}" -> black._1,
      s"${F.whitePlayer}.${Player.BSONFields.ratingDiff}" -> white._2,
      s"${F.blackPlayer}.${Player.BSONFields.ratingDiff}" -> black._2))

  def urgentGames(user: User): Fu[List[Pov]] =
    coll.list[Game](Query nowPlaying user.id, Game.maxPlayingRealtime) map { games =>
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
    coll.find(Query recentlyPlaying user.id)
      .sort(Query.sortUpdatedNoIndex)
      .cursor[Game](readPreference = ReadPreference.secondaryPreferred)
      .uno
      .map { _ flatMap { Pov(_, user) } }

  def lastPlayed(user: User): Fu[Option[Pov]] =
    coll.find(Query user user.id)
      .sort($sort desc F.createdAt)
      .cursor[Game]()
      .gather[List](20).map {
        _.sortBy(_.updatedAt).lastOption flatMap { Pov(_, user) }
      }

  def lastFinishedRatedNotFromPosition(user: User): Fu[Option[Game]] = coll.find(
    Query.user(user.id) ++
      Query.rated ++
      Query.finished ++
      Query.turnsMoreThan(2) ++
      Query.notFromPosition
  ).sort(Query.sortAntiChronological).uno[Game]

  def setTv(id: ID) {
    coll.updateFieldUnchecked($id(id), F.tvAt, DateTime.now)
  }

  def onTv(nb: Int): Fu[List[Game]] = coll.find($doc(F.tvAt $exists true))
    .sort($sort desc F.tvAt)
    .cursor[Game]()
    .gather[List](nb)

  def setAnalysed(id: ID) {
    coll.updateFieldUnchecked($id(id), F.analysed, true)
  }
  def setUnanalysed(id: ID) {
    coll.updateFieldUnchecked($id(id), F.analysed, false)
  }

  def isAnalysed(id: ID): Fu[Boolean] =
    coll.exists($id(id) ++ Query.analysed(true))

  def filterAnalysed(ids: Seq[String]): Fu[Set[String]] =
    coll.distinct[String, Set]("_id", ($inIds(ids) ++ $doc(
      F.analysed -> true
    )).some)

  def exists(id: String) = coll.exists($id(id))

  def incBookmarks(id: ID, value: Int) =
    coll.update($id(id), $inc(F.bookmarks -> value)).void

  def setHoldAlert(pov: Pov, mean: Int, sd: Int, ply: Option[Int] = None) = {
    import Player.holdAlertBSONHandler
    coll.update(
      $id(pov.gameId),
      $set(
        s"p${pov.color.fold(0, 1)}.${Player.BSONFields.holdAlert}" ->
          Player.HoldAlert(ply = ply | pov.game.turns, mean = mean, sd = sd)
      )
    ).void
  }
  def setBorderAlert(pov: Pov) = setHoldAlert(pov, 0, 0, 20.some)

  def finish(
    id: ID,
    winnerColor: Option[Color],
    winnerId: Option[String],
    status: Status) = {
    val partialUnsets = $doc(
      F.positionHashes -> true,
      F.playingUids -> true,
      F.unmovedRooks -> true,
      ("p0." + Player.BSONFields.lastDrawOffer) -> true,
      ("p1." + Player.BSONFields.lastDrawOffer) -> true,
      ("p0." + Player.BSONFields.isOfferingDraw) -> true,
      ("p1." + Player.BSONFields.isOfferingDraw) -> true,
      ("p0." + Player.BSONFields.proposeTakebackAt) -> true,
      ("p1." + Player.BSONFields.proposeTakebackAt) -> true)
    // keep the checkAt field when game is aborted,
    // so it gets deleted in 24h
    val unsets =
      if (status >= Status.Mate) partialUnsets ++ $doc(F.checkAt -> true)
      else partialUnsets
    coll.update(
      $id(id),
      nonEmptyMod("$set", $doc(
        F.winnerId -> winnerId,
        F.winnerColor -> winnerColor.map(_.white)
      )) ++ $doc("$unset" -> unsets)
    )
  }

  def findRandomStandardCheckmate(distribution: Int): Fu[Option[Game]] = coll.find(
    Query.mate ++ Query.variantStandard
  ).sort(Query.sortCreated)
    .skip(Random nextInt distribution)
    .uno[Game]

  def findRandomFinished(distribution: Int): Fu[Option[Game]] = coll.find(
    Query.finished ++ Query.variantStandard ++ Query.turnsMoreThan(20) ++ Query.rated
  ).sort(Query.sortCreated)
    .skip(Random nextInt distribution)
    .uno[Game]

  def randomFinished(distribution: Int): Fu[Option[Game]] = coll.find(
    Query.finished ++ Query.rated ++
      Query.variantStandard ++ Query.bothRatingsGreaterThan(1600)
  ).sort(Query.sortCreated)
    .skip(Random nextInt distribution)
    .cursor[Game](ReadPreference.secondary)
    .uno

  def insertDenormalized(g: Game, ratedCheck: Boolean = true, initialFen: Option[chess.format.FEN] = None): Funit = {
    val g2 = if (ratedCheck && g.rated && g.userIds.distinct.size != 2)
      g.copy(mode = chess.Mode.Casual)
    else g
    val userIds = g2.userIds.distinct
    val fen = initialFen.map(_.value) orElse {
      (!g2.variant.standardInitialPosition)
        .option(Forsyth >> g2.toChess)
        .filter(Forsyth.initial !=)
    }
    val bson = (gameBSONHandler write g2) ++ $doc(
      F.initialFen -> fen,
      F.checkAt -> (!g2.isPgnImport).option(DateTime.now plusHours g2.hasClock.fold(1, 10 * 24)),
      F.playingUids -> (g2.started && userIds.nonEmpty).option(userIds)
    )
    coll insert bson void
  } >>- {
    lila.mon.game.create.variant(g.variant.key)()
    lila.mon.game.create.source(g.source.fold("unknown")(_.name))()
    lila.mon.game.create.speed(g.speed.name)()
    lila.mon.game.create.mode(g.mode.name)()
  }

  def removeRecentChallengesOf(userId: String) =
    coll.remove(Query.created ++ Query.friend ++ Query.user(userId) ++
      Query.createdSince(DateTime.now minusHours 1))

  def setCheckAt(g: Game, at: DateTime) =
    coll.update($id(g.id), $doc("$set" -> $doc(F.checkAt -> at)))

  def unsetCheckAt(g: Game) =
    coll.update($id(g.id), $doc("$unset" -> $doc(F.checkAt -> true)))

  def unsetPlayingUids(g: Game): Unit =
    coll.update($id(g.id), $unset(F.playingUids), writeConcern = GetLastError.Unacknowledged)

  // used to make a compound sparse index
  def setImportCreatedAt(g: Game) =
    coll.update($id(g.id), $set("pgni.ca" -> g.createdAt)).void

  def saveNext(game: Game, nextId: ID): Funit = coll.update(
    $id(game.id),
    $set(F.next -> nextId) ++
      $unset(
        "p0." + Player.BSONFields.isOfferingRematch,
        "p1." + Player.BSONFields.isOfferingRematch)
  ).void

  def initialFen(gameId: ID): Fu[Option[String]] =
    coll.primitiveOne[String]($id(gameId), F.initialFen)

  def initialFen(game: Game): Fu[Option[String]] =
    if (game.imported || !game.variant.standardInitialPosition) initialFen(game.id) map {
      case None if game.variant == chess.variant.Chess960 => Forsyth.initial.some
      case fen => fen
    }
    else fuccess(none)

  def gameWithInitialFen(gameId: ID): Fu[Option[(Game, Option[FEN])]] = game(gameId) flatMap {
    _ ?? { game =>
      initialFen(game) map { fen =>
        (game -> fen.map(FEN.apply)).some
      }
    }
  }

  def withInitialFen(game: Game): Fu[Game.WithInitialFen] =
    initialFen(game) map { fen =>
      Game.WithInitialFen(game, fen.map(FEN.apply))
    }

  def withInitialFens(games: List[Game]): Fu[List[(Game, Option[FEN])]] = games.map { game =>
    initialFen(game) map { fen =>
      game -> fen.map(FEN.apply)
    }
  }.sequenceFu

  def featuredCandidates: Fu[List[Game]] = coll.list[Game](
    Query.playable ++ Query.clock(true) ++ $doc(
      F.createdAt $gt (DateTime.now minusMinutes 5),
      F.updatedAt $gt (DateTime.now minusSeconds 40)
    ) ++ $or(
        s"${F.whitePlayer}.${Player.BSONFields.rating}" $gt 1200,
        s"${F.blackPlayer}.${Player.BSONFields.rating}" $gt 1200
      )
  )

  def count(query: Query.type => Bdoc): Fu[Int] = coll countSel query(Query)

  def nbPerDay(days: Int): Fu[List[Int]] =
    ((days to 1 by -1).toList map { day =>
      val from = DateTime.now.withTimeAtStartOfDay minusDays day
      val to = from plusDays 1
      coll.countSel($doc(F.createdAt -> ($gte(from) ++ $lt(to))))
    }).sequenceFu

  // #TODO expensive stuff, run on DB replica
  // Can't be done on reactivemongo 0.11.9 :(
  def bestOpponents(userId: String, limit: Int): Fu[List[(String, Int)]] = {
    import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
    coll.aggregate(Match($doc(F.playerUids -> userId)), List(
      Match($doc(F.playerUids -> $doc("$size" -> 2))),
      Sort(Descending(F.createdAt)),
      Limit(1000), // only look in the last 1000 games
      Project($doc(
        F.playerUids -> true,
        F.id -> false)),
      UnwindField(F.playerUids),
      Match($doc(F.playerUids -> $doc("$ne" -> userId))),
      GroupField(F.playerUids)("gs" -> SumValue(1)),
      Sort(Descending("gs")),
      Limit(limit))).map(_.firstBatch.flatMap { obj =>
      obj.getAs[String]("_id") flatMap { id =>
        obj.getAs[Int]("gs") map { id -> _ }
      }
    })
  }

  def random: Fu[Option[Game]] = coll.find($empty)
    .sort(Query.sortCreated)
    .skip(Random nextInt 1000)
    .uno[Game]

  def findMirror(game: Game): Fu[Option[Game]] = coll.uno[Game]($doc(
    F.id -> $doc("$ne" -> game.id),
    F.playerUids $in game.userIds,
    F.status -> Status.Started.id,
    F.createdAt $gt (DateTime.now minusMinutes 15),
    F.updatedAt $gt (DateTime.now minusMinutes 5),
    "$or" -> $arr(
      $doc(s"${F.whitePlayer}.ai" -> $doc("$exists" -> true)),
      $doc(s"${F.blackPlayer}.ai" -> $doc("$exists" -> true))
    ),
    F.binaryPieces -> game.binaryPieces
  ))

  def findPgnImport(pgn: String): Fu[Option[Game]] = coll.uno[Game](
    $doc(s"${F.pgnImport}.h" -> PgnImport.hash(pgn))
  )

  def getPgn(id: ID): Fu[PgnMoves] = getOptionPgn(id) map (~_)

  def getNonEmptyPgn(id: ID): Fu[Option[PgnMoves]] =
    getOptionPgn(id) map (_ filter (_.nonEmpty))

  def getOptionPgn(id: ID): Fu[Option[PgnMoves]] =
    coll.find(
      $id(id), $doc(
        F.id -> false,
        F.binaryPgn -> true
      )
    ).uno[Bdoc] map { _ flatMap extractPgnMoves }

  def lastGameBetween(u1: String, u2: String, since: DateTime): Fu[Option[Game]] =
    coll.uno[Game]($doc(
      F.playerUids $all List(u1, u2),
      F.createdAt $gt since
    ))

  def getUserIds(id: ID): Fu[List[String]] =
    coll.find(
      $id(id), $doc(
        F.id -> false,
        F.playerUids -> true
      )
    ).uno[Bdoc] map { ~_.flatMap(_.getAs[List[String]](F.playerUids)) }

  // #TODO this breaks it all since reactivemongo > 0.11.9
  def activePlayersSinceNOPENOPENOPE(since: DateTime, max: Int): Fu[List[UidNb]] = {
    import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework, AggregationFramework.{
      Descending,
      GroupField,
      Limit,
      Match,
      Sort,
      SumValue,
      UnwindField
    }

    coll.aggregate(Match($doc(
      F.createdAt $gt since,
      F.status $gte chess.Status.Mate.id,
      s"${F.playerUids}.0" $exists true
    )), List(
      UnwindField(F.playerUids),
      Match($doc(
        F.playerUids -> $doc("$ne" -> "")
      )),
      GroupField(F.playerUids)("nb" -> SumValue(1)),
      Sort(Descending("nb")),
      Limit(max))).map(_.firstBatch.flatMap { obj =>
      obj.getAs[Int]("nb") map { nb =>
        UidNb(~obj.getAs[String]("_id"), nb)
      }
    })
  }

  private def extractPgnMoves(doc: Bdoc) =
    doc.getAs[BSONBinary](F.binaryPgn) map { bin =>
      BinaryFormat.pgn read { ByteArray.ByteArrayBSONHandler read bin }
    }
}
