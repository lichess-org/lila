package lila.game

import scala.util.Random

import chess.format.{ Forsyth, FEN }
import chess.{ Color, Status }
import org.joda.time.DateTime
import reactivemongo.akkastream.{ AkkaStreamCursor, cursorProducer }
import reactivemongo.api.bson.BSONDocument
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.WriteConcern
import reactivemongo.api.ReadPreference

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._
import lila.db.{ ByteArray, isDuplicateKey }
import lila.user.User

final class GameRepo(val coll: Coll) {

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
    coll.one[Game]($id(gameId) ++ Query.finished)

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

  def remove(id: ID) = coll.delete.one($id(id)).void

  def userPovsByGameIds(gameIds: List[String], user: User, readPreference: ReadPreference = ReadPreference.secondaryPreferred): Fu[List[Pov]] =
    coll.byOrderedIds[Game, ID](gameIds)(_.id) map { _.flatMap(g => Pov(g, user)) }

  def recentPovsByUserFromSecondary(user: User, nb: Int): Fu[List[Pov]] =
    coll.ext.find(Query user user)
      .sort(Query.sortCreated)
      .cursor[Game](ReadPreference.secondaryPreferred)
      .gather[List](nb)
      .map { _.flatMap(g => Pov(g, user)) }

  def gamesForAssessment(userId: String, nb: Int): Fu[List[Game]] = coll.ext.find(
    Query.finished
      ++ Query.rated
      ++ Query.user(userId)
      ++ Query.analysed(true)
      ++ Query.turnsGt(20)
      ++ Query.clockHistory(true)
  )
    .sort($sort asc F.createdAt)
    .list[Game](nb, ReadPreference.secondaryPreferred)

  def extraGamesForIrwin(userId: String, nb: Int): Fu[List[Game]] = coll.ext.find(
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
  ): AkkaStreamCursor[Game] =
    coll.ext.find(selector).cursor[Game](readPreference)

  def sortedCursor(
    selector: Bdoc,
    sort: Bdoc,
    batchSize: Int = 0,
    readPreference: ReadPreference = ReadPreference.secondaryPreferred
  ): AkkaStreamCursor[Game] =
    coll.ext.find(selector).sort(sort).batchSize(batchSize).cursor[Game](readPreference)

  def goBerserk(pov: Pov): Funit =
    coll.update.one($id(pov.gameId), $set(
      s"${pov.color.fold(F.whitePlayer, F.blackPlayer)}.${Player.BSONFields.berserk}" -> true
    )).void

  def update(progress: Progress): Funit =
    saveDiff(progress.origin, GameDiff(progress.origin, progress.game))

