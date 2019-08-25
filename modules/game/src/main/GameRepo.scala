package lidraughts.game

import scala.util.Random

import draughts.format.{ Forsyth, FEN }
import draughts.{ Color, Status }
import org.joda.time.DateTime
import reactivemongo.api.commands.GetLastError
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.{ CursorProducer, Cursor, ReadPreference }
import reactivemongo.bson.BSONDocument

import lidraughts.db.BSON.BSONJodaDateTimeHandler
import lidraughts.db.dsl._
import lidraughts.db.{ ByteArray, isDuplicateKey }
import lidraughts.user.User

object GameRepo {

  // dirty
  val coll = Env.current.gameColl

  import BSONHandlers._
  import Game.{ ID, BSONFields => F }
  import Player.holdAlertBSONHandler

  def game(gameId: ID): Fu[Option[Game]] = coll.byId[Game](gameId)

  def gamesFromPrimary(gameIds: Seq[ID]): Fu[List[Game]] = coll.byOrderedIds[Game, ID](gameIds)(_.id)

  def gamesFromSecondary(gameIds: Seq[ID]): Fu[List[Game]] =
    coll.byOrderedIds[Game, ID](gameIds, readPreference = ReadPreference.secondaryPreferred)(_.id)

  def gameOptionsFromSecondary(gameIds: Seq[ID]): Fu[List[Option[Game]]] =
    coll.optionsByOrderedIds[Game, ID](gameIds, ReadPreference.secondaryPreferred)(_.id)

  object light {

    def game(gameId: ID): Fu[Option[LightGame]] = coll.byId[LightGame](gameId, LightGame.projection)

    def pov(gameId: ID, color: Color): Fu[Option[LightPov]] =
      game(gameId) map2 { (game: LightGame) => LightPov(game, game player color) }

    def pov(ref: PovRef): Fu[Option[LightPov]] = pov(ref.gameId, ref.color)

    def gamesFromPrimary(gameIds: Seq[ID]): Fu[List[LightGame]] =
      coll.byOrderedIds[LightGame, ID](gameIds, projection = LightGame.projection.some)(_.id)

    def gamesFromSecondary(gameIds: Seq[ID]): Fu[List[LightGame]] =
      coll.byOrderedIds[LightGame, ID](gameIds, projection = LightGame.projection.some, readPreference = ReadPreference.secondaryPreferred)(_.id)
  }

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

  def pov(gameId: ID, user: User): Fu[Option[Pov]] =
    coll.byId[Game](gameId) map {
      _ flatMap { Pov(_, user) }
    }

  def pov(gameId: ID, color: Color): Fu[Option[Pov]] =
    game(gameId) map2 { (game: Game) => Pov(game, game player color) }

  def pov(gameId: ID, color: String): Fu[Option[Pov]] =
    Color(color) ?? (pov(gameId, _))

  def pov(playerRef: PlayerRef): Fu[Option[Pov]] =
    game(playerRef.gameId) map { _ flatMap { _ playerIdPov playerRef.playerId } }

  def pov(fullId: ID): Fu[Option[Pov]] = pov(PlayerRef(fullId))

  def pov(ref: PovRef): Fu[Option[Pov]] = pov(ref.gameId, ref.color)

  def remove(id: ID) = coll.remove($id(id)).void

  def userPovsByGameIds(gameIds: List[String], user: User, readPreference: ReadPreference = ReadPreference.secondaryPreferred): Fu[List[Pov]] =
    coll.byOrderedIds[Game, ID](gameIds)(_.id) map { _.flatMap(g => Pov(g, user)) }

  def recentPovsByUserFromSecondary(user: User, nb: Int): Fu[List[Pov]] =
    coll.find(Query user user)
      .sort(Query.sortCreated)
      .cursor[Game](ReadPreference.secondaryPreferred)
      .gather[List](nb)
      .map { _.flatMap(g => Pov(g, user)) }

  def gamesForAssessment(userId: String, nb: Int): Fu[List[Game]] = coll.find(
    Query.finished
      ++ Query.rated
      ++ Query.user(userId)
      ++ Query.analysed(true)
      ++ Query.turnsGt(20)
      ++ Query.clockHistory(true)
  )
    .sort($sort asc F.createdAt)
    .list[Game](nb, ReadPreference.secondaryPreferred)

  def extraGamesForIrwin(userId: String, nb: Int): Fu[List[Game]] = coll.find(
    Query.finished
      ++ Query.rated
      ++ Query.user(userId)
      ++ Query.turnsGt(22)
      ++ Query.variantStandard
      ++ Query.clock(true)
  )
    .sort($sort asc F.createdAt)
    .list[Game](nb, ReadPreference.secondaryPreferred)

