package lila.irwin

import com.softwaremill.tagging._
import org.joda.time.DateTime
import reactivemongo.api.bson._
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._

import lila.db.dsl._
import lila.report.{ Suspect }
import lila.user.Holder

final class KaladinApi(coll: Coll @@ KaladinColl)(implicit
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
    sequence[Unit](user) { prev =>
      val request: Option[KaladinUser] = prev match {
        case Some(prev) if prev.recentlyQueued => none
        case Some(prev)                        => prev.queueAgain(by).some
        case _                                 => KaladinUser.make(user, by).some
      }
      request ?? { req =>
        lila.mon.mod.kaladin.request.increment()
        coll.update.one($id(req._id), req, upsert = true).void
      }
    }
}
