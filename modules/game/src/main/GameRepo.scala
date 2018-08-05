package lila.game

import scala.util.Random

import chess.format.{ Forsyth, FEN }
import chess.{ Color, Status }
import org.joda.time.DateTime

import reactivemongo.api.{ CursorProducer, ReadPreference }
import reactivemongo.api.WriteConcern

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._
import lila.user.User

object GameRepo {

  // dirty
  val coll = Env.current.gameColl

  import BSONHandlers._
  import Game.{ ID, BSONFields => F }

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
    coll.find($id(gameId) ++ Query.finished).one[Game]

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
    game(playerRef.gameId) map { _ flatMap { _ playerIdPov playerRef.playerId } }

  def pov(fullId: ID): Fu[Option[Pov]] = pov(PlayerRef(fullId))

  def pov(ref: PovRef): Fu[Option[Pov]] = pov(ref.gameId, ref.color)

  def remove(id: ID): Funit = coll.delete.one($id(id)).void

  def userPovsByGameIds(gameIds: List[String], user: User, readPreference: ReadPreference = ReadPreference.secondaryPreferred): Fu[List[Pov]] =
    coll.byOrderedIds[Game, ID](gameIds)(_.id) map { _.flatMap(g => Pov(g, user)) }

  def recentPovsByUserFromSecondary(user: User, nb: Int): Fu[List[Pov]] =
    coll.find(Query user user).sort(Query.sortCreated)
      .cursor[Game](ReadPreference.secondaryPreferred).list(nb)
      .map { _.flatMap(g => Pov(g, user)) }

  def gamesForAssessment(userId: String, nb: Int): Fu[List[Game]] = coll.find(
    Query.finished
      ++ Query.rated
      ++ Query.user(userId)
      ++ Query.analysed(true)
      ++ Query.turnsGt(20)
      ++ Query.clockHistory(true)
  ).sort($sort asc F.createdAt)
    .cursor[Game](ReadPreference.secondaryPreferred).list(nb)

  def extraGamesForIrwin(userId: String, nb: Int): Fu[List[Game]] = coll.find(
    Query.finished
      ++ Query.rated
      ++ Query.user(userId)
      ++ Query.turnsGt(22)
      ++ Query.variantStandard
      ++ Query.clock(true)
  ).sort($sort asc F.createdAt)
    .cursor[Game](ReadPreference.secondaryPreferred).list(nb)

  def cursor(
    selector: Bdoc,
    readPreference: ReadPreference = ReadPreference.secondaryPreferred
  )(
    implicit
    cursorProducer: CursorProducer[Game]
  ): cursorProducer.ProducedCursor =
    coll.find(selector).cursor[Game](readPreference)

  def sortedCursor(
    selector: Bdoc,
    sort: Bdoc,
    batchSize: Int = 0,
    readPreference: ReadPreference = ReadPreference.secondaryPreferred
  )(implicit cursorProducer: CursorProducer[Game]) =
    coll.find(selector).sort(sort).batchSize(batchSize)
      .cursor[Game](readPreference)

  def goBerserk(pov: Pov): Funit =
    coll.update.one($id(pov.gameId), $set(
      s"${pov.color.fold(F.whitePlayer, F.blackPlayer)}.${Player.BSONFields.berserk}" -> true
    )).void

  def save(progress: Progress): Funit =
    GameDiff(progress.origin, progress.game) match {
      case (Nil, Nil) => funit
      case (sets, unsets) => coll.update.one(
        $id(progress.origin.id),
        nonEmptyMod("$set", $doc(sets)) ++ nonEmptyMod("$unset", $doc(unsets))
      ).void
    }

  private def nonEmptyMod(mod: String, doc: Bdoc) =
    if (doc.isEmpty) $empty else $doc(mod -> doc)

  def setRatingDiffs(id: ID, diffs: RatingDiffs) =
    coll.update.one($id(id), $set(
      s"${F.whitePlayer}.${Player.BSONFields.ratingDiff}" -> diffs.white,
      s"${F.blackPlayer}.${Player.BSONFields.ratingDiff}" -> diffs.black
    ))

  def urgentGames(user: User): Fu[List[Pov]] =
    coll.find(Query nowPlaying user.id).cursor[Game]()
      .list(Game.maxPlayingRealtime) map { games =>
        val povs = games flatMap { Pov(_, user) }
        try {
          povs sortWith Pov.priority
        } catch {
          case e: IllegalArgumentException =>
            povs sortBy (-_.game.movedAt.getSeconds)
        }
      }