  def cursor(
    selector: Bdoc,
    readPreference: ReadPreference = ReadPreference.secondaryPreferred
  )(implicit cp: CursorProducer[Game]) =
    coll.find(selector).cursor[Game](readPreference)

  def sortedCursor(
    selector: Bdoc,
    sort: Bdoc,
    batchSize: Int = 0,
    readPreference: ReadPreference = ReadPreference.secondaryPreferred
  )(implicit cp: CursorProducer[Game]): cp.ProducedCursor = {
    val query = coll.find(selector).sort(sort)
    query.copy(options = query.options.batchSize(batchSize)).cursor[Game](readPreference)
  }

  def goBerserk(pov: Pov): Funit =
    coll.update($id(pov.gameId), $set(
      s"${pov.color.fold(F.whitePlayer, F.blackPlayer)}.${Player.BSONFields.berserk}" -> true
    )).void

  def update(progress: Progress): Funit =
    saveDiff(progress.origin, GameDiff(progress.origin, progress.game))

  private def saveDiff(origin: Game, diff: GameDiff.Diff): Funit = diff match {
    case (Nil, Nil) => funit
    case (sets, unsets) => coll.update(
      $id(origin.id),
      nonEmptyMod("$set", $doc(sets)) ++ nonEmptyMod("$unset", $doc(unsets))
    ).void
  }

  private def nonEmptyMod(mod: String, doc: Bdoc) =
    if (doc.isEmpty) $empty else $doc(mod -> doc)

  def setRatingDiffs(id: ID, diffs: RatingDiffs) =
    coll.update($id(id), $set(
      s"${F.whitePlayer}.${Player.BSONFields.ratingDiff}" -> diffs.white,
      s"${F.blackPlayer}.${Player.BSONFields.ratingDiff}" -> diffs.black
    ))

  // Use Env.round.proxy.urgentGames to get in-heap states!
  def urgentPovsUnsorted(user: User): Fu[List[Pov]] =
    coll.list[Game](Query nowPlaying user.id, Game.maxPlayingRealtime) map {
      _ flatMap { Pov(_, user) }
    }

  def playingRealtimeNoAi(user: User, nb: Int): Fu[List[Game.ID]] =
    coll.distinct[Game.ID, List](F.id, Some(Query.nowPlaying(user.id) ++ Query.noAi ++ Query.clock(true)))

  // gets last recently played move game in progress
  def lastPlayedPlaying(user: User): Fu[Option[Pov]] =
    lastPlayedPlaying(user.id).map { _ flatMap { Pov(_, user) } }

  def lastPlayedPlaying(userId: User.ID): Fu[Option[Game]] =
    coll.find(Query recentlyPlaying userId)
      .sort(Query.sortMovedAtNoIndex)
      .cursor[Game](readPreference = ReadPreference.secondaryPreferred)
      .uno

  def lastPlayedPlayingId(userId: User.ID): Fu[Option[Game.ID]] =
    coll.find(Query recentlyPlaying userId, $id(true))
      .sort(Query.sortMovedAtNoIndex)
      .uno[Bdoc](readPreference = ReadPreference.secondaryPreferred)
      .map { _.flatMap(_.getAs[Game.ID](F.id)) }

  def allPlaying(userId: User.ID): Fu[List[Pov]] =
    coll.find(Query nowPlaying userId).list[Game]()
      .map { _ flatMap { Pov.ofUserId(_, userId) } }

  def lastPlayed(user: User): Fu[Option[Pov]] =
    coll.find(Query user user.id)
      .sort($sort desc F.createdAt)
      .list[Game](3).map {
        _.sortBy(_.movedAt).lastOption flatMap { Pov(_, user) }
      }

  def lastFinishedRatedNotFromPosition(user: User): Fu[Option[Game]] = coll.find(
    Query.user(user.id) ++
      Query.rated ++
      Query.finished ++
      Query.turnsGt(2) ++
      Query.notFromPosition
  ).sort(Query.sortAntiChronological).uno[Game]

  def setTimeOut(id: ID, seconds: Int) =
    coll.updateFieldUnchecked($id(id), F.timeOutUntil, DateTime.now plusSeconds seconds)

  def unsetTimeOut(id: ID) =
    coll.update($id(id), $unset(F.timeOutUntil), writeConcern = GetLastError.Unacknowledged)

  def setTv(id: ID) = coll.updateFieldUnchecked($id(id), F.tvAt, DateTime.now)

  def setAnalysed(id: ID): Unit = {
    coll.updateFieldUnchecked($id(id), F.analysed, true)
  }
  def setUnanalysed(id: ID): Unit = {
    coll.updateFieldUnchecked($id(id), F.analysed, false)
  }

  def isAnalysed(id: ID): Fu[Boolean] =
    coll.exists($id(id) ++ Query.analysed(true))

