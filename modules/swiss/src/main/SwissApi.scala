package lila.swiss

import akka.stream.scaladsl.*
import alleycats.Zero
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.time.format.{ DateTimeFormatter, FormatStyle }
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.*
import reactivemongo.api.bson.*
import scala.util.chaining.*

import lila.common.config.MaxPerSecond
import lila.common.{ Bus, LightUser }
import lila.db.dsl.{ *, given }
import lila.game.{ Game, Pov }
import lila.round.actorApi.round.QuietFlag
import lila.user.{ User, UserRepo }
import lila.common.config.Max
import lila.gathering.GreatPlayer
import lila.gathering.Condition.WithVerdicts

final class SwissApi(
    mongo: SwissMongo,
    cache: SwissCache,
    userRepo: UserRepo,
    socket: SwissSocket,
    director: SwissDirector,
    scoring: SwissScoring,
    rankingApi: SwissRankingApi,
    standingApi: SwissStandingApi,
    banApi: SwissBanApi,
    boardApi: SwissBoardApi,
    verify: SwissCondition.Verify,
    chatApi: lila.chat.ChatApi,
    lightUserApi: lila.user.LightUserApi,
    roundSocket: lila.round.RoundSocket
)(using
    ec: Executor,
    scheduler: Scheduler,
    mat: akka.stream.Materializer
):

  private val sequencer = lila.hub.AsyncActorSequencers[SwissId](
    maxSize = Max(1024), // queue many game finished events
    expiration = 20 minutes,
    timeout = 10 seconds,
    name = "swiss.api"
  )

  import BsonHandlers.{ *, given }

  def fetchByIdNoCache(id: SwissId) = mongo.swiss.byId[Swiss](id)

  def create(data: SwissForm.SwissData, me: User, teamId: TeamId): Fu[Swiss] =
    val swiss = Swiss(
      _id = Swiss.makeId,
      name = data.name | GreatPlayer.randomName,
      clock = data.clock,
      variant = data.realVariant,
      round = SwissRoundNumber(0),
      nbPlayers = 0,
      nbOngoing = 0,
      createdAt = nowInstant,
      createdBy = me.id,
      teamId = teamId,
      nextRoundAt = data.realStartsAt.some,
      startsAt = data.realStartsAt,
      finishedAt = none,
      winnerId = none,
      settings = Swiss.Settings(
        nbRounds = data.nbRounds,
        rated = data.realPosition.isEmpty && data.isRated,
        description = data.description,
        position = data.realPosition,
        chatFor = data.realChatFor,
        roundInterval = data.realRoundInterval,
        password = data.password,
        conditions = data.conditions,
        forbiddenPairings = ~data.forbiddenPairings,
        manualPairings = ~data.manualPairings
      )
    )
    mongo.swiss.insert.one(addFeaturable(swiss)) >>-
      cache.featuredInTeam.invalidate(swiss.teamId) inject swiss

  def update(swissId: SwissId, data: SwissForm.SwissData): Fu[Option[Swiss]] =
    Sequencing(swissId)(cache.swissCache.byId) { old =>
      val position =
        if (old.isCreated || old.settings.position.isDefined) data.realVariant.standard ?? data.realPosition
        else old.settings.position
      val swiss =
        old.copy(
          name = data.name | old.name,
          clock = if (old.isCreated) data.clock else old.clock,
          variant = if (old.isCreated && data.variant.isDefined) data.realVariant else old.variant,
          startsAt = data.startsAt.ifTrue(old.isCreated) | old.startsAt,
          nextRoundAt =
            if (old.isCreated) Some(data.startsAt | old.startsAt)
            else old.nextRoundAt,
          settings = old.settings.copy(
            nbRounds = data.nbRounds,
            rated = position.isEmpty && (data.rated | old.settings.rated),
            description = data.description orElse old.settings.description,
            position = position,
            chatFor = data.chatFor | old.settings.chatFor,
            roundInterval =
              if (data.roundInterval.isDefined) data.realRoundInterval
              else old.settings.roundInterval,
            password = data.password,
            conditions = data.conditions,
            forbiddenPairings = ~data.forbiddenPairings,
            manualPairings = ~data.manualPairings
          )
        ) pipe { s =>
          if (
            s.isStarted && s.nbOngoing == 0 && (s.nextRoundAt.isEmpty || old.settings.manualRounds) && !s.settings.manualRounds
          )
            s.copy(nextRoundAt = nowInstant.plusSeconds(s.settings.roundInterval.toSeconds.toInt).some)
          else if (s.settings.manualRounds && !old.settings.manualRounds)
            s.copy(nextRoundAt = none)
          else s
        }
      mongo.swiss.update.one($id(old.id), addFeaturable(swiss)).void >> {
        (swiss.perfType != old.perfType) ?? recomputePlayerRatings(swiss)
      } >>- {
        cache.swissCache clear swiss.id
        cache.roundInfo.put(swiss.id, fuccess(swiss.roundInfo.some))
        socket.reload(swiss.id)
      } inject swiss.some
    }

  private def recomputePlayerRatings(swiss: Swiss): Funit = for {
    ranking <- rankingApi(swiss)
    perfs   <- userRepo.perfOf(ranking.keys, swiss.perfType)
    update = mongo.player.update(ordered = false)
    elements <- perfs.map { case (userId, perf) =>
      update.element(
        q = $id(SwissPlayer.makeId(swiss.id, userId)),
        u = $set(
          SwissPlayer.Fields.rating      -> perf.intRating,
          SwissPlayer.Fields.provisional -> perf.provisional.yes.option(true)
        )
      )
    }.parallel
    _ <- elements.nonEmpty ?? update.many(elements).void
  } yield ()

  def scheduleNextRound(swiss: Swiss, date: Instant): Funit =
    Sequencing(swiss.id)(cache.swissCache.notFinishedById) { old =>
      for
        _ <- !old.settings.manualRounds ?? mongo.swiss
          .updateField($id(old.id), "settings.i", Swiss.RoundInterval.manual)
          .void
        _ <- old.isCreated ?? mongo.swiss.updateField($id(old.id), "startsAt", date).void
        _ <- (!old.isFinished && old.nbOngoing == 0) ??
          mongo.swiss.updateField($id(old.id), "nextRoundAt", date).void >>- {
            val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            val showDate  = formatter print date
            systemChat(swiss.id, s"Round ${swiss.round.value + 1} scheduled at $showDate UTC")
          }
      yield
        cache.swissCache clear swiss.id
        socket.reload(swiss.id)
    }

  def verdicts(swiss: Swiss, me: Option[User]): Fu[WithVerdicts] = me match
    case None       => fuccess(swiss.settings.conditions.accepted)
    case Some(user) => verify(swiss, user)

  def join(id: SwissId, me: User, isInTeam: TeamId => Boolean, password: Option[String]): Fu[Boolean] =
    Sequencing(id)(cache.swissCache.notFinishedById) { swiss =>
      if (
        swiss.settings.password.forall(p =>
          MessageDigest.isEqual(p.getBytes(UTF_8), (~password).getBytes(UTF_8))
        ) && isInTeam(swiss.teamId)
      )
        mongo.player // try a rejoin first
          .updateField($id(SwissPlayer.makeId(swiss.id, me.id)), SwissPlayer.Fields.absent, false)
          .flatMap { rejoin =>
            fuccess(rejoin.n == 1) >>| { // if the match failed (not the update!), try a join
              verify(swiss, me).dmap(_.accepted && swiss.isEnterable) >>& {
                mongo.player.insert.one(SwissPlayer.make(swiss.id, me, swiss.perfType)) zip
                  mongo.swiss.update.one($id(swiss.id), $inc("nbPlayers" -> 1)) >>-
                  cache.swissCache.clear(swiss.id) inject true
              }
            }
          }
      else fuFalse
    } flatMap { res =>
      recomputeAndUpdateAll(id) inject res
    }

  def gameIdSource(
      swissId: SwissId,
      player: Option[UserId],
      batchSize: Int = 0,
      readPreference: ReadPreference = temporarilyPrimary
  ): Source[GameId, ?] =
    SwissPairing.fields { f =>
      mongo.pairing
        .find($doc(f.swissId -> swissId) ++ player.??(u => $doc(f.players -> u)), $id(true).some)
        .sort($sort asc f.round)
        .batchSize(batchSize)
        .cursor[Bdoc](readPreference)
        .documentSource()
        .mapConcat(_.getAsOpt[GameId]("_id").toList)
    }

  def featuredInTeam(teamId: TeamId): Fu[List[Swiss]] =
    cache.featuredInTeam.get(teamId) flatMap { ids =>
      mongo.swiss.byOrderedIds[Swiss, SwissId](ids)(_.id)
    }

  def visibleByTeam(teamId: TeamId, nbPast: Int, nbSoon: Int): Fu[Swiss.PastAndNext] =
    (nbPast > 0).?? {
      mongo.swiss
        .find($doc("teamId" -> teamId, "finishedAt" $exists true))
        .sort($sort desc "startsAt")
        .cursor[Swiss]()
        .list(nbPast)
    } zip
      (nbSoon > 0).?? {
        mongo.swiss
          .find(
            $doc("teamId" -> teamId, "startsAt" $gt nowInstant.minusWeeks(2), "finishedAt" $exists false)
          )
          .sort($sort asc "startsAt")
          .cursor[Swiss]()
          .list(nbSoon)
      } map
      (Swiss.PastAndNext.apply).tupled

  def playerInfo(swiss: Swiss, userId: UserId): Fu[Option[SwissPlayer.ViewExt]] =
    userRepo byId userId flatMapz { user =>
      mongo.player.byId[SwissPlayer](SwissPlayer.makeId(swiss.id, user.id).value) flatMapz { player =>
        SwissPairing.fields { f =>
          mongo.pairing
            .find($doc(f.swissId -> swiss.id, f.players -> player.userId))
            .sort($sort asc f.round)
            .cursor[SwissPairing]()
            .listAll()
        } flatMap {
          pairingViews(_, player)
        } flatMap { pairings =>
          SwissPlayer.fields { f =>
            mongo.player.countSel($doc(f.swissId -> swiss.id, f.score $gt player.score)).dmap(1.+)
          } map { rank =>
            val pairingMap = pairings.mapBy(_.pairing.round)
            SwissPlayer
              .ViewExt(
                player,
                rank,
                user.light,
                pairingMap,
                SwissSheet.one(swiss, pairingMap.view.mapValues(_.pairing).toMap, player)
              )
              .some
          }
        }

      }

    }

  def pairingViews(pairings: Seq[SwissPairing], player: SwissPlayer): Fu[Seq[SwissPairing.View]] =
    pairings.headOption ?? { first =>
      mongo.player
        .list[SwissPlayer]($inIds(pairings.map(_ opponentOf player.userId).map {
          SwissPlayer.makeId(first.swissId, _)
        }))
        .flatMap { opponents =>
          lightUserApi asyncMany opponents.map(_.userId) map { users =>
            opponents.zip(users) map { case (o, u) =>
              SwissPlayer.WithUser(o, u | LightUser.fallback(o.userId into UserName))
            }
          } map { opponents =>
            pairings flatMap { pairing =>
              opponents.find(_.player.userId == pairing.opponentOf(player.userId)) map {
                SwissPairing.View(pairing, _)
              }
            }
          }
        }
    }

  def searchPlayers(id: SwissId, term: UserStr, nb: Int): Fu[List[UserId]] =
    User.validateId(term) ?? { valid =>
      SwissPlayer.fields { f =>
        mongo.player.primitive[UserId](
          selector = $doc(
            f.swissId -> id,
            f.userId $startsWith valid.value
          ),
          sort = $sort desc f.score,
          nb = nb,
          field = f.userId
        )
      }
    }

  def pageOf(swiss: Swiss, userId: UserId): Fu[Option[Int]] =
    rankingApi(swiss) map {
      _ get userId map { rank =>
        (rank - 1).value / 10 + 1
      }
    }

  def gameView(pov: Pov): Fu[Option[GameView]] =
    (pov.game.swissId ?? cache.swissCache.byId) flatMapz { swiss =>
      getGameRanks(swiss, pov.game) dmap {
        GameView(swiss, _).some
      }

    }

  private def getGameRanks(swiss: Swiss, game: Game): Fu[Option[GameRanks]] =
    game.whitePlayer.userId.ifTrue(swiss.isStarted) ?? { whiteId =>
      game.blackPlayer.userId ?? { blackId =>
        rankingApi(swiss) map { ranking =>
          import cats.syntax.all.*
          (ranking.get(whiteId), ranking.get(blackId)) mapN GameRanks.apply
        }
      }
    }

  private[swiss] def leaveTeam(teamId: TeamId, userId: UserId) =
    joinedPlayableSwissIds(userId, List(teamId))
      .flatMap { kickFromSwissIds(userId, _) }

  private[swiss] def kickLame(userId: UserId) =
    Bus
      .ask[List[TeamId]]("teamJoinedBy")(lila.hub.actorApi.team.TeamIdsJoinedBy(userId, _))
      .flatMap { joinedPlayableSwissIds(userId, _) }
      .flatMap { kickFromSwissIds(userId, _, forfeit = true) }

  def joinedPlayableSwissIds(userId: UserId, teamIds: List[TeamId]): Fu[List[SwissId]] =
    mongo.swiss
      .aggregateList(100, ReadPreference.secondaryPreferred) { framework =>
        import framework.*
        Match($doc("teamId" $in teamIds, "featurable" -> true)) -> List(
          PipelineOperator(
            $lookup.pipeline(
              as = "player",
              from = mongo.player.name,
              local = "_id",
              foreign = "s",
              pipe = List($doc("$match" -> $doc("u" -> userId)))
            )
          ),
          Match("player" $ne $arr()),
          Limit(100),
          Project($id(true))
        )
      }
      .map(_.flatMap(_.getAsOpt[SwissId]("_id")))

  private def kickFromSwissIds(userId: UserId, swissIds: Seq[SwissId], forfeit: Boolean = false): Funit =
    swissIds.map { withdraw(_, userId, forfeit) }.parallel.void

  def withdraw(id: SwissId, userId: UserId, forfeit: Boolean = false): Funit =
    Sequencing(id)(cache.swissCache.notFinishedById) { swiss =>
      SwissPlayer.fields { f =>
        val selId = $id(SwissPlayer.makeId(swiss.id, userId))
        if (swiss.isStarted)
          mongo.player.updateField(selId, f.absent, true) >>
            forfeit.?? { forfeitPairings(swiss, userId) }
        else
          mongo.player.delete.one(selId) flatMap { res =>
            (res.n == 1) ?? {
              mongo.swiss.update.one($id(swiss.id), $inc("nbPlayers" -> -1)).void >>-
                cache.swissCache.clear(swiss.id)
            }
          }
      }.void
    } >> recomputeAndUpdateAll(id)

  private def forfeitPairings(swiss: Swiss, userId: UserId): Funit =
    SwissPairing.fields { F =>
      mongo.pairing
        .list[SwissPairing]($doc(F.swissId -> swiss.id, F.players -> userId))
        .flatMap {
          _.filter(p => p.isDraw || p.winner.has(userId))
            .map { pairing =>
              mongo.pairing.update.one($id(pairing.id), pairing forfeit userId)
            }
            .parallel
            .void
        }
    }

  private[swiss] def finishGame(game: Game): Funit =
    game.swissId ?? { swissId =>
      Sequencing(swissId)(cache.swissCache.byId) { swiss =>
        if (!swiss.isStarted)
          logger.info(s"Removing pairing ${game.id} finished after swiss ${swiss.id}")
          mongo.pairing.delete.one($id(game.id)) inject false
        else
          mongo.pairing
            .updateField(
              $id(game.id),
              SwissPairing.Fields.status,
              Right(game.winnerColor): SwissPairing.Status
            )
            .flatMap { result =>
              if (result.nModified == 0) fuccess(false) // dedup
              else
                {
                  if (swiss.nbOngoing > 0)
                    mongo.swiss.update.one($id(swiss.id), $inc("nbOngoing" -> -1))
                  else
                    fuccess {
                      logger.warn(s"swiss ${swiss.id} nbOngoing = ${swiss.nbOngoing}")
                    }
                } >>
                  game.playerWhoDidNotMove.flatMap(_.userId).?? { absent =>
                    SwissPlayer.fields { f =>
                      mongo.player
                        .updateField($doc(f.swissId -> swiss.id, f.userId -> absent), f.absent, true)
                        .void
                    }
                  } >> {
                    (swiss.nbOngoing <= 1) ?? {
                      if (swiss.round.value == swiss.settings.nbRounds) doFinish(swiss)
                      else if (swiss.settings.manualRounds) fuccess {
                        systemChat(swiss.id, s"Round ${swiss.round.value + 1} needs to be scheduled.")
                      }
                      else
                        mongo.swiss
                          .updateField(
                            $id(swiss.id),
                            "nextRoundAt",
                            swiss.settings.dailyInterval match {
                              case Some(days) => game.createdAt plusDays days
                              case None =>
                                nowInstant.plusSeconds(swiss.settings.roundInterval.toSeconds.toInt)
                            }
                          )
                          .void >>-
                          systemChat(swiss.id, s"Round ${swiss.round.value + 1} will start soon.")
                    }
                  } inject true
            } >>- cache.swissCache.clear(swiss.id)
      }.flatMap {
        if _ then recomputeAndUpdateAll(swissId) >> banApi.onGameFinish(game)
        else funit
      }
    }

  private[swiss] def destroy(swiss: Swiss): Funit =
    mongo.swiss.delete.one($id(swiss.id)) >>
      mongo.pairing.delete.one($doc(SwissPairing.Fields.swissId -> swiss.id)) >>
      mongo.player.delete.one($doc(SwissPairing.Fields.swissId -> swiss.id)).void >>-
      cache.swissCache.clear(swiss.id) >>-
      socket.reload(swiss.id)

  private[swiss] def finish(oldSwiss: Swiss): Funit =
    Sequencing(oldSwiss.id)(cache.swissCache.startedById) { swiss =>
      mongo.pairing.exists($doc(SwissPairing.Fields.swissId -> swiss.id)) flatMap {
        if (_) doFinish(swiss)
        else destroy(swiss)
      }
    }
  private def doFinish(swiss: Swiss): Funit =
    SwissPlayer
      .fields { f =>
        mongo.player.primitiveOne[UserId]($doc(f.swissId -> swiss.id), $sort desc f.score, f.userId)
      }
      .flatMap { winnerUserId =>
        mongo.swiss.update
          .one(
            $id(swiss.id),
            $unset("nextRoundAt", "lastRoundAt", "featurable") ++ $set(
              "settings.n" -> swiss.round,
              "finishedAt" -> nowInstant,
              "winnerId"   -> winnerUserId
            )
          )
          .void zip
          SwissPairing.fields { f =>
            mongo.pairing.delete.one($doc(f.swissId -> swiss.id, f.status -> true)) map { res =>
              if (res.n > 0) logger.warn(s"Swiss ${swiss.id} finished with ${res.n} ongoing pairings")
            }
          } void
      } >>- {
      systemChat(swiss.id, s"Tournament completed!")
      cache.swissCache clear swiss.id
      socket.reload(swiss.id)
      scheduler
        .scheduleOnce(10 seconds) {
          // we're delaying this to make sure the ranking has been recomputed
          // since doFinish is called by finishGame before that
          rankingApi(swiss) foreach { ranking =>
            Bus.publish(SwissFinish(swiss.id, ranking), "swissFinish")
          }
        }
        .unit
    }

  def kill(swiss: Swiss): Funit = {
    if (swiss.isStarted)
      finish(swiss) >>- {
        logger.info(s"Tournament ${swiss.id} cancelled by its creator.")
        systemChat(swiss.id, "Tournament cancelled by its creator.")
      }
    else if (swiss.isCreated) destroy(swiss)
    else funit
  } >>- cache.featuredInTeam.invalidate(swiss.teamId)

  def roundInfo = cache.roundInfo.get

  def byTeamCursor(teamId: TeamId) =
    mongo.swiss
      .find($doc("teamId" -> teamId))
      .sort($sort desc "startsAt")
      .cursor[Swiss]()

  def teamOf(id: SwissId): Fu[Option[TeamId]] =
    mongo.swiss.primitiveOne[TeamId]($id(id), "teamId")

  private def recomputeAndUpdateAll(id: SwissId): Funit =
    scoring(id).flatMapz { res =>
      rankingApi.update(res)
      standingApi.update(res) >>
        boardApi.update(res) >>-
        socket.reload(id)
    }

  private[swiss] def startPendingRounds: Funit =
    mongo.swiss
      .find($doc("nextRoundAt" $lt nowInstant), $id(true).some)
      .cursor[Bdoc]()
      .list(10)
      .map(_.flatMap(_.getAsOpt[SwissId]("_id")))
      .flatMap { ids =>
        lila.common.LilaFuture.applySequentially(ids) { id =>
          Sequencing(id)(cache.swissCache.notFinishedById) { swiss =>
            if (swiss.round.value >= swiss.settings.nbRounds) doFinish(swiss)
            else if (swiss.nbPlayers >= 2)
              countPresentPlayers(swiss) flatMap { nbPresent =>
                if (nbPresent < 2)
                  systemChat(swiss.id, "Not enough players left.")
                  doFinish(swiss)
                else
                  director.startRound(swiss).flatMap {
                    _.fold {
                      systemChat(swiss.id, "All possible pairings were played.")
                      doFinish(swiss)
                    } {
                      case s if s.nextRoundAt.isEmpty =>
                        systemChat(s.id, s"Round ${s.round.value} started.")
                        funit
                      case s =>
                        systemChat(s.id, s"Round ${s.round.value} failed.", volatile = true)
                        mongo.swiss.update
                          .one($id(s.id), $set("nextRoundAt" -> nowInstant.plusSeconds(61)))
                          .void
                    }
                  } >>- cache.swissCache.clear(swiss.id)
              }
            else if (swiss.startsAt isBefore nowInstant.minusMinutes(60)) destroy(swiss)
            else
              systemChat(swiss.id, "Not enough players for first round; delaying start.", volatile = true)
              mongo.swiss.update
                .one($id(swiss.id), $set("nextRoundAt" -> nowInstant.plusSeconds(121)))
                .void >>- cache.swissCache.clear(swiss.id)
          } >> recomputeAndUpdateAll(id)
        }
      }
      .monSuccess(_.swiss.tick)

  private def countPresentPlayers(swiss: Swiss) = SwissPlayer.fields { f =>
    mongo.player.countSel($doc(f.swissId -> swiss.id, f.absent $ne true))
  }

  private[swiss] def checkOngoingGames: Funit =
    SwissPairing
      .fields { f =>
        mongo.pairing
          .aggregateList(100) { framework =>
            import framework.*
            Match($doc(f.status -> SwissPairing.ongoing)) -> List(
              GroupField(f.swissId)("ids" -> PushField(f.id)),
              Limit(100)
            )
          }
      }
      .map {
        _.flatMap { doc =>
          for {
            swissId <- doc.getAsOpt[SwissId]("_id")
            gameIds <- doc.getAsOpt[List[GameId]]("ids")
          } yield swissId -> gameIds
        }
      }
      .flatMap {
        _.map { case (swissId, gameIds) =>
          Sequencing[List[Game]](swissId)(cache.swissCache.byId) { _ =>
            roundSocket.getGames(gameIds) map { pairs =>
              val games               = pairs.collect { case (_, Some(g)) => g }
              val (finished, ongoing) = games.partition(_.finishedOrAborted)
              val flagged             = ongoing.filter(_ outoftime true)
              val missingIds          = pairs.collect { case (id, None) => id }
              lila.mon.swiss.games("finished").record(finished.size)
              lila.mon.swiss.games("ongoing").record(ongoing.size)
              lila.mon.swiss.games("flagged").record(flagged.size)
              lila.mon.swiss.games("missing").record(missingIds.size)
              if (flagged.nonEmpty)
                Bus.publish(lila.hub.actorApi.map.TellMany(flagged.map(_.id.value), QuietFlag), "roundSocket")
              if (missingIds.nonEmpty)
                mongo.pairing.delete.one($inIds(missingIds))
              finished
            }
          } flatMap {
            _.map(finishGame).parallel.void
          }
        }.parallel.void
      }

  private def systemChat(id: SwissId, text: String, volatile: Boolean = false): Unit =
    chatApi.userChat.service(id into ChatId, text, _.Swiss, isVolatile = volatile)

  def withdrawAll(user: User, teamIds: List[TeamId]): Funit =
    mongo.swiss
      .aggregateList(Int.MaxValue, readPreference = ReadPreference.secondaryPreferred) { implicit framework =>
        import framework.*
        Match($doc("finishedAt" $exists false, "nbPlayers" $gt 0, "teamId" $in teamIds)) -> List(
          PipelineOperator(
            $lookup.pipelineFull(
              from = mongo.player.name,
              let = $doc("s" -> "$_id"),
              as = "player",
              pipe = List(
                $doc(
                  "$match" -> $expr(
                    $and(
                      $doc("$eq" -> $arr("$u", user.id)),
                      $doc("$eq" -> $arr("$s", "$$s"))
                    )
                  )
                )
              )
            )
          ),
          Match("player" $ne $arr()),
          Project($id(true))
        )
      }
      .map(_.flatMap(_.getAsOpt[SwissId]("_id")))
      .flatMap {
        _.map { withdraw(_, user.id) }.parallel.void
      }

  def isUnfinished(id: SwissId): Fu[Boolean] =
    mongo.swiss.exists($id(id) ++ $doc("finishedAt" $exists false))

  def filterPlaying(id: SwissId, userIds: Seq[UserId]): Fu[List[UserId]] =
    userIds.nonEmpty ??
      mongo.swiss.exists($id(id) ++ $doc("finishedAt" $exists false)) flatMapz {
        SwissPlayer.fields { f =>
          mongo.player.distinctEasy[UserId, List](
            f.userId,
            $doc(
              f.id $in userIds.map(SwissPlayer.makeId(id, _)),
              f.absent $ne true
            )
          )
        }
      }

  def resultStream(swiss: Swiss, perSecond: MaxPerSecond, nb: Int): Source[SwissPlayer.WithRank, ?] =
    SwissPlayer.fields { f =>
      mongo.player
        .find($doc(f.swissId -> swiss.id))
        .sort($sort desc f.score)
        .batchSize(perSecond.value)
        .cursor[SwissPlayer](temporarilyPrimary)
        .documentSource(nb)
        .throttle(perSecond.value, 1 second)
        .zipWithIndex
        .map { case (player, index) =>
          SwissPlayer.WithRank(player, index.toInt + 1)
        }
    }

  private val idNameProjection = $doc("name" -> true)

  def idNames(ids: List[SwissId]): Fu[List[Swiss.IdName]] =
    mongo.swiss.find($inIds(ids), idNameProjection.some).cursor[Swiss.IdName]().listAll()

  private def Sequencing[A <: Matchable: Zero](
      id: SwissId
  )(fetch: SwissId => Fu[Option[Swiss]])(run: Swiss => Fu[A]): Fu[A] =
    sequencer(id) {
      fetch(id) flatMapz run
    }
