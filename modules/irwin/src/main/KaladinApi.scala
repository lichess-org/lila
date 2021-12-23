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
import lila.report.{ Suspect }
import lila.tournament.Tournament
import lila.tournament.TournamentTop
import lila.user.Holder
import lila.user.User
import lila.user.UserRepo

final class KaladinApi(
    coll: AsyncColl @@ KaladinColl,
    userRepo: UserRepo,
    gameRepo: GameRepo,
    cacheApi: CacheApi,
    insightApi: lila.insight.InsightApi
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem
) {

  import BSONHandlers._

  private val workQueue =
    new lila.hub.AsyncActorSequencer(maxSize = 512, timeout = 2 minutes, name = "kaladinApi")

  private def sequence[A](user: Suspect)(f: Option[KaladinUser] => Fu[A]): Fu[A] =
    workQueue { coll(_.byId[KaladinUser](user.id.value)) flatMap f }

  def dashboard: Fu[KaladinUser.Dashboard] = for {
    c <- coll.get
    docs <- c
    completed <- c
      .find($doc("response" $exists true))
      .sort($doc("response.at" -> -1))
      .cursor[KaladinUser]()
      .list(30)
    queued <- c
      .find($doc("response" $exists false))
      .sort($doc("queuedAt" -> -1))
      .cursor[KaladinUser]()
      .list(30)
  } yield KaladinUser.Dashboard(completed ::: queued)

  def modRequest(user: Suspect, by: Holder) =
    request(user, KaladinUser.Requester.Mod(by.id))

  def request(user: Suspect, requester: KaladinUser.Requester) =
    sequence[Unit](user) { prev =>
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

  private[irwin] def countQueued: Fu[Map[Int, Int]] =
    coll {
      _.aggregateList(Int.MaxValue, ReadPreference.secondaryPreferred) { framework =>
        import framework._
        Match($doc("response" $exists false)) -> List(GroupField("priority")("nb" -> SumAll))
      }
        .map { res =>
          for {
            obj      <- res
            priority <- obj int "_id"
            nb       <- obj int "nb"
          } yield priority -> nb
        }
    }
      .map(_.toMap)

  private object hasEnoughRecentMoves {
    private val minMoves = 1050
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
            .foldWhile(0) {
              case (nb, doc) => {
                val next = nb + ~(for {
                  clockBin    <- doc.getAsOpt[BSONBinary](F.clock)
                  clockConfig <- BinaryFormat.clock.readConfig(clockBin.byteArray)
                  speed = Speed(clockConfig)
                  if speed == Speed.Blitz || speed == Speed.Rapid
                  moves <- doc.int(F.turns)
                } yield moves / 2)
                if (next > minMoves) Cursor.Done(next)
                else Cursor.Cont(next)
              }
            }
        } dmap (_ >= minMoves)
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
