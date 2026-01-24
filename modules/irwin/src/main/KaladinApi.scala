package lila.irwin

import chess.Speed
import reactivemongo.api.Cursor
import reactivemongo.api.bson.*

import lila.core.perf.UserWithPerfs
import lila.core.report.SuspectId
import lila.core.userId.ModId
import lila.db.AsyncColl
import lila.db.dsl.{ *, given }
import lila.game.{ BinaryFormat, GameRepo }
import lila.memo.CacheApi
import lila.report.{ Mod, Report, Reporter, Suspect }

final class KaladinApi(
    coll: AsyncColl,
    userApi: lila.core.user.UserApi,
    gameRepo: GameRepo,
    cacheApi: CacheApi,
    insightApi: lila.game.core.insight.InsightApi,
    modApi: lila.core.mod.ModApi,
    reportApi: lila.report.ReportApi,
    notifyApi: lila.core.notify.NotifyApi,
    settingStore: lila.memo.SettingStore.Builder
)(using Executor, Scheduler):

  import BSONHandlers.given

  lazy val thresholds = IrwinThresholds.makeSetting("kaladin", settingStore)

  private val workQueue = scalalib.actor.AsyncActorSequencer(
    maxSize = Max(512),
    timeout = 2.minutes,
    name = "kaladinApi",
    lila.log.asyncActorMonitor.full
  )

  private def sequence[A <: Matchable](user: Suspect)(f: Option[KaladinUser] => Fu[A]): Fu[A] =
    workQueue { coll(_.byId[KaladinUser](user.id.value)).flatMap(f) }

  def get(user: User): Fu[Option[KaladinUser]] =
    coll(_.byId[KaladinUser](user.id))

  def dashboard: Fu[KaladinUser.Dashboard] = for
    c <- coll.get
    completed <- c
      .find($doc("response.at".$exists(true)))
      .sort($doc("response.at" -> -1))
      .cursor[KaladinUser]()
      .list(30)
    queued <- c
      .find($doc("response.at".$exists(false)))
      .sort($doc("queuedAt" -> -1))
      .cursor[KaladinUser]()
      .list(30)
  yield KaladinUser.Dashboard(completed ::: queued)

  def modRequest(user: Suspect)(using me: Me) =
    for _ <- request(user, KaladinUser.Requester.Mod(me))
    yield notification.add(user.id)

  private def request(sus: Suspect, requester: KaladinUser.Requester) = sus.user.noBot.so:
    sequence(sus):
      _.fold(KaladinUser.make(sus, requester).some)(_.queueAgain(requester))
        .so: req =>
          for
            user <- userApi.withPerfs(sus.user)
            enoughMoves <- hasEnoughRecentMoves(user)
            _ <-
              if enoughMoves then
                lila.mon.mod.kaladin.request(requester.name).increment()
                insightApi.indexAll(user.user) >>
                  coll(_.update.one($id(req.id), req, upsert = true)).void
              else
                lila.mon.mod.kaladin.insufficientMoves(requester.name).increment()
                funit
          yield ()

  private[irwin] def readResponses: Funit =
    coll: coll =>
      // hits a mongodb index
      // db.kaladin_queue.createIndex({'response.at':1,'response.read':1},{partialFilterExpression:{'response.at':{$exists:true}}})
      coll
        .find($doc("response.at".$exists(true), "response.read" -> false))
        .sort($doc("response.at" -> 1))
        .hint(coll.hint("response.at_1_response.read_1"))
        .cursor[KaladinUser]()
        .list(50)
        .flatMap: docs =>
          docs.nonEmpty.so:
            coll.update.one($inIds(docs.map(_.id)), $set("response.read" -> true), multi = true) >>
              docs.sequentiallyVoid(readResponse)
        .void

  private def readResponse(user: KaladinUser): Funit = user.response.so: res =>
    res.pred match
      case Some(pred) =>
        for _ <- markOrReport(user, pred)
        yield
          notification(user)
          lila.mon.mod.kaladin.activation.record(pred.percent)
      case None =>
        fuccess:
          res.err.foreach: err =>
            lila.mon.mod.kaladin.error(err).increment()

  private def markOrReport(user: KaladinUser, pred: KaladinUser.Pred): Funit =

    def sendReport = for
      suspect <- getSuspect(user.suspectId.value)
      kaladin <- userApi.byId(UserId.kaladin).orFail("Kaladin user not found").dmap(Mod.apply)
      _ <- reportApi.create(
        Report
          .Candidate(
            reporter = Reporter(kaladin.user),
            suspect = suspect,
            reason = lila.report.Reason.Cheat,
            text = pred.note
          )
      )
    yield
      lila.mon.mod.kaladin.report.increment()
      ()

    if pred.percent >= thresholds.get().mark then
      userApi
        .getTitle(user.id)
        .dmap(_.isDefined)
        .flatMap:
          if _ then sendReport
          else
            for _ <- modApi.autoMark(user.suspectId, pred.note)(using UserId.kaladin.into(MyId))
            yield lila.mon.mod.kaladin.mark.increment()
    else if pred.percent >= thresholds.get().report then sendReport
    else funit

  object notification:

    private var subs = Map.empty[SuspectId, Set[ModId]]

    def add(suspectId: SuspectId)(using me: Me): Unit =
      subs = subs.updated(suspectId, ~subs.get(suspectId) + me.modId)

    private[KaladinApi] def apply(user: KaladinUser): Funit =
      subs.get(user.suspectId).so { modIds =>
        subs = subs - user.suspectId
        modIds.toList.sequentiallyVoid: modId =>
          val notif = lila.core.notify.NotificationContent.KaladinDone(user.suspectId.value)
          notifyApi.notifyOne(modId, notif)
      }

  private[irwin] def monitorQueued: Funit =
    coll {
      _.aggregateList(Int.MaxValue, _.sec): framework =>
        import framework.*
        Match($doc("response.at".$exists(false))) -> List(GroupField("priority")("nb" -> SumAll))
      .map: res =>
        for
          obj <- res
          priority <- obj.int("_id")
          nb <- obj.int("nb")
        yield (priority, nb)
    }.map:
      _.foreach: (priority, nb) =>
        lila.mon.mod.kaladin.queue(priority).update(nb)

  private object hasEnoughRecentMoves:
    private val minMoves = 1050
    private case class Counter(blitz: Int, rapid: Int):
      def add(nb: Int, speed: Speed) =
        if speed == Speed.Blitz then copy(blitz = blitz + nb)
        else if speed == Speed.Rapid then copy(rapid = rapid + nb)
        else this
      def isEnough = blitz >= minMoves || rapid >= minMoves
    private val cache = cacheApi[UserId, Boolean](1024, "kaladin.hasEnoughRecentMoves"):
      _.expireAfterWrite(1.hour).buildAsyncFuture(userId =>
        {
          import lila.game.Query
          import lila.game.Game.BSONFields as F
          import lila.db.ByteArray.given
          gameRepo.coll
            .find(
              Query.user(userId) ++ Query.rated ++ Query.createdSince(nowInstant.minusMonths(6)),
              $doc(F.turns -> true, F.clock -> true).some
            )
            .cursor[Bdoc](ReadPref.sec)
            .foldWhile(Counter(0, 0)):
              case (counter, doc) =>
                val next = (for
                  clockBin <- doc.getAsOpt[BSONBinary](F.clock)
                  clockConfig <- BinaryFormat.clock.readConfig(clockBin.byteArray)
                  speed = Speed(clockConfig)
                  if speed == Speed.Blitz || speed == Speed.Rapid
                  moves <- doc.int(F.turns)
                yield counter.add(moves / 2, speed)) | counter
                if next.isEnough then Cursor.Done(next)
                else Cursor.Cont(next)

        }.dmap(_.isEnough)
      )
    def apply(u: UserWithPerfs): Fu[Boolean] =
      fuccess(u.perfs.blitz.nb + u.perfs.rapid.nb > 30) >>& cache.get(u.id)

  private[irwin] def autoRequest(requester: KaladinUser.Requester)(user: Suspect) =
    request(user, requester)

  private[irwin] def tournamentLeaders(suspects: List[Suspect]): Funit =
    suspects.sequentiallyVoid(autoRequest(KaladinUser.Requester.TournamentLeader))

  private[irwin] def topOnline(suspects: List[Suspect]): Funit =
    suspects.sequentiallyVoid(autoRequest(KaladinUser.Requester.TopOnline))

  private def getSuspect(suspectId: UserId) =
    userApi.byId(suspectId).orFail(s"suspect $suspectId not found").dmap(Suspect.apply)

  lila.common.Bus.sub[lila.core.report.CheatReportCreated]: rep =>
    getSuspect(rep.userId).flatMap(autoRequest(KaladinUser.Requester.Report))
