package lila.challenge

import akka.stream.scaladsl.*
import reactivemongo.api.bson.*
import scala.util.chaining.*

import lila.common.{ Bus, Days, LilaStream, Template }
import lila.db.dsl.{ *, given }
import lila.game.{ Game, Player }
import lila.hub.actorApi.map.TellMany
import lila.hub.AsyncActorSequencers
import lila.rating.PerfType
import lila.setup.SetupBulk.{ ScheduledBulk, ScheduledGame, maxBulks }
import lila.user.User
import chess.{ Clock, ByColor, Speed }
import lila.common.config.Max
import lila.round.actorApi.round.StartClock

final class ChallengeBulkApi(
    colls: ChallengeColls,
    msgApi: ChallengeMsg,
    gameRepo: lila.game.GameRepo,
    userApi: lila.user.UserApi,
    onStart: lila.round.OnStart
)(using Executor, akka.stream.Materializer, Scheduler):

  import lila.game.BSONHandlers.given
  private given BSONDocumentHandler[ScheduledGame]      = Macros.handler
  private given BSONHandler[chess.variant.Variant]      = variantByKeyHandler
  private given BSONHandler[Clock.Config]               = clockConfigHandler
  private given BSONHandler[Either[Clock.Config, Days]] = eitherHandler[Clock.Config, Days]
  private given BSONHandler[Template]                   = stringAnyValHandler(_.value, Template.apply)
  private given BSONDocumentHandler[ScheduledBulk]      = Macros.handler

  private val coll = colls.bulk

  private val workQueue = AsyncActorSequencers[UserId](
    maxSize = Max(16),
    expiration = 10 minutes,
    timeout = 10 seconds,
    name = "challenge.bulk"
  )

  def scheduledBy(me: User): Fu[List[ScheduledBulk]] =
    coll.find($doc("by" -> me.id)).sort($sort desc "pairAt").cursor[ScheduledBulk]().list(100)

  def findBy(id: String, me: User): Fu[Option[ScheduledBulk]] =
    coll.one[ScheduledBulk]($doc("_id" -> id, "by" -> me.id))

  def deleteBy(id: String, me: User): Fu[Boolean] =
    coll.delete.one($doc("_id" -> id, "by" -> me.id)).map(_.n == 1)

  def startClocksAsap(id: String, me: User): Fu[Boolean] =
    coll
      .updateField($doc("_id" -> id, "by" -> me.id, "pairedAt" $exists true), "startClocksAt", nowInstant)
      .map(_.n == 1)

  def schedule(bulk: ScheduledBulk): Fu[Either[String, ScheduledBulk]] = workQueue(bulk.by):
    coll.list[ScheduledBulk]($doc("by" -> bulk.by, "pairedAt" $exists false)) flatMap: bulks =>
      if bulks.sizeIs >= maxBulks then fuccess(Left("Already too many bulks queued"))
      else if bulks.map(_.games.size).sum >= 1000
      then fuccess(Left("Already too many games queued"))
      else if bulks.exists(_ collidesWith bulk)
      then fuccess(Left("A bulk containing the same players is scheduled at the same time"))
      else coll.insert.one(bulk) inject Right(bulk)

  private[challenge] def tick: Funit =
    checkForPairing >> checkForClocks

  private def checkForPairing: Funit =
    coll
      .one[ScheduledBulk]($doc("pairAt" $lte nowInstant, "pairedAt" $exists false))
      .flatMapz: bulk =>
        workQueue(bulk.by):
          makePairings(bulk).void

  private def checkForClocks: Funit =
    coll.one[ScheduledBulk](
      $doc("startClocksAt" $lte nowInstant, "startedClocksAt" $exists false, "pairedAt" $exists true)
    ) flatMapz: bulk =>
      workQueue(bulk.by):
        Bus.publish(TellMany(bulk.games.map(_.id.value), StartClock), "roundSocket")
        coll.updateField($id(bulk.id), "startedClocksAt", nowInstant).void

  private def makePairings(bulk: ScheduledBulk): Funit =
    def timeControl =
      bulk.clock.fold(Challenge.TimeControl.Clock.apply, Challenge.TimeControl.Correspondence.apply)
    val (chessGame, state) = ChallengeJoiner.gameSetup(bulk.variant, timeControl, bulk.fen)
    val perfType           = PerfType(bulk.variant, Speed(bulk.clock.left.toOption))
    Source(bulk.games)
      .mapAsyncUnordered(8): game =>
        userApi.gamePlayers.loggedIn(game.userIds, bulk.perfType, useCache = false) map2: users =>
          (game.id, users)
      .mapConcat(_.toList)
      .map: (id, users) =>
        val game = Game
          .make(
            chess = chessGame,
            players = users.map(some).mapWithColor(Player.make),
            mode = bulk.mode,
            source = lila.game.Source.Api,
            daysPerTurn = bulk.clock.toOption,
            pgnImport = None,
            rules = bulk.rules
          )
          .withId(id)
          .pipe(ChallengeJoiner.addGameHistory(state))
          .start
        (game, users)
      .mapAsyncUnordered(8): (game, users) =>
        gameRepo
          .insertDenormalized(game)
          .recover(e => logger.error(s"Bulk.insertGame ${game.id} ${e.getMessage}"))
          .andDo(onStart(game.id))
          .inject(game -> users)
      .mapAsyncUnordered(8): (game, users) =>
        msgApi
          .onApiPair(game.id, users.map(_.light))(bulk.by, bulk.message)
          .recover(e => logger.error(s"Bulk.sendMsg ${game.id} ${e.getMessage}"))
      .toMat(LilaStream.sinkCount)(Keep.right)
      .run()
      .addEffect(lila.mon.api.challenge.bulk.createNb(bulk.by.value).increment(_))
      .logFailure(logger, e => s"Bulk.makePairings ${bulk.id} ${e.getMessage}") >> {
      coll.updateField($id(bulk.id), "pairedAt", nowInstant)
    }.void
