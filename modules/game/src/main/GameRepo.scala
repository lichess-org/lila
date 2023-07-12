package lila.game

import chess.{ Color, Status }
import chess.format.Fen
import chess.format.pgn.{ PgnStr, SanStr }
import reactivemongo.akkastream.{ cursorProducer, AkkaStreamCursor }
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.{ Cursor, WriteConcern }
import ornicar.scalalib.ThreadLocalRandom

import lila.db.dsl.{ *, given }
import lila.db.isDuplicateKey
import lila.user.User
import lila.common.config

final class GameRepo(val coll: Coll)(using Executor):

  import BSONHandlers.given
  import Game.{ BSONFields as F }
  import Player.given

  val fixedColorLobbyCache = lila.memo.ExpireSetMemo[GameId](2 hours)

  def game(gameId: GameId): Fu[Option[Game]]              = coll.byId[Game](gameId)
  def gameFromSecondary(gameId: GameId): Fu[Option[Game]] = coll.secondaryPreferred.byId[Game](gameId)

  def gamesFromSecondary(gameIds: Seq[GameId]): Fu[List[Game]] =
    coll.byOrderedIds[Game, GameId](gameIds, readPref = _.sec)(_.id)

  // #TODO FIXME
  // https://github.com/ReactiveMongo/ReactiveMongo/issues/1185
  def gamesTemporarilyFromPrimary(gameIds: Seq[GameId]): Fu[List[Game]] =
    coll.byOrderedIds[Game, GameId](gameIds, readPref = _.priTemp)(_.id)

  def gameOptionsFromSecondary(gameIds: Seq[GameId]): Fu[List[Option[Game]]] =
    coll.optionsByOrderedIds[Game, GameId](gameIds, none, _.priTemp)(_.id)

  object light:

    def game(gameId: GameId): Fu[Option[LightGame]] = coll.byId[LightGame](gameId.value, LightGame.projection)

    def pov(gameId: GameId, color: Color): Fu[Option[LightPov]] =
      game(gameId) dmap2 { (game: LightGame) =>
        LightPov(game, game player color)
      }

    def pov(ref: PovRef): Fu[Option[LightPov]] = pov(ref.gameId, ref.color)

    def gamesFromPrimary(gameIds: Seq[GameId]): Fu[List[LightGame]] =
      coll.byOrderedIds[LightGame, GameId](gameIds, projection = LightGame.projection.some)(_.id)

    def gamesFromSecondary(gameIds: Seq[GameId]): Fu[List[LightGame]] =
      coll.byOrderedIds[LightGame, GameId](
        gameIds,
        projection = LightGame.projection.some,
        _.sec
      )(_.id)

  def finished(gameId: GameId): Fu[Option[Game]] =
    coll.one[Game]($id(gameId) ++ Query.finished)

  def player(gameId: GameId, color: Color): Fu[Option[Player]] =
    game(gameId) dmap2 { _ player color }

  def player(gameId: GameId, playerId: GamePlayerId): Fu[Option[Player]] =
    game(gameId) dmap { gameOption =>
      gameOption flatMap { _ player playerId }
    }

  def player(playerRef: PlayerRef): Fu[Option[Player]] =
    player(playerRef.gameId, playerRef.playerId)

  def pov(gameId: GameId, color: Color): Fu[Option[Pov]] =
    game(gameId) dmap2 { (game: Game) =>
      Pov(game, game player color)
    }

  def pov(gameId: GameId, color: String): Fu[Option[Pov]] =
    Color.fromName(color) so (pov(gameId, _))

  def pov(playerRef: PlayerRef): Fu[Option[Pov]] =
    game(playerRef.gameId) dmap { _ flatMap { _ playerIdPov playerRef.playerId } }

  def pov(fullId: GameFullId): Fu[Option[Pov]] = pov(PlayerRef(fullId))

  def pov(ref: PovRef): Fu[Option[Pov]] = pov(ref.gameId, ref.color)

  def remove(id: GameId) = coll.delete.one($id(id)).void

  def userPovsByGameIds(
      gameIds: List[GameId],
      user: User,
      readPref: ReadPref = _.priTemp
  ): Fu[List[Pov]] =
    coll.byOrderedIds[Game, GameId](gameIds, readPref = readPref)(_.id) dmap {
      _.flatMap(g => Pov(g, user))
    }

  def recentPovsByUserFromSecondary(user: User, nb: Int, select: Bdoc = $empty): Fu[List[Pov]] =
    recentGamesFromSecondaryCursor(Query.user(user) ++ select)
      .list(nb)
      .map { _.flatMap(Pov(_, user)) }

  def recentGamesFromSecondaryCursor(select: Bdoc = $empty) =
    coll
      .find(select)
      .sort(Query.sortCreated)
      .cursor[Game](ReadPref.priTemp)

  def gamesForAssessment(userId: UserId, nb: Int): Fu[List[Game]] =
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
      .cursor[Game](ReadPref.priTemp)
      .list(nb)

  def extraGamesForIrwin(userId: UserId, nb: Int): Fu[List[Game]] =
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
      .cursor[Game](ReadPref.sec)
      .list(nb)

  def unanalysedGames(gameIds: Seq[GameId], max: config.Max = config.Max(100)): Fu[List[Game]] =
    coll
      .find($inIds(gameIds) ++ Query.analysed(false) ++ Query.turns(30 to 160))
      .cursor[Game](ReadPref.priTemp)
      .list(max.value)

  def cursor(
      selector: Bdoc,
      readPref: ReadPref = _.sec
  ): AkkaStreamCursor[Game] =
    coll.find(selector).cursor[Game](readPref)

  def docCursor(
      selector: Bdoc,
      readPref: ReadPref = _.priTemp
  ): AkkaStreamCursor[Bdoc] =
    coll.find(selector).cursor[Bdoc](readPref)

  def sortedCursor(
      selector: Bdoc,
      sort: Bdoc,
      batchSize: Int = 0,
      readPref: ReadPref = _.priTemp
  ): AkkaStreamCursor[Game] =
    coll.find(selector).sort(sort).batchSize(batchSize).cursor[Game](readPref)

  def byIdsCursor(ids: Iterable[GameId]): Cursor[Game] = coll.find($inIds(ids)).cursor[Game]()

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
    diff match
      case (Nil, Nil) => funit
      case (sets, unsets) =>
        coll.update
          .one(
            $id(origin.id),
            nonEmptyMod("$set", $doc(sets)) ++ nonEmptyMod("$unset", $doc(unsets))
          )
          .void

  private def nonEmptyMod(mod: String, doc: Bdoc) =
    if doc.isEmpty then $empty else $doc(mod -> doc)

  def setRatingDiffs(id: GameId, diffs: RatingDiffs) =
    coll.update.one(
      $id(id),
      $set(
        s"${F.whitePlayer}.${Player.BSONFields.ratingDiff}" -> diffs.white,
        s"${F.blackPlayer}.${Player.BSONFields.ratingDiff}" -> diffs.black
      )
    )

  // Use Env.round.proxy.urgentGames to get in-heap states!
  def urgentPovsUnsorted[U: UserIdOf](user: U): Fu[List[Pov]] =
    coll.list[Game](Query nowPlaying user.id, Game.maxPlaying + 5) dmap {
      _ flatMap { Pov(_, user) }
    }

  def countWhereUserTurn(userId: UserId): Fu[Int] =
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

  def playingRealtimeNoAi(user: User): Fu[List[GameId]] =
    coll.distinctEasy[GameId, List](
      F.id,
      Query.nowPlaying(user.id) ++ Query.noAi ++ Query.clock(true),
      _.sec
    )

  def lastPlayedPlayingId(userId: UserId): Fu[Option[GameId]] =
    coll
      .find(Query recentlyPlaying userId, $id(true).some)
      .sort(Query.sortMovedAtNoIndex)
      .one[Bdoc](readPreference = ReadPref.pri)
      .dmap { _.flatMap(_.getAsOpt[GameId](F.id)) }

  def allPlaying[U: UserIdOf](user: U): Fu[List[Pov]] =
    coll
      .list[Game](Query nowPlaying user)
      .dmap { _ flatMap { Pov(_, user) } }

  def lastPlayed(user: User): Fu[Option[Pov]] =
    coll
      .find(Query user user.id)
      .sort($sort desc F.createdAt)
      .cursor[Game]()
      .list(2)
      .dmap {
        _.sortBy(_.movedAt).lastOption flatMap { Pov(_, user) }
      }

  def quickLastPlayedId(userId: UserId): Fu[Option[GameId]] =
    coll
      .find(Query user userId, $id(true).some)
      .sort($sort desc F.createdAt)
      .one[Bdoc]
      .dmap { _.flatMap(_.getAsOpt[GameId](F.id)) }

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

  def setTv(id: GameId) = coll.updateFieldUnchecked($id(id), F.tvAt, nowInstant)

  def setAnalysed(id: GameId): Funit  = coll.updateField($id(id), F.analysed, true).void
  def setUnanalysed(id: GameId): Unit = coll.updateFieldUnchecked($id(id), F.analysed, false)

  def isAnalysed(id: GameId): Fu[Boolean] =
    coll.exists($id(id) ++ Query.analysed(true))

  def analysed(id: GameId) = coll.one[Game]($id(id) ++ Query.analysed(true))

  def exists(id: GameId) = coll.exists($id(id))

  def tournamentId(id: GameId): Fu[Option[String]] = coll.primitiveOne[String]($id(id), F.tournamentId)

  def incBookmarks(id: GameId, value: Int) =
    coll.update.one($id(id), $inc(F.bookmarks -> value)).void

  def setHoldAlert(pov: Pov, alert: Player.HoldAlert): Funit =
    coll
      .updateField(
        $id(pov.gameId),
        holdAlertField(pov.color),
        alert
      )
      .void

  object holdAlert:
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
          chess.ByColor(holdAlertOf(doc, _))
        }
      }

    def povs(povs: Seq[Pov]): Fu[Map[GameId, Player.HoldAlert]] =
      coll
        .find(
          $doc($inIds(povs.map(_.gameId)), holdAlertSelector),
          holdAlertProjection.some
        )
        .cursor[Bdoc](ReadPref.sec)
        .listAll() map { docs =>
        val idColors = povs.view.map { p =>
          p.gameId -> p.color
        }.toMap
        val holds = for
          doc   <- docs
          id    <- doc.getAsOpt[GameId]("_id")
          color <- idColors get id
          holds <- holdAlertOf(doc, color)
        yield id -> holds
        holds.toMap
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
      id: GameId,
      winnerColor: Option[Color],
      winnerId: Option[UserId],
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
          (status >= Status.Mate) so $doc(F.checkAt -> true)
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

  def insertDenormalized(g: Game, initialFen: Option[chess.format.Fen.Epd] = None): Funit =
    val g2 =
      if g.rated && (g.userIds.distinct.size != 2 || !Game.allowRated(g.variant, g.clock.map(_.config))) then
        g.copy(mode = chess.Mode.Casual)
      else g
    val userIds = g2.userIds.distinct
    val fen: Option[Fen.Epd] = initialFen orElse {
      (g2.variant.fromPosition || g2.variant.chess960)
        .option(Fen write g2.chess)
        .filterNot(_.isInitial)
    }
    val checkInHours =
      if g2.isPgnImport then none
      else if g2.fromApi then some(24 * 7)
      else if g2.hasClock then 1.some
      else some(24 * 10)
    val bson = (gameBSONHandler write g2) ++ $doc(
      F.initialFen  -> fen,
      F.checkAt     -> checkInHours.map(nowInstant.plusHours(_)),
      F.playingUids -> (g2.started && userIds.nonEmpty).option(userIds)
    )
    coll.insert.one(bson) addFailureEffect {
      case wr: WriteResult if isDuplicateKey(wr) => lila.mon.game.idCollision.increment()
    } void

  def removeRecentChallengesOf(userId: UserId) =
    coll.delete.one(
      Query.created ++ Query.friend ++ Query.user(userId) ++
        Query.createdSince(nowInstant minusHours 1)
    )

  def setCheckAt(g: Game, at: Instant) =
    coll.updateField($id(g.id), F.checkAt, at).void

  def unsetCheckAt(id: GameId): Funit =
    coll.unsetField($id(id), F.checkAt).void

  def unsetPlayingUids(g: Game): Unit =
    coll.update(ordered = false, WriteConcern.Unacknowledged).one($id(g.id), $unset(F.playingUids))

  // used to make a compound sparse index
  def setImportCreatedAt(g: Game) =
    coll.updateField($id(g.id), "pgni.ca", g.createdAt).void

  def initialFen(gameId: GameId): Fu[Option[Fen.Epd]] =
    coll.primitiveOne[Fen.Epd]($id(gameId), F.initialFen)

  def initialFen(game: Game): Fu[Option[Fen.Epd]] =
    if game.imported || !game.variant.standardInitialPosition then
      initialFen(game.id) dmap {
        case None if game.variant == chess.variant.Chess960 => Fen.initial.some
        case fen                                            => fen
      }
    else fuccess(none)

  def gameWithInitialFen(gameId: GameId): Fu[Option[Game.WithInitialFen]] =
    game(gameId) flatMapz { game =>
      initialFen(game) dmap { fen =>
        Game.WithInitialFen(game, fen).some
      }
    }

  def withInitialFen(game: Game): Fu[Game.WithInitialFen] =
    initialFen(game) dmap { Game.WithInitialFen(game, _) }

  def withInitialFens(games: List[Game]): Fu[List[(Game, Option[Fen.Epd])]] =
    games.map { game =>
      initialFen(game) dmap { game -> _ }
    }.parallel

  def count(query: Query.type => Bdoc): Fu[Int]    = coll countSel query(Query)
  def countSec(query: Query.type => Bdoc): Fu[Int] = coll.secondaryPreferred countSel query(Query)

  private[game] def favoriteOpponents(
      userId: UserId,
      opponentLimit: Int,
      gameLimit: Int
  ): Fu[List[(UserId, Int)]] =
    coll
      .aggregateList(maxDocs = opponentLimit, _.sec): framework =>
        import framework.*
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
      .map(_.flatMap: obj =>
        obj.getAsOpt[UserId](F.id) flatMap { id =>
          obj.int("gs") map { id -> _ }
        })

  def random: Fu[Option[Game]] =
    coll
      .find(Query.variantStandard)
      .sort(Query.sortCreated)
      .skip(ThreadLocalRandom nextInt 1000)
      .one[Game]

  def findPgnImport(pgn: PgnStr): Fu[Option[Game]] =
    coll.one[Game](
      $doc(s"${F.pgnImport}.h" -> PgnImport.hash(pgn))
    )

  def getOptionPgn(id: GameId): Fu[Option[Vector[SanStr]]] = game(id).dmap2(_.sans)

  def lastGameBetween(u1: UserId, u2: UserId, since: Instant): Fu[Option[Game]] =
    coll.one[Game](
      $doc(
        F.playerUids $all List(u1, u2),
        F.createdAt $gt since
      )
    )

  def lastGamesBetween(u1: User, u2: User, since: Instant, nb: Int): Fu[List[Game]] =
    List(u1, u2).forall(_.count.game > 0) so
      coll.secondaryPreferred.list[Game](
        $doc(
          F.playerUids $all List(u1.id, u2.id),
          F.createdAt $gt since
        ),
        nb
      )

  def getSourceAndUserIds(id: GameId): Fu[(Option[Source], List[UserId])] =
    coll.one[Bdoc]($id(id), $doc(F.playerUids -> true, F.source -> true)) dmap {
      _.fold(none[Source] -> List.empty[UserId]): doc =>
        (doc.int(F.source) flatMap Source.apply, ~doc.getAsOpt[List[UserId]](F.playerUids))
    }

  def recentAnalysableGamesByUserId(userId: UserId, nb: Int) =
    coll
      .find(
        Query.finished
          ++ Query.rated
          ++ Query.user(userId)
          ++ Query.turnsGt(20)
      )
      .sort(Query.sortCreated)
      .cursor[Game](ReadPref.sec)
      .list(nb)

  // only for student games, for aggregation
  def denormalizePerfType(game: Game): Unit =
    coll.updateFieldUnchecked($id(game.id), F.perfType, game.perfType.id)