  // gets most urgent game to play
  def mostUrgentGame(user: User): Fu[Option[Pov]] = urgentGames(user) map (_.headOption)

  def playingRealtimeNoAi(user: User, nb: Int): Fu[List[Pov]] =
    coll.find(
      Query.nowPlaying(user.id) ++ Query.noAi ++ Query.clock(true)
    ).cursor[Game]().list(nb) map {
        _ flatMap { Pov(_, user) }
      }

  // gets last recently played move game in progress
  def lastPlayedPlaying(user: User): Fu[Option[Pov]] =
    coll.find(Query recentlyPlaying user.id)
      .sort(Query.sortMovedAtNoIndex)
      .one[Game](ReadPreference.secondaryPreferred)
      .map { _ flatMap { Pov(_, user) } }

  def lastPlayed(user: User): Fu[Option[Pov]] =
    coll.find(Query user user.id).sort($sort desc F.createdAt)
      .cursor[Game]().list(20) map {
        _.sortBy(_.movedAt).lastOption flatMap { Pov(_, user) }
      }

  def lastFinishedRatedNotFromPosition(user: User): Fu[Option[Game]] =
    coll.find(
      Query.user(user.id) ++
        Query.rated ++
        Query.finished ++
        Query.turnsGt(2) ++
        Query.notFromPosition
    ).sort(Query.sortAntiChronological).one[Game]

  def setTv(id: ID): Unit = {
    coll.update(false, WriteConcern.Unacknowledged).one(
      q = $id(id), u = $set(F.tvAt -> DateTime.now)
    )

    ()
  }

  def setAnalysed(id: ID): Unit = {
    coll.update(false, WriteConcern.Unacknowledged).one(
      q = $id(id), u = $set(F.analysed -> true)
    )

    ()
  }

  def setUnanalysed(id: ID): Unit = {
    coll.update(false, WriteConcern.Unacknowledged).one(
      q = $id(id), u = $set(F.analysed -> false)
    )

    ()
  }

  def isAnalysed(id: ID): Fu[Boolean] =
    coll.exists($id(id) ++ Query.analysed(true))

  def filterAnalysed(ids: Seq[ID]): Fu[Set[ID]] =
    coll.distinct[ID, Set]("_id", $inIds(ids) ++ $doc(
      F.analysed -> true
    ))

  def exists(id: ID) = coll.exists($id(id))

  def tournamentId(id: ID): Fu[Option[String]] = coll.primitiveOne[String]($id(id), F.tournamentId)

  def incBookmarks(id: ID, value: Int) =
    coll.update.one($id(id), $inc(F.bookmarks -> value)).void

  def setHoldAlert(pov: Pov, mean: Int, sd: Int, ply: Option[Int] = None) = {
    import Player.holdAlertBSONHandler
    coll.update.one(
      $id(pov.gameId),
      $set(
        s"p${pov.color.fold(0, 1)}.${Player.BSONFields.holdAlert}" ->
          Player.HoldAlert(ply = ply | pov.game.turns, mean = mean, sd = sd)
      )
    ).void
  }

  def setBorderAlert(pov: Pov) = setHoldAlert(pov, 0, 0, 20.some)

