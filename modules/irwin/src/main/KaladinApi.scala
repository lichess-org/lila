package lila.irwin

import chess.Speed
import com.softwaremill.tagging._
import org.joda.time.DateTime
import reactivemongo.api.bson._
import reactivemongo.api.Cursor
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._

import lila.db.AsyncColl
import lila.db.dsl._
import lila.game.BinaryFormat
import lila.game.GameRepo
import lila.memo.CacheApi
import lila.report.Mod
import lila.report.ModId
import lila.report.Report
import lila.report.Reporter
import lila.report.Suspect
import lila.tournament.Tournament
import lila.tournament.TournamentTop
import lila.user.Holder
import lila.user.User
import lila.user.UserRepo
import lila.report.SuspectId

final class KaladinApi(
    coll: AsyncColl @@ KaladinColl,
    userRepo: UserRepo,
    gameRepo: GameRepo,
    cacheApi: CacheApi,
    insightApi: lila.insight.InsightApi,
    modApi: lila.mod.ModApi,
    reportApi: lila.report.ReportApi,
    notifyApi: lila.notify.NotifyApi,
    settingStore: lila.memo.SettingStore.Builder
)(implicit
    ec: scala.concurrent.ExecutionContext,
    scheduler: akka.actor.Scheduler
) {

  import BSONHandlers._

  lazy val thresholds = IrwinThresholds.makeSetting("kaladin", settingStore)

  private val workQueue =
    new lila.hub.AsyncActorSequencer(maxSize = 512, timeout = 2 minutes, name = "kaladinApi")

  private def sequence[A](user: Suspect)(f: Option[KaladinUser] => Fu[A]): Fu[A] =
    workQueue { coll(_.byId[KaladinUser](user.id.value)) flatMap f }

  def get(user: User): Fu[Option[KaladinUser]] =
    coll(_.byId[KaladinUser](user.id))

  def dashboard: Fu[KaladinUser.Dashboard] = for {
    c <- coll.get
    completed <- c
      .find($doc("response.at" $exists true))
      .sort($doc("response.at" -> -1))
      .cursor[KaladinUser]()
      .list(30)
    queued <- c
      .find($doc("response.at" $exists false))
      .sort($doc("queuedAt" -> -1))
      .cursor[KaladinUser]()
      .list(30)
  } yield KaladinUser.Dashboard(completed ::: queued)

  def modRequest(user: Suspect, by: Holder) =
    request(user, KaladinUser.Requester.Mod(by.id)) >>- notification.add(user.id, ModId(by.id))

  def request(user: Suspect, requester: KaladinUser.Requester) = user.user.noBot ??
    sequence(user) { prev =>
      prev.fold(KaladinUser.make(user, requester).some)(_.queueAgain(requester)) ?? { req =>
        hasEnoughRecentMoves(user) flatMap {
          case false =>
            lila.mon.mod.kaladin.insufficientMoves(requester.name).increment()
            funit
          case true =>
            lila.mon.mod.kaladin.request(requester.name).increment()
            insightApi.indexAll(user.user) >>
              coll(_.update.one($id(req._id), req, upsert = true)).void
        }
      }
    }

  private[irwin] def readResponses: Funit =
    coll { coll =>
      // hits a mongodb index
      // db.kaladin_queue.createIndex({'response.at':1,'response.read':1},{partialFilterExpression:{'response.at':{$exists:true}}})
      coll
        .find($doc("response.at" $exists true, "response.read" $ne true))
        .sort($doc("response.at" -> 1))
        .hint(coll hint "response.at_1_response.read_1")
        .cursor[KaladinUser]()
        .list(50)
        .flatMap { docs =>
          docs.nonEmpty ?? {
            coll.update.one($inIds(docs.map(_.id)), $set("response.read" -> true), multi = true) >>
              lila.common.Future.applySequentially(docs)(readResponse)
          }
        }
        .void
    }

  private def readResponse(user: KaladinUser): Funit = user.response ?? { res =>
    res.pred match {
      case Some(pred) =>
        markOrReport(user, res, pred) >>- {
          notification(user)
          lila.mon.mod.kaladin.activation.record(pred.percent).unit
        }
      case None =>
        fuccess {
          res.err foreach { err =>
            lila.mon.mod.kaladin.error(err).increment().unit
          }
        }
    }
  }

  private def markOrReport(user: KaladinUser, res: KaladinUser.Response, pred: KaladinUser.Pred): Funit = {

    def sendReport = for {
      suspect <- getSuspect(user.suspectId.value)
      kaladin <- userRepo.kaladin orFail s"Kaladin user not found" dmap Mod
      _ <- reportApi.create(
        Report
          .Candidate(
            reporter = Reporter(kaladin.user),
            suspect = suspect,
            reason = lila.report.Reason.Cheat,
            text = pred.note
          )
      )
    } yield lila.mon.mod.kaladin.report.increment().unit

    if (pred.percent >= thresholds.get().mark)
      userRepo.hasTitle(user.id) flatMap {
        case true => sendReport
        case false =>
          modApi.autoMark(user.suspectId, ModId.kaladin, pred.note) >>-
            lila.mon.mod.kaladin.mark.increment().unit
      }
    else if (pred.percent >= thresholds.get().report) sendReport
    else funit
  }

  object notification {

    private var subs = Map.empty[SuspectId, Set[ModId]]

    def add(suspectId: SuspectId, modId: ModId): Unit =
      subs = subs.updated(suspectId, ~subs.get(suspectId) + modId)

    private[KaladinApi] def apply(user: KaladinUser): Funit =
      subs.get(user.suspectId) ?? { modIds =>
        subs = subs - user.suspectId
        import lila.notify.{ KaladinDone, Notification }
        modIds
          .map { modId =>
            notifyApi.addNotification(
              Notification.make(Notification.Notifies(modId.value), KaladinDone(user.suspectId.value))
            )
          }
          .sequenceFu
          .void
      }
  }

  private[irwin] def monitorQueued: Funit =
    coll {
      _.aggregateList(Int.MaxValue, ReadPreference.secondaryPreferred) { framework =>
        import framework._
        Match($doc("response.at" $exists false)) -> List(GroupField("priority")("nb" -> SumAll))
      }
        .map { res =>
          for {
            obj      <- res
            priority <- obj int "_id"
            nb       <- obj int "nb"
          } yield (priority, nb)
        }
    } map {
      _ foreach { case (priority, nb) =>
        lila.mon.mod.kaladin.queue(priority).update(nb)
      }
    }

  private object hasEnoughRecentMoves {
    private val minMoves = 1050
    private case class Counter(blitz: Int, rapid: Int) {
      def add(nb: Int, speed: Speed) =
        if (speed == Speed.Blitz) copy(blitz = blitz + nb)
        else if (speed == Speed.Rapid) copy(rapid = rapid + nb)
        else this
      def isEnough = blitz >= minMoves || rapid >= minMoves
    }
    private val cache = cacheApi[User.ID, Boolean](1024, "kaladin.hasEnoughRecentMoves") {
      _.expireAfterWrite(1 hour).buildAsyncFuture(userId =>
        {
          import lila.game.Query
          import lila.game.Game.{ BSONFields => F }
          gameRepo.coll
            .find(
              Query.user(userId) ++ Query.rated ++ Query.createdSince(DateTime.now minusMonths 6),
              $doc(F.turns -> true, F.clock -> true).some
            )
            .cursor[Bdoc](ReadPreference.secondaryPreferred)
            .foldWhile(Counter(0, 0)) {
              case (counter, doc) => {
                val next = (for {
                  clockBin    <- doc.getAsOpt[BSONBinary](F.clock)
                  clockConfig <- BinaryFormat.clock.readConfig(clockBin.byteArray)
                  speed = Speed(clockConfig)
                  if speed == Speed.Blitz || speed == Speed.Rapid
                  moves <- doc.int(F.turns)
                } yield counter.add(moves / 2, speed)) | counter
                if (next.isEnough) Cursor.Done(next)
                else Cursor.Cont(next)
              }
            }
        }.dmap(_.isEnough)
      )
    }
    def apply(u: Suspect): Fu[Boolean] =
      fuccess(u.user.perfs.blitz.nb + u.user.perfs.rapid.nb > 30) >>& cache.get(u.id.value)
  }

  private[irwin] def autoRequest(requester: KaladinUser.Requester)(user: Suspect) =
    request(user, requester)

  private[irwin] def tournamentLeaders(suspects: List[Suspect]): Funit =
    lila.common.Future.applySequentially(suspects)(autoRequest(KaladinUser.Requester.TournamentLeader))

  private[irwin] def topOnline(suspects: List[Suspect]): Funit =
    lila.common.Future.applySequentially(suspects)(autoRequest(KaladinUser.Requester.TopOnline))

  private def getSuspect(suspectId: User.ID) =
    userRepo byId suspectId orFail s"suspect $suspectId not found" dmap Suspect

  lila.common.Bus.subscribeFun("cheatReport") { case lila.hub.actorApi.report.CheatReportCreated(userId) =>
    getSuspect(userId) flatMap autoRequest(KaladinUser.Requester.Report) unit
  }
}
