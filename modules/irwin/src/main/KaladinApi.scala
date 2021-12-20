package lila.irwin

import com.softwaremill.tagging._
import org.joda.time.DateTime
import reactivemongo.api.bson._
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._

import lila.db.dsl._
import lila.report.{ Suspect }
import lila.tournament.Tournament
import lila.tournament.TournamentTop
import lila.user.Holder
import lila.user.User
import lila.user.UserRepo

final class KaladinApi(coll: Coll @@ KaladinColl, userRepo: UserRepo)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem
) {

  import BSONHandlers._

  private val workQueue =
    new lila.hub.AsyncActorSequencer(maxSize = 256, timeout = 5 seconds, name = "kaladinApi")

  private def sequence[A](user: Suspect)(f: Option[KaladinUser] => Fu[A]): Fu[A] =
    workQueue { coll.byId[KaladinUser](user.id.value) flatMap f }

  def dashboard: Fu[KaladinUser.Dashboard] = for {
    completed <- coll
      .find($doc("response.at" $exists true))
      .sort($doc("response.at" -> -1))
      .cursor[KaladinUser]()
      .list(30)
    queued <- coll
      .find($doc("response.at" $exists false))
      .sort($doc("queuedAt" -> -1))
      .cursor[KaladinUser]()
      .list(30)
  } yield KaladinUser.Dashboard(completed ::: queued)

  def modRequest(user: Suspect, by: Holder) =
    request(user, KaladinUser.Requester.Mod(by.id))

  def request(user: Suspect, requester: KaladinUser.Requester) =
    sequence[Unit](user) { prev =>
      prev.fold(KaladinUser.make(user, requester).some)(_.queueAgain(requester)) ?? { req =>
        lila.mon.mod.kaladin.request(requester.name).increment()
        coll.update.one($id(req._id), req, upsert = true).void
      }
    }

  private[irwin] def autoRequest(requester: KaladinUser.Requester)(user: Suspect) =
    request(user, requester)

  private[irwin] def tournamentLeaders(suspects: List[Suspect]): Funit =
    lila.common.Future.applySequentially(suspects)(autoRequest(KaladinUser.Requester.TournamentLeader))

  private[irwin] def topOnline(suspects: List[Suspect]): Funit =
    lila.common.Future.applySequentially(suspects)(autoRequest(KaladinUser.Requester.TopOnline))

  private def getSuspect(suspectId: User.ID) =
    userRepo byId suspectId orFail s"suspect $suspectId not found" dmap Suspect.apply

  lila.common.Bus.subscribeFun("cheatReport") { case lila.hub.actorApi.report.CheatReportCreated(userId) =>
    getSuspect(userId) flatMap autoRequest(KaladinUser.Requester.Report) unit
  }
}
