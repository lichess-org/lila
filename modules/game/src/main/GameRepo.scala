package lila.game

import scala.concurrent.duration._

import chess.format.{ FEN, Forsyth }
import chess.{ Color, Status }
import org.joda.time.DateTime
import reactivemongo.akkastream.{ cursorProducer, AkkaStreamCursor }
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.{ Cursor, ReadPreference, WriteConcern }

import lila.common.ThreadLocalRandom
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._
import lila.db.isDuplicateKey
import lila.user.User
import lila.common.config

final class GameRepo(val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._
  import Game.{ BSONFields => F, ID }
  import Player.holdAlertBSONHandler

  val fixedColorLobbyCache = new lila.memo.ExpireSetMemo(2 hours)

  def game(gameId: ID): Fu[Option[Game]]              = coll.byId[Game](gameId)
  def gameFromSecondary(gameId: ID): Fu[Option[Game]] = coll.secondaryPreferred.byId[Game](gameId)

  def gamesFromSecondary(gameIds: Seq[ID]): Fu[List[Game]] =
    coll.byOrderedIds[Game, ID](gameIds, readPreference = ReadPreference.secondaryPreferred)(_.id)

  def gameOptionsFromSecondary(gameIds: Seq[ID]): Fu[List[Option[Game]]] =
    coll.optionsByOrderedIds[Game, ID](gameIds, none, ReadPreference.secondaryPreferred)(_.id)

  object light {

    def game(gameId: ID): Fu[Option[LightGame]] = coll.byId[LightGame](gameId, LightGame.projection)

    def pov(gameId: ID, color: Color): Fu[Option[LightPov]] =
      game(gameId) dmap2 { (game: LightGame) =>
        LightPov(game, game player color)
      }

    def pov(ref: PovRef): Fu[Option[LightPov]] = pov(ref.gameId, ref.color)

    def gamesFromPrimary(gameIds: Seq[ID]): Fu[List[LightGame]] =
      coll.byOrderedIds[LightGame, ID](gameIds, projection = LightGame.projection.some)(_.id)

    def gamesFromSecondary(gameIds: Seq[ID]): Fu[List[LightGame]] =
      coll.byOrderedIds[LightGame, ID](
        gameIds,
        projection = LightGame.projection.some,
        readPreference = ReadPreference.secondaryPreferred
      )(_.id)
  }

  def finished(gameId: ID): Fu[Option[Game]] =
    coll.one[Game]($id(gameId) ++ Query.finished)

  def player(gameId: ID, color: Color): Fu[Option[Player]] =
    game(gameId) dmap2 { _ player color }

  def player(gameId: ID, playerId: ID): Fu[Option[Player]] =
    game(gameId) dmap { gameOption =>
      gameOption flatMap { _ player playerId }
    }

  def player(playerRef: PlayerRef): Fu[Option[Player]] =
    player(playerRef.gameId, playerRef.playerId)

  def pov(gameId: ID, color: Color): Fu[Option[Pov]] =
    game(gameId) dmap2 { (game: Game) =>
      Pov(game, game player color)
    }

  def pov(gameId: ID, color: String): Fu[Option[Pov]] =
    Color.fromName(color) ?? (pov(gameId, _))

  def pov(playerRef: PlayerRef): Fu[Option[Pov]] =
    game(playerRef.gameId) dmap { _ flatMap { _ playerIdPov playerRef.playerId } }

  def pov(fullId: ID): Fu[Option[Pov]] = pov(PlayerRef(fullId))

  def pov(ref: PovRef): Fu[Option[Pov]] = pov(ref.gameId, ref.color)

  def remove(id: ID) = coll.delete.one($id(id)).void

  def userPovsByGameIds(
      gameIds: List[Game.ID],
      user: User,
      readPreference: ReadPreference = ReadPreference.secondaryPreferred
  ): Fu[List[Pov]] =
    coll.byOrderedIds[Game, ID](gameIds, readPreference = readPreference)(_.id) dmap {
      _.flatMap(g => Pov(g, user))
    }

  def recentPovsByUserFromSecondary(user: User, nb: Int, select: Bdoc = $empty): Fu[List[Pov]] =
    recentGamesByUserFromSecondaryCursor(user, select)
      .list(nb)
      .map { _.flatMap(Pov(_, user)) }

  def recentGamesByUserFromSecondaryCursor(user: User, select: Bdoc = $empty) =
    coll
      .find(Query.user(user) ++ select)
      .sort(Query.sortCreated)
      .cursor[Game](ReadPreference.secondaryPreferred)

  def gamesForAssessment(userId: User.ID, nb: Int): Fu[List[Game]] =
    coll
      .find(
        Query.finished
          ++ Query.rated
          ++ Query.user(userId)
          ++ Query.analysed(true)
          ++ Query.turnsGt(20)
          ++ Query.clockHistory(true)
      )
      .sort($sort asc F.createdAt)
      .cursor[Game](ReadPreference.secondaryPreferred)
      .list(nb)

  def extraGamesForIrwin(userId: User.ID, nb: Int): Fu[List[Game]] =
    coll
      .find(
        Query.finished
          ++ Query.rated
          ++ Query.user(userId)
          ++ Query.turnsGt(22)
          ++ Query.variantStandard
          ++ Query.clock(true)
      )
      .sort($sort asc F.createdAt)
      .cursor[Game](ReadPreference.secondaryPreferred)
      .list(nb)

  def unanalysedGames(gameIds: Seq[ID], max: config.Max = config.Max(100)): Fu[List[Game]] =
    coll
      .find($inIds(gameIds) ++ Query.analysed(false) ++ Query.turns(30 to 160))
      .cursor[Game](ReadPreference.secondaryPreferred)
      .list(max.value)

  def cursor(
      selector: Bdoc,
      readPreference: ReadPreference = ReadPreference.secondaryPreferred
  ): AkkaStreamCursor[Game] =
    coll.find(selector).cursor[Game](readPreference)

  def docCursor(
      selector: Bdoc,
      readPreference: ReadPreference = ReadPreference.secondaryPreferred
  ): AkkaStreamCursor[Bdoc] =
    coll.find(selector).cursor[Bdoc](readPreference)

  def sortedCursor(
      selector: Bdoc,
      sort: Bdoc,
      batchSize: Int = 0,
      readPreference: ReadPreference = ReadPreference.secondaryPreferred
  ): AkkaStreamCursor[Game] =
    coll.find(selector).sort(sort).batchSize(batchSize).cursor[Game](readPreference)

  def byIdsCursor(ids: Iterable[Game.ID]): Cursor[Game] = coll.find($inIds(ids)).cursor[Game]()

  def goBerserk(pov: Pov): Funit =
    coll.update
      .one(
        $id(pov.gameId),
        $set(
          s"${pov.color.fold(F.whitePlayer, F.blackPlayer)}.${Player.BSONFields.berserk}" -> true
        )
      )
      .void

  def update(progress: Progress): Funit =
    saveDiff(progress.origin, GameDiff(progress.origin, progress.game))

  private def saveDiff(origin: Game, diff: GameDiff.Diff): Funit =
    diff match {
      case (Nil, Nil) => funit
      case (sets, unsets) =>
        coll.update
          .one(
            $id(origin.id),
            nonEmptyMod("$set", $doc(sets)) ++ nonEmptyMod("$unset", $doc(unsets))
          )
          .void
    }

  private def nonEmptyMod(mod: String, doc: Bdoc) =
    if (doc.isEmpty) $empty else $doc(mod -> doc)

  def setRatingDiffs(id: ID, diffs: RatingDiffs) =
    coll.update.one(
      $id(id),
      $set(
        s"${F.whitePlayer}.${Player.BSONFields.ratingDiff}" -> diffs.white,
        s"${F.blackPlayer}.${Player.BSONFields.ratingDiff}" -> diffs.black
      )
    )

  // Use Env.round.proxy.urgentGames to get in-heap states!
  def urgentPovsUnsorted(user: User): Fu[List[Pov]] =
    coll.list[Game](Query nowPlaying user.id, Game.maxPlaying + 5) dmap {
      _ flatMap { Pov(_, user) }
    }

  def countWhereUserTurn(userId: User.ID): Fu[Int] =
    coll
      .countSel(
        // important, hits the index!
        Query.nowPlaying(userId) ++ $doc(
          "$or" ->
            List(0, 1).map { rem =>
              $doc(
                s"${Game.BSONFields.playingUids}.$rem" -> userId,
                Game.BSONFields.turns                  -> $doc("$mod" -> $arr(2, rem))
              )
            }
        )
      )
      .dmap(_.toInt)

  def playingRealtimeNoAi(user: User): Fu[List[Game.ID]] =
    coll.distinctEasy[Game.ID, List](
      F.id,
      Query.nowPlaying(user.id) ++ Query.noAi ++ Query.clock(true),
      ReadPreference.secondaryPreferred
    )

  def lastPlayedPlayingId(userId: User.ID): Fu[Option[Game.ID]] =
    coll
      .find(Query recentlyPlaying userId, $id(true).some)
      .sort(Query.sortMovedAtNoIndex)
      .one[Bdoc](readPreference = ReadPreference.primary)
      .dmap { _.flatMap(_.getAsOpt[Game.ID](F.id)) }

  def allPlaying(userId: User.ID): Fu[List[Pov]] =
    coll
      .list[Game](Query nowPlaying userId)
      .dmap { _ flatMap { Pov.ofUserId(_, userId) } }

  def lastPlayed(user: User): Fu[Option[Pov]] =
    coll
      .find(Query user user.id)
      .sort($sort desc F.createdAt)
      .cursor[Game]()
      .list(2)
      .dmap {
        _.sortBy(_.movedAt).lastOption flatMap { Pov(_, user) }
      }

  def quickLastPlayedId(userId: User.ID): Fu[Option[Game.ID]] =
    coll
      .find(Query user userId, $id(true).some)
      .sort($sort desc F.createdAt)
      .one[Bdoc]
      .dmap { _.flatMap(_.getAsOpt[Game.ID](F.id)) }

  def lastFinishedRatedNotFromPosition(user: User): Fu[Option[Game]] =
    coll
      .find(
        Query.user(user.id) ++
          Query.rated ++
          Query.finished ++
          Query.turnsGt(2) ++
          Query.notFromPosition
      )
      .sort(Query.sortAntiChronological)
      .one[Game]

  def setTv(id: ID) = coll.updateFieldUnchecked($id(id), F.tvAt, DateTime.now)

  def setAnalysed(id: ID): Funit  = coll.updateField($id(id), F.analysed, true).void
  def setUnanalysed(id: ID): Unit = coll.updateFieldUnchecked($id(id), F.analysed, false)

  def isAnalysed(id: ID): Fu[Boolean] =
    coll.exists($id(id) ++ Query.analysed(true))

  def exists(id: ID) = coll.exists($id(id))

  def tournamentId(id: ID): Fu[Option[String]] = coll.primitiveOne[String]($id(id), F.tournamentId)

  def incBookmarks(id: ID, value: Int) =
    coll.update.one($id(id), $inc(F.bookmarks -> value)).void

  def setHoldAlert(pov: Pov, alert: Player.HoldAlert): Funit =
    coll
      .updateField(
        $id(pov.gameId),
        holdAlertField(pov.color),
        alert
      )
      .void

  object holdAlert {
    private val holdAlertSelector = $or(
      holdAlertField(chess.White) $exists true,
      holdAlertField(chess.Black) $exists true
    )
    private val holdAlertProjection = $doc(
      holdAlertField(chess.White) -> true,
      holdAlertField(chess.Black) -> true
    )
    private def holdAlertOf(doc: Bdoc, color: Color): Option[Player.HoldAlert] =
      doc.child(color.fold("p0", "p1")).flatMap(_.getAsOpt[Player.HoldAlert](Player.BSONFields.holdAlert))

    def game(game: Game): Fu[Player.HoldAlert.Map] =
      coll.one[Bdoc](
        $doc(F.id -> game.id, holdAlertSelector),
        holdAlertProjection
      ) map {
        _.fold(Player.HoldAlert.emptyMap) { doc =>
          Color.Map(white = holdAlertOf(doc, chess.White), black = holdAlertOf(doc, chess.Black))
        }
      }

    def povs(povs: Seq[Pov]): Fu[Map[Game.ID, Player.HoldAlert]] =
      coll
        .find(
          $doc($inIds(povs.map(_.gameId)), holdAlertSelector),
          holdAlertProjection.some
        )
        .cursor[Bdoc](ReadPreference.secondaryPreferred)
        .list() map { docs =>
        val idColors = povs.view.map { p =>
          p.gameId -> p.color
        }.toMap
        val holds = for {
          doc   <- docs
          id    <- doc string "_id"
          color <- idColors get id
          holds <- holdAlertOf(doc, color)
        } yield id -> holds
        holds.toMap
      }
  }

  def hasHoldAlert(pov: Pov): Fu[Boolean] =
    coll.exists(
      $doc(
        $id(pov.gameId),
        holdAlertField(pov.color) $exists true
      )
    )

  private def holdAlertField(color: Color) = s"p${color.fold(0, 1)}.${Player.BSONFields.holdAlert}"

  private val finishUnsets = $doc(
    F.positionHashes                              -> true,
    F.playingUids                                 -> true,
    F.unmovedRooks                                -> true,
    ("p0." + Player.BSONFields.isOfferingDraw)    -> true,
    ("p1." + Player.BSONFields.isOfferingDraw)    -> true,
    ("p0." + Player.BSONFields.proposeTakebackAt) -> true,
    ("p1." + Player.BSONFields.proposeTakebackAt) -> true
  )

  def finish(
      id: ID,
      winnerColor: Option[Color],
      winnerId: Option[User.ID],
      status: Status
  ) =
    coll.update.one(
      $id(id),
      nonEmptyMod(
        "$set",
        $doc(
          F.winnerId    -> winnerId,
          F.winnerColor -> winnerColor.map(_.white),
          F.status      -> status
        )
      ) ++ $doc(
        "$unset" -> finishUnsets.++ {
          // keep the checkAt field when game is aborted,
          // so it gets deleted in 24h
          (status >= Status.Mate) ?? $doc(F.checkAt -> true)
        }
      )
    )

  def findRandomStandardCheckmate(distribution: Int): Fu[Option[Game]] =
    coll
      .find(
        Query.mate ++ Query.variantStandard
      )
      .sort(Query.sortCreated)
      .skip(ThreadLocalRandom nextInt distribution)
      .one[Game]

  def insertDenormalized(g: Game, initialFen: Option[chess.format.FEN] = None): Funit = {
    val g2 =
      if (g.rated && (g.userIds.distinct.size != 2 || !Game.allowRated(g.variant, g.clock.map(_.config))))
        g.copy(mode = chess.Mode.Casual)
      else g
    val userIds = g2.userIds.distinct
    val fen: Option[FEN] = initialFen orElse {
      (g2.variant.fromPosition || g2.variant.chess960)
        .option(Forsyth >> g2.chess)
        .filterNot(_.initial)
    }
    val checkInHours =
      if (g2.isPgnImport) none
      else if (g2.fromApi) some(24 * 7)
      else if (g2.hasClock) 1.some
      else some(24 * 10)
    val bson = (gameBSONHandler write g2) ++ $doc(
      F.initialFen  -> fen,
      F.checkAt     -> checkInHours.map(DateTime.now.plusHours),
      F.playingUids -> (g2.started && userIds.nonEmpty).option(userIds)
    )
    coll.insert.one(bson) addFailureEffect {
      case wr: WriteResult if isDuplicateKey(wr) => lila.mon.game.idCollision.increment().unit
    } void
  }

  def removeRecentChallengesOf(userId: User.ID) =
    coll.delete.one(
      Query.created ++ Query.friend ++ Query.user(userId) ++
        Query.createdSince(DateTime.now minusHours 1)
    )

  def setCheckAt(g: Game, at: DateTime) =
    coll.updateField($id(g.id), F.checkAt, at).void

  def unsetCheckAt(id: Game.ID): Funit =
    coll.unsetField($id(id), F.checkAt).void

  def unsetPlayingUids(g: Game): Unit =
    coll.update(ordered = false, WriteConcern.Unacknowledged).one($id(g.id), $unset(F.playingUids)).unit

  // used to make a compound sparse index
  def setImportCreatedAt(g: Game) =
    coll.updateField($id(g.id), "pgni.ca", g.createdAt).void

  def initialFen(gameId: ID): Fu[Option[FEN]] =
    coll.primitiveOne[FEN]($id(gameId), F.initialFen)

  def initialFen(game: Game): Fu[Option[FEN]] =
    if (game.imported || !game.variant.standardInitialPosition) initialFen(game.id) dmap {
      case None if game.variant == chess.variant.Chess960 => Forsyth.initial.some
      case fen                                            => fen
    }
    else fuccess(none)

  def gameWithInitialFen(gameId: ID): Fu[Option[(Game, Option[FEN])]] =
    game(gameId) flatMap {
      _ ?? { game =>
        initialFen(game) dmap { fen =>
          Option(game -> fen)
        }
      }
    }

  def withInitialFen(game: Game): Fu[Game.WithInitialFen] =
    initialFen(game) dmap { Game.WithInitialFen(game, _) }

  def withInitialFens(games: List[Game]): Fu[List[(Game, Option[FEN])]] =
    games.map { game =>
      initialFen(game) dmap { game -> _ }
    }.sequenceFu

  def count(query: Query.type => Bdoc): Fu[Int]    = coll countSel query(Query)
  def countSec(query: Query.type => Bdoc): Fu[Int] = coll.secondaryPreferred countSel query(Query)

  private[game] def favoriteOpponents(
      userId: User.ID,
      opponentLimit: Int,
      gameLimit: Int
  ): Fu[List[(User.ID, Int)]] = {
    coll
      .aggregateList(
        maxDocs = opponentLimit,
        ReadPreference.secondaryPreferred
      ) { framework =>
        import framework._
        Match($doc(F.playerUids -> userId)) -> List(
          Match($doc(F.playerUids -> $doc("$size" -> 2))),
          Sort(Descending(F.createdAt)),
          Limit(gameLimit), // only look in the last n games
          Project(
            $doc(
              F.playerUids -> true,
              F.id         -> false
            )
          ),
          UnwindField(F.playerUids),
          Match($doc(F.playerUids $ne userId)),
          GroupField(F.playerUids)("gs" -> SumAll),
          Sort(Descending("gs")),
          Limit(opponentLimit)
        )
      }
      .map(_.flatMap { obj =>
        obj.string(F.id) flatMap { id =>
          obj.int("gs") map { id -> _ }
        }
      })
  }

  def random: Fu[Option[Game]] =
    coll
      .find($empty)
      .sort(Query.sortCreated)
      .skip(ThreadLocalRandom nextInt 1000)
      .one[Game]

  def findPgnImport(pgn: String): Fu[Option[Game]] =
    coll.one[Game](
      $doc(s"${F.pgnImport}.h" -> PgnImport.hash(pgn))
    )

  def getOptionPgn(id: ID): Fu[Option[PgnMoves]] = game(id) dmap2 { _.pgnMoves }

  def lastGameBetween(u1: User.ID, u2: User.ID, since: DateTime): Fu[Option[Game]] =
    coll.one[Game](
      $doc(
        F.playerUids $all List(u1, u2),
        F.createdAt $gt since
      )
    )

  def lastGamesBetween(u1: User, u2: User, since: DateTime, nb: Int): Fu[List[Game]] =
    List(u1, u2).forall(_.count.game > 0) ??
      coll.secondaryPreferred.list[Game](
        $doc(
          F.playerUids $all List(u1.id, u2.id),
          F.createdAt $gt since
        ),
        nb
      )

  def getSourceAndUserIds(id: ID): Fu[(Option[Source], List[User.ID])] =
    coll.one[Bdoc]($id(id), $doc(F.playerUids -> true, F.source -> true)) dmap {
      _.fold(none[Source] -> List.empty[User.ID]) { doc =>
        (doc.int(F.source) flatMap Source.apply, ~doc.getAsOpt[List[User.ID]](F.playerUids))
      }
    }

  def recentAnalysableGamesByUserId(userId: User.ID, nb: Int) =
    coll
      .find(
        Query.finished
          ++ Query.rated
          ++ Query.user(userId)
          ++ Query.turnsGt(20)
      )
      .sort(Query.sortCreated)
      .cursor[Game](ReadPreference.secondaryPreferred)
      .list(nb)

  // only for student games, for aggregation
  def denormalizePerfType(game: Game): Unit =
    game.perfType ?? { pt =>
      coll.updateFieldUnchecked($id(game.id), F.perfType, pt.id)
    }
}