  def filterAnalysed(ids: Seq[ID]): Fu[Set[ID]] =
    coll.distinct[ID, Set]("_id", ($inIds(ids) ++ $doc(
      F.analysed -> true
    )).some)

  def exists(id: ID) = coll.exists($id(id))

  def tournamentId(id: ID): Fu[Option[String]] = coll.primitiveOne[String]($id(id), F.tournamentId)

  def incBookmarks(id: ID, value: Int) =
    coll.update($id(id), $inc(F.bookmarks -> value)).void

  def setHoldAlert(pov: Pov, alert: Player.HoldAlert) = coll.updateField(
    $id(pov.gameId), holdAlertField(pov.color), alert
  )

  def setBorderAlert(pov: Pov) = setHoldAlert(pov, Player.HoldAlert(0, 0, 20))

  def holdAlerts(game: Game): Fu[Player.HoldAlert.Map] =
    coll.uno[Bdoc](
      $doc(
        F.id -> game.id,
        $or(
          holdAlertField(draughts.White) $exists true,
          holdAlertField(draughts.Black) $exists true
        )
      ),
      $doc(
        F.id -> false,
        holdAlertField(draughts.White) -> true,
        holdAlertField(draughts.Black) -> true
      )
    ) map {
        _.fold(Player.HoldAlert.emptyMap) { doc =>
          def holdAlertOf(playerField: String) =
            doc.getAs[Bdoc](playerField).flatMap(_.getAs[Player.HoldAlert](Player.BSONFields.holdAlert))
          Color.Map(
            white = holdAlertOf("p0"),
            black = holdAlertOf("p1")
          )
        }
      }

  def hasHoldAlert(pov: Pov): Fu[Boolean] = coll.exists($doc(
    $id(pov.gameId),
    holdAlertField(pov.color) $exists true
  ))

  private def holdAlertField(color: Color) = s"p${color.fold(0, 1)}.${Player.BSONFields.holdAlert}"

  private val finishUnsets = $doc(
    F.playingUids -> true,
    F.unmovedRooks -> true,
    ("p0." + Player.BSONFields.lastDrawOffer) -> true,
    ("p1." + Player.BSONFields.lastDrawOffer) -> true,
    ("p0." + Player.BSONFields.isOfferingDraw) -> true,
    ("p1." + Player.BSONFields.isOfferingDraw) -> true,
    ("p0." + Player.BSONFields.proposeTakebackAt) -> true,
    ("p1." + Player.BSONFields.proposeTakebackAt) -> true
  )

  def finish(
    id: ID,
    winnerColor: Option[Color],
    winnerId: Option[String],
    status: Status,
    keepHashes: Boolean = false
  ) = coll.update(
    $id(id),
    nonEmptyMod("$set", $doc(
      F.winnerId -> winnerId,
      F.winnerColor -> winnerColor.map(_.white),
      F.status -> status
    )) ++ $doc(
      "$unset" -> finishUnsets.++ {
        // keep the checkAt field when game is aborted,
        // so it gets deleted in 24h
        (status >= Status.Mate) ?? $doc(F.checkAt -> true)
      }.++ {
        (!keepHashes) ?? $doc(F.positionHashes -> true)
      }
    )
  )

  def findRandomStandardCheckmate(distribution: Int): Fu[Option[Game]] = coll.find(
    Query.mate ++ Query.variantStandard
  ).sort(Query.sortCreated)
    .skip(Random nextInt distribution)
    .uno[Game]

  def findRandomFinished(distribution: Int): Fu[Option[Game]] = coll.find(
    Query.finished ++ Query.variantStandard ++ Query.turnsGt(20) ++ Query.rated
  ).sort(Query.sortCreated)
    .skip(Random nextInt distribution)
    .uno[Game]