  private val finishUnsets = $doc(
    F.positionHashes -> true,
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
    status: Status
  ) = coll.update.one(
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
      }
    )
  )

  def findRandomStandardCheckmate(distribution: Int): Fu[Option[Game]] =
    coll.find(Query.mate ++ Query.variantStandard).sort(Query.sortCreated)
      .skip(Random nextInt distribution).one[Game]

  def insertDenormalized(g: Game, initialFen: Option[chess.format.FEN] = None): Funit = {
    val g2 = if (g.rated && (g.userIds.distinct.size != 2 || !Game.allowRated(g.variant, g.clock.map(_.config))))
      g.copy(mode = chess.Mode.Casual)
    else g
    val userIds = g2.userIds.distinct
    val fen = initialFen.map(_.value) orElse {
      (!g2.variant.standardInitialPosition)
        .option(Forsyth >> g2.chess)
        .filter(Forsyth.initial !=)
    }
    val checkInHours =
      if (g2.isPgnImport) none
      else if (g2.hasClock) 1.some
      else if (g2.hasAi) (Game.aiAbandonedHours + 1).some
      else (24 * 10).some
    val bson = (gameBSONHandler write g2) ++ $doc(
      F.initialFen -> fen,
      F.checkAt -> checkInHours.map(DateTime.now.plusHours),
      F.playingUids -> (g2.started && userIds.nonEmpty).option(userIds)
    )

    coll.insert.one(bson).void
  } >>- {
    lila.mon.game.create.variant(g.variant.key)()
    lila.mon.game.create.source(g.source.fold("unknown")(_.name))()
    lila.mon.game.create.speed(g.speed.name)()
    lila.mon.game.create.mode(g.mode.name)()
  }

  def removeRecentChallengesOf(userId: String): Funit = coll.delete.one(
    Query.created ++ Query.friend ++ Query.user(userId) ++
      Query.createdSince(DateTime.now minusHours 1)
  ).void

  def setCheckAt(g: Game, at: DateTime) =
    coll.update.one($id(g.id), $doc("$set" -> $doc(F.checkAt -> at)))

  def unsetCheckAt(g: Game) =
    coll.update.one($id(g.id), $doc("$unset" -> $doc(F.checkAt -> true)))

  def unsetPlayingUids(g: Game): Unit = {
    coll.update(false, WriteConcern.Unacknowledged)
      .one($id(g.id), $unset(F.playingUids))

    ()
  }

  // used to make a compound sparse index
  def setImportCreatedAt(g: Game): Funit =
    coll.update.one($id(g.id), $set("pgni.ca" -> g.createdAt)).void

  def saveNext(game: Game, nextId: ID): Funit = coll.update.one(
    $id(game.id),
    $set(F.next -> nextId) ++
      $unset(
        "p0." + Player.BSONFields.isOfferingRematch,
        "p1." + Player.BSONFields.isOfferingRematch
      )
  ).void

  def initialFen(gameId: ID): Fu[Option[FEN]] =
    coll.primitiveOne[FEN]($id(gameId), F.initialFen)

  def initialFen(game: Game): Fu[Option[FEN]] =
    if (game.imported || !game.variant.standardInitialPosition) initialFen(game.id) map {
      case None if game.variant == chess.variant.Chess960 => FEN(Forsyth.initial).some
      case fen => fen
    }
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

  def featuredCandidates: Fu[List[Game]] = coll.find(
    Query.playable ++ Query.clock(true) ++ $doc(
      F.createdAt $gt (DateTime.now minusMinutes 5),
      F.movedAt $gt (DateTime.now minusSeconds 40)
    ) ++ $or(
        s"${F.whitePlayer}.${Player.BSONFields.rating}" $gt 1200,
        s"${F.blackPlayer}.${Player.BSONFields.rating}" $gt 1200
      )
  ).cursor[Game]().list

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
    .one[Game]

  def findPgnImport(pgn: String): Fu[Option[Game]] =
    coll.find($doc(s"${F.pgnImport}.h" -> PgnImport.hash(pgn))).one[Game]

  def getOptionPgn(id: ID): Fu[Option[PgnMoves]] = game(id) map2 { (g: Game) => g.pgnMoves }

  def lastGameBetween(u1: String, u2: String, since: DateTime): Fu[Option[Game]] =
    coll.find($doc(
      F.playerUids $all List(u1, u2),
      F.createdAt $gt since
    )).one[Game]

  def lastGamesBetween(u1: User, u2: User, since: DateTime, nb: Int): Fu[List[Game]] =
    List(u1, u2).forall(_.count.game > 0) ??
      coll.find($doc(
        F.playerUids $all List(u1.id, u2.id),
        F.createdAt $gt since
      )).cursor[Game](ReadPreference.secondaryPreferred).list(nb)

  def getSourceAndUserIds(id: ID): Fu[(Option[Source], List[User.ID])] =
    coll.find($id(id), Some($doc(F.playerUids -> true, F.source -> true)))
      .one[Bdoc].map {
        _.fold(none[Source] -> List.empty[User.ID]) { doc =>
          (doc.getAs[Int](F.source) flatMap Source.apply,
            ~doc.getAs[List[User.ID]](F.playerUids))
        }
      }

  def recentAnalysableGamesByUserId(userId: User.ID, nb: Int): Fu[List[Game]] =
    coll.find(
      Query.finished
        ++ Query.rated
        ++ Query.user(userId)
        ++ Query.turnsGt(20)
    ).sort(Query.sortCreated)
      .cursor[Game](ReadPreference.secondaryPreferred).list(nb)
}