  private def saveDiff(origin: Game, diff: GameDiff.Diff): Funit = diff match {
    case (Nil, Nil) => funit
    case (sets, unsets) => coll.update.one(
      $id(origin.id),
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

  // Use Env.round.proxy.urgentGames to get in-heap states!
  def urgentPovsUnsorted(user: User): Fu[List[Pov]] =
    coll.list[Game](Query nowPlaying user.id, Game.maxPlayingRealtime) map {
      _ flatMap { Pov(_, user) }
    }

  def playingRealtimeNoAi(user: User, nb: Int): Fu[List[Game.ID]] =
    coll.distinctEasy[Game.ID, List](F.id, Query.nowPlaying(user.id) ++ Query.noAi ++ Query.clock(true))

  def lastPlayedPlayingId(userId: User.ID): Fu[Option[Game.ID]] =
    coll.find(Query recentlyPlaying userId, $id(true).some)
      .sort(Query.sortMovedAtNoIndex)
      .one[Bdoc](readPreference = ReadPreference.secondaryPreferred)
      .map { _.flatMap(_.getAsOpt[Game.ID](F.id)) }

  def allPlaying(userId: User.ID): Fu[List[Pov]] =
    coll.ext.find(Query nowPlaying userId).list[Game]()
      .map { _ flatMap { Pov.ofUserId(_, userId) } }

  def lastPlayed(user: User): Fu[Option[Pov]] =
    coll.ext.find(Query user user.id)
      .sort($sort desc F.createdAt)
      .list[Game](3).map {
        _.sortBy(_.movedAt).lastOption flatMap { Pov(_, user) }
      }

  def lastFinishedRatedNotFromPosition(user: User): Fu[Option[Game]] = coll.ext.find(
    Query.user(user.id) ++
      Query.rated ++
      Query.finished ++
      Query.turnsGt(2) ++
      Query.notFromPosition
  ).sort(Query.sortAntiChronological).one[Game]

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
    coll.distinctEasy[ID, Set]("_id", $inIds(ids) ++ $doc(F.analysed -> true))

  def exists(id: ID) = coll.exists($id(id))

  def tournamentId(id: ID): Fu[Option[String]] = coll.primitiveOne[String]($id(id), F.tournamentId)

  def incBookmarks(id: ID, value: Int) =
    coll.update.one($id(id), $inc(F.bookmarks -> value)).void

  def setHoldAlert(pov: Pov, alert: Player.HoldAlert) = coll.updateField(
    $id(pov.gameId), holdAlertField(pov.color), alert
  )

  def setBorderAlert(pov: Pov) = setHoldAlert(pov, Player.HoldAlert(0, 0, 20))

  def holdAlerts(game: Game): Fu[Player.HoldAlert.Map] =
    coll.one[Bdoc](
      $doc(
        F.id -> game.id,
        $or(
          holdAlertField(chess.White) $exists true,
          holdAlertField(chess.Black) $exists true
        )
      ),
      $doc(
        F.id -> false,
        holdAlertField(chess.White) -> true,
        holdAlertField(chess.Black) -> true
      )
    ) map {
        _.fold(Player.HoldAlert.emptyMap) { doc =>
          def holdAlertOf(playerField: String) =
            doc.child(playerField).flatMap(_.getAsOpt[Player.HoldAlert](Player.BSONFields.holdAlert))
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

  def findRandomStandardCheckmate(distribution: Int): Fu[Option[Game]] = coll.ext.find(
    Query.mate ++ Query.variantStandard
  ).sort(Query.sortCreated)
    .skip(Random nextInt distribution)
    .one[Game]

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
    coll.insert.one(bson) addFailureEffect {
      case wr: WriteResult if isDuplicateKey(wr) => lila.mon.game.idCollision()
    } void
  } >>- {
    lila.mon.game.create.variant(g.variant.key)()
    lila.mon.game.create.source(g.source.fold("unknown")(_.name))()
    lila.mon.game.create.speed(g.speed.name)()
    lila.mon.game.create.mode(g.mode.name)()
  }

  def removeRecentChallengesOf(userId: String) =
    coll.delete.one(Query.created ++ Query.friend ++ Query.user(userId) ++
      Query.createdSince(DateTime.now minusHours 1))

  def setCheckAt(g: Game, at: DateTime) =
    coll.update.one($id(g.id), $doc("$set" -> $doc(F.checkAt -> at)))

  def unsetCheckAt(g: Game) =
    coll.update.one($id(g.id), $doc("$unset" -> $doc(F.checkAt -> true)))

  def unsetPlayingUids(g: Game): Unit =
    coll.update(false, WriteConcern.Unacknowledged).one($id(g.id), $unset(F.playingUids))

  // used to make a compound sparse index
  def setImportCreatedAt(g: Game) =
    coll.update.one($id(g.id), $set("pgni.ca" -> g.createdAt)).void

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

  def withInitialFens(games: List[Game]): Fu[List[(Game, Option[FEN])]] =
    games.map { game =>
      initialFen(game) map { game -> _ }
    }.sequenceFu

  def count(query: Query.type => Bdoc): Fu[Int] = coll countSel query(Query)

  private[game] def bestOpponents(userId: String, limit: Int): Fu[List[(User.ID, Int)]] = {
    coll.aggregateList(
      maxDocs = limit,
      ReadPreference.secondaryPreferred
    ) { framework =>
        import framework._
        Match($doc(F.playerUids -> userId)) -> List(
          Match($doc(F.playerUids -> $doc("$size" -> 2))),
          Sort(Descending(F.createdAt)),
          Limit(1000), // only look in the last 1000 games
          Project($doc(
            F.playerUids -> true,
            F.id -> false
          )),
          UnwindField(F.playerUids),
          Match($doc(F.playerUids -> $doc("$ne" -> userId))),
          GroupField(F.playerUids)("gs" -> SumAll),
          Sort(Descending("gs")),
          Limit(limit)
        )
      }.map(_.flatMap { obj =>
        obj.string("_id") flatMap { id =>
          obj.int("gs") map { id -> _ }
        }
      })
  }

  def random: Fu[Option[Game]] = coll.ext.find($empty)
    .sort(Query.sortCreated)
    .skip(Random nextInt 1000)
    .one[Game]

  def findPgnImport(pgn: String): Fu[Option[Game]] = coll.one[Game](
    $doc(s"${F.pgnImport}.h" -> PgnImport.hash(pgn))
  )

  def getOptionPgn(id: ID): Fu[Option[PgnMoves]] = game(id) map2 { (g: Game) => g.pgnMoves }

  def lastGameBetween(u1: String, u2: String, since: DateTime): Fu[Option[Game]] =
    coll.one[Game]($doc(
      F.playerUids $all List(u1, u2),
      F.createdAt $gt since
    ))

  def lastGamesBetween(u1: User, u2: User, since: DateTime, nb: Int): Fu[List[Game]] =
    List(u1, u2).forall(_.count.game > 0) ??
      coll.ext.find($doc(
        F.playerUids $all List(u1.id, u2.id),
        F.createdAt $gt since
      )).list[Game](nb, ReadPreference.secondaryPreferred)

  def getSourceAndUserIds(id: ID): Fu[(Option[Source], List[User.ID])] =
    coll.one[Bdoc]($id(id), $doc(F.playerUids -> true, F.source -> true)) map {
      _.fold(none[Source] -> List.empty[User.ID]) { doc =>
        (doc.int(F.source) flatMap Source.apply,
          ~doc.getAsOpt[List[User.ID]](F.playerUids))
      }
    }

  def recentAnalysableGamesByUserId(userId: User.ID, nb: Int) =
    coll.ext.find(
      Query.finished
        ++ Query.rated
        ++ Query.user(userId)
        ++ Query.turnsGt(20)
    ).sort(Query.sortCreated)
      .cursor[Game](ReadPreference.secondaryPreferred)
      .list(nb)
}