  def findAnyRandomFinished(distribution: Int, minTurns: Int): Fu[Option[Game]] = coll.find(
    Query.finished ++ Query.variantStandard ++ Query.turnsGt(minTurns)
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

  def insertDenormalized(g: Game, ratedCheck: Boolean = true, initialFen: Option[draughts.format.FEN] = None): Funit = {
    val g2 = if (ratedCheck && g.rated && g.userIds.distinct.size != 2)
      g.copy(mode = draughts.Mode.Casual)
    else g
    val userIds = g2.userIds.distinct
    val fen = initialFen.map(_.value) orElse {
      (!g2.variant.standardInitialPosition)
        .option(Forsyth >> g2.draughts)
        .filter(Forsyth.initial !=)
    }
    val checkInHours =
      if (g2.isPdnImport) none
      else if (g2.hasClock) 1.some
      else if (g2.hasAi) (Game.aiAbandonedHours + 1).some
      else (24 * 10).some
    val bson = (gameBSONHandler write g2) ++ $doc(
      F.initialFen -> fen,
      F.checkAt -> checkInHours.map(DateTime.now.plusHours),
      F.playingUids -> (g2.started && userIds.nonEmpty).option(userIds)
    )
    coll insert bson addFailureEffect {
      case wr: WriteResult if isDuplicateKey(wr) => lidraughts.mon.game.idCollision()
    } void
  } >>- {
    lidraughts.mon.game.create.variant(g.variant.key)()
    lidraughts.mon.game.create.source(g.source.fold("unknown")(_.name))()
    lidraughts.mon.game.create.speed(g.speed.name)()
    lidraughts.mon.game.create.mode(g.mode.name)()
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
    coll.update($id(g.id), $set("pdni.ca" -> g.createdAt)).void

  def initialFen(gameId: ID): Fu[Option[FEN]] =
    coll.primitiveOne[FEN]($id(gameId), F.initialFen)

  def initialFen(game: Game): Fu[Option[FEN]] =
    if (game.imported || !game.variant.standardInitialPosition) initialFen(game.id)
    else fuccess(none)

  def gameWithInitialFen(gameId: ID): Fu[Option[(Game, Option[FEN])]] = game(gameId) flatMap {
    _ ?? { game =>
      initialFen(game) map { fen =>
        Option(game -> fen)
      }
    }
  }

  def withInitialFen(game: Game): Fu[Game.WithInitialFen] =
    initialFen(game) map { Game.WithInitialFen(game, _) }

  def withInitialFens(games: List[Game]): Fu[List[(Game, Option[FEN])]] = games.map { game =>
    initialFen(game) map { game -> _ }
  }.sequenceFu

  def count(query: Query.type => Bdoc): Fu[Int] = coll countSel query(Query)

  def nbPerDay(days: Int): Fu[List[Int]] =
    ((days to 1 by -1).toList map { day =>
      val from = DateTime.now.withTimeAtStartOfDay minusDays day
      val to = from plusDays 1
      coll.countSel($doc(F.createdAt -> ($gte(from) ++ $lt(to))))
    }).sequenceFu

  private[game] def bestOpponents(userId: String, limit: Int): Fu[List[(User.ID, Int)]] = {
    import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
    coll.aggregateList(
      Match($doc(F.playerUids -> userId)),
      List(
        Match($doc(F.playerUids -> $doc("$size" -> 2))),
        Sort(Descending(F.createdAt)),
        Limit(1000), // only look in the last 1000 games
        Project($doc(
          F.playerUids -> true,
          F.id -> false
        )),
        UnwindField(F.playerUids),
        Match($doc(F.playerUids -> $doc("$ne" -> userId))),
        GroupField(F.playerUids)("gs" -> SumValue(1)),
        Sort(Descending("gs")),
        Limit(limit)
      ),
      maxDocs = limit,
      ReadPreference.secondaryPreferred
    ).map(_.flatMap { obj =>
        obj.getAs[String]("_id") flatMap { id =>
          obj.getAs[Int]("gs") map { id -> _ }
        }
      })
  }

  def random: Fu[Option[Game]] = coll.find($empty)
    .sort(Query.sortCreated)
    .skip(Random nextInt 1000)
    .uno[Game]

  def findPdnImport(pdn: String): Fu[Option[Game]] = coll.uno[Game](
    $doc(s"${F.pdnImport}.h" -> PdnImport.hash(pdn))
  )

  def getOptionPdn(id: ID): Fu[Option[PdnMoves]] = game(id) map2 { (g: Game) => g.pdnMoves }

  def lastGameBetween(u1: String, u2: String, since: DateTime): Fu[Option[Game]] =
    coll.uno[Game]($doc(
      F.playerUids $all List(u1, u2),
      F.createdAt $gt since
    ))

  def lastGamesBetween(u1: User, u2: User, since: DateTime, nb: Int): Fu[List[Game]] =
    List(u1, u2).forall(_.count.game > 0) ??
      coll.find($doc(
        F.playerUids $all List(u1.id, u2.id),
        F.createdAt $gt since
      )).list[Game](nb, ReadPreference.secondaryPreferred)

  def getSourceAndUserIds(id: ID): Fu[(Option[Source], List[User.ID])] =
    coll.uno[Bdoc]($id(id), $doc(F.playerUids -> true, F.source -> true)) map {
      _.fold(none[Source] -> List.empty[User.ID]) { doc =>
        (doc.getAs[Int](F.source) flatMap Source.apply,
          ~doc.getAs[List[User.ID]](F.playerUids))
      }
    }

  def recentAnalysableGamesByUserId(userId: User.ID, nb: Int) =
    coll.find(
      Query.finished
        ++ Query.rated
        ++ Query.user(userId)
        ++ Query.turnsGt(20)
    ).sort(Query.sortCreated)
      .cursor[Game](ReadPreference.secondaryPreferred)
      .list(nb)
}
