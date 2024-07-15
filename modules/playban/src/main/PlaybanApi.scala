package lila.playban

import chess.{ Centis, Color, Status }
import reactivemongo.api.bson.*
import scalalib.model.Days

import lila.common.{ Bus, Uptime }
import lila.core.game.Source
import lila.core.msg.MsgApi
import lila.core.playban.RageSit as RageSitCounter
import lila.db.dsl.{ *, given }

final class PlaybanApi(
    coll: Coll,
    feedback: PlaybanFeedback,
    gameApi: lila.core.game.GameApi,
    userApi: lila.core.user.UserApi,
    noteApi: lila.core.user.NoteApi,
    cacheApi: lila.memo.CacheApi,
    messenger: MsgApi
)(using ec: Executor, mode: play.api.Mode):

  private val sittingAutoPreset = lila.core.msg.MsgPreset(
    name = "Warning: leaving games / stalling on time",
    text =
      """In your game history, you have several games where you have left the game or just let the time run out instead of playing or resigning.
  This can be very annoying for your opponents. If this behavior continues to happen, we may be forced to terminate your account."""
  )

  private given BSONHandler[Outcome] = tryHandler(
    { case BSONInteger(v) => Outcome(v).toTry(s"No such playban outcome: $v") },
    x => BSONInteger(x.id)
  )
  private given BSONDocumentHandler[TempBan]    = Macros.handler
  private given BSONDocumentHandler[UserRecord] = Macros.handler

  private val blameableSources: Set[Source] = Set(Source.Lobby, Source.Pool, Source.Arena)

  private def blameable(game: Game): Fu[Boolean] =
    (game.source.exists(blameableSources.contains) && game.hasClock).so {
      if game.rated then fuTrue
      else userApi.containsEngine(game.userIds).not
    }

  private def IfBlameable[A: alleycats.Zero](game: Game)(f: => Fu[A]): Fu[A] =
    (mode.notProd || Uptime.startedSinceMinutes(10)).so:
      blameable(game).flatMapz(f)

  def abort(pov: Pov, isOnGame: Set[Color]): Funit =
    IfBlameable(pov.game):
      pov.player.userId
        .ifTrue(isOnGame(pov.opponent.color))
        .so: userId =>
          save(Outcome.Abort, userId, RageSit.Update.Reset, pov.game.source).andDo(feedback.abort(pov))

  def noStart(pov: Pov): Funit =
    IfBlameable(pov.game):
      pov.player.userId.so: userId =>
        save(Outcome.NoPlay, userId, RageSit.Update.Reset, pov.game.source).andDo(feedback.noStart(pov))

  def rageQuit(game: Game, quitterColor: Color): Funit =
    IfBlameable(game):
      game
        .player(quitterColor)
        .userId
        .so: userId =>
          save(Outcome.RageQuit, userId, RageSit.imbalanceInc(game, quitterColor), game.source)
            .andDo(feedback.rageQuit(Pov(game, quitterColor)))

  def flag(game: Game, flaggerColor: Color): Funit =

    def unreasonableTime =
      game.clock.map: c =>
        (c.estimateTotalSeconds / 10).atLeast(30).atMost(3 * 60)

    // flagged after waiting a long time
    def sitting: Option[Funit] =
      for
        userId <- game.player(flaggerColor).userId
        seconds = nowSeconds - game.movedAt.toSeconds
        if unreasonableTime.exists(seconds >= _)
      yield (save(Outcome.Sitting, userId, RageSit.imbalanceInc(game, flaggerColor), game.source) >>
        propagateSitting(game, userId)).andDo(feedback.sitting(Pov(game, flaggerColor)))

    // flagged after waiting a short time;
    // but the previous move used a long time.
    // assumes game was already checked for sitting
    def sitMoving: Option[Funit] =
      game
        .player(flaggerColor)
        .userId
        .ifTrue:
          ~(for
            movetimes    <- gameApi.computeMoveTimes(game, flaggerColor)
            lastMovetime <- movetimes.lastOption
            limit        <- unreasonableTime
          yield lastMovetime.toSeconds >= limit)
        .map: userId =>
          (save(Outcome.SitMoving, userId, RageSit.imbalanceInc(game, flaggerColor), game.source) >>
            propagateSitting(game, userId)).andDo(feedback.sitting(Pov(game, flaggerColor)))

    IfBlameable(game):
      sitting.orElse(sitMoving).getOrElse(good(game, flaggerColor))

  private def propagateSitting(game: Game, userId: UserId): Funit =
    game.tournamentId.so: tourId =>
      rageSitCache.get(userId).map { rageSit =>
        if rageSit.isBad
        then Bus.publish(lila.core.playban.SittingDetected(tourId, userId), "playban")
      }

  def other(game: Game, status: Status.type => Status, winner: Option[Color]): Funit =
    IfBlameable(game) {
      ~(for
        w <- winner
        loser = game.player(!w)
        loserId <- loser.userId
      yield
        if Status.NoStart.is(status) then
          save(Outcome.NoPlay, loserId, RageSit.Update.Reset, game.source)
            .andDo(feedback.noStart(Pov(game, !w)))
        else
          game.clock
            .filter:
              _.remainingTime(loser.color) < Centis(1000) &&
                game.turnOf(loser) &&
                Status.Resign.is(status)
            .map: c =>
              (c.estimateTotalSeconds / 10).atLeast(30).atMost(3 * 60)
            .exists(_ < nowSeconds - game.movedAt.toSeconds)
            .option:
              (save(Outcome.SitResign, loserId, RageSit.imbalanceInc(game, loser.color), game.source) >>
                propagateSitting(game, loserId)).andDo(feedback.sitting(Pov(game, loser.color)))
            .getOrElse:
              good(game, !w)
      )
    }

  private def good(game: Game, loserColor: Color): Funit =
    import chess.variant.*
    val quickResign = game.status == Status.Resign &&
      game.durationSeconds.exists(_ < 60) &&
      game.playedTurns >= game.variant.match
        case Standard | Chess960 | Horde            => 20
        case Antichess | Crazyhouse | KingOfTheHill => 15
        case ThreeCheck | Atomic | RacingKings      => 10

    if quickResign then
      game.userIds.parallelVoid: userId =>
        save(Outcome.Sandbag, userId, RageSit.Update.Noop, game.source)
    else
      game
        .player(loserColor)
        .userId
        .so: userId =>
          save(Outcome.Good, userId, RageSit.redeem(game), game.source)

  // memorize users without any ban to save DB reads
  private val cleanUserIds = scalalib.cache.ExpireSetMemo[UserId](30 minutes)

  def currentBan[U: UserIdOf](user: U): Fu[Option[TempBan]] =
    (!cleanUserIds.get(user.id)).so:
      coll
        .find(
          $doc("_id" -> user.id, "b.0".$exists(true)),
          $doc("_id" -> false, "b" -> $doc("$slice" -> -1)).some
        )
        .one[Bdoc]
        .dmap:
          _.flatMap(_.getAsOpt[List[TempBan]]("b")).so(_.find(_.inEffect))
        .addEffect: ban =>
          if ban.isEmpty then cleanUserIds.put(user.id)

  val HasCurrentPlayban: lila.core.playban.HasCurrentPlayban = userId => currentBan(userId).map(_.isDefined)

  val bansOf: lila.core.playban.BansOf = userIds =>
    coll
      .aggregateList(Int.MaxValue, _.pri): framework =>
        import framework.*
        Match($inIds(userIds) ++ $doc("b".$exists(true))) -> List(
          Project($doc("bans" -> $doc("$size" -> "$b")))
        )
      .map:
        _.flatMap: obj =>
          obj.getAsOpt[UserId]("_id").flatMap { id =>
            obj.getAsOpt[Int]("bans").map { id -> _ }
          }
        .toMap

  def bans(userId: UserId): Fu[Int] = coll
    .aggregateOne(_.sec): framework =>
      import framework.*
      Match($id(userId) ++ $doc("b".$exists(true))) -> List(
        Project($doc("bans" -> $doc("$size" -> "$b")))
      )
    .map { ~_.flatMap { _.getAsOpt[Int]("bans") } }

  val rageSitOf: lila.core.playban.RageSitOf = userId => rageSitCache.get(userId)

  private val rageSitCache = cacheApi[UserId, RageSitCounter](32_768, "playban.ragesit") {
    _.expireAfterAccess(10 minutes)
      .buildAsyncFuture { userId =>
        coll
          .primitiveOne[RageSitCounter]($doc("_id" -> userId, "c".$exists(true)), "c")
          .map(_ | RageSit.empty)
      }
  }

  private def save(
      outcome: Outcome,
      userId: UserId,
      rsUpdate: RageSit.Update,
      source: Option[Source]
  ): Funit = {
    lila.mon.playban.outcome(outcome.key).increment()
    for
      withOutcome <- coll
        .findAndUpdateSimplified[UserRecord](
          selector = $id(userId),
          update = $doc(
            $push("o" -> $doc("$each" -> List(outcome), "$slice" -> -30)) ++ {
              rsUpdate match
                case RageSit.Update.Reset            => $min("c" -> 0)
                case RageSit.Update.Inc(v) if v != 0 => $inc("c" -> v)
                case _                               => $empty
            }
          ),
          fetchNewObject = true,
          upsert = true
        )
        .orFail(s"can't find newly created record for user $userId")
      withBan <-
        if outcome == Outcome.Good then fuccess(withOutcome)
        else
          for
            age     <- userApi.accountAge(userId)
            withBan <- legiferate(withOutcome, age, source)
          yield withBan
      _ <- registerRageSit(withBan, rsUpdate)
    yield ()
  }.void.logFailure(lila.log("playban"))

  private def legiferate(record: UserRecord, age: Days, source: Option[Source]): Fu[UserRecord] =
    record
      .bannable(age)
      .ifFalse(record.banInEffect)
      .so: ban =>
        lila.mon.playban.ban.count.increment()
        lila.mon.playban.ban.mins.record(ban.mins)
        Bus.publish(
          lila.core.playban.Playban(record.userId, ban.mins, inTournament = source.has(Source.Arena)),
          "playban"
        )
        coll
          .findAndUpdateSimplified[UserRecord](
            selector = $id(record.userId),
            update = $unset("o") ++ $push(
              "b" -> $doc(
                "$each"  -> List(ban),
                "$slice" -> -30
              )
            ),
            fetchNewObject = true
          )
      .dmap(_ | record)
      .andDo(cleanUserIds.remove(record.userId))

  private def registerRageSit(record: UserRecord, update: RageSit.Update): Funit =
    update match
      case RageSit.Update.Inc(delta) =>
        rageSitCache.put(record.userId, fuccess(record.rageSit))
        (delta < 0 && record.rageSit.isVeryBad).so:
          messenger.postPreset(record.userId, sittingAutoPreset).void.andDo {
            Bus.publish(
              lila.core.mod.AutoWarning(record.userId, sittingAutoPreset.name),
              "autoWarning"
            )
            if record.rageSit.isLethal && record.banMinutes.exists(_ > 12 * 60) then
              userApi
                .byId(record.userId)
                .flatMapz: user =>
                  noteApi
                    .lichessWrite(user, "Closed for ragesit recidive")
                    .andDo(Bus.publish(lila.core.playban.RageSitClose(user.id), "rageSitClose"))
          }
      case _ => funit
