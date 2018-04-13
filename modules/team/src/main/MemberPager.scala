package lila.team

import play.api.libs.iteratee._
import reactivemongo.play.iteratees.cursorProducer
import scala.concurrent.duration._

import lila.db.dsl._
import lila.user.{ User, UserRepo }

final class MemberPager(coll: Coll)(implicit system: akka.actor.ActorSystem) {

  def stream(team: Team, max: Option[Int]): Enumerator[User] = {
    val query = coll.find($doc("team" -> team.id), $doc("user" -> true))
      .sort($sort desc "date")
    query.copy(options = query.options.batchSize(20))
      .cursor[Bdoc]()
      .bulkEnumerator(maxDocs = max | Int.MaxValue) &>
      lila.common.Iteratee.delay(1 second) &>
      Enumeratee.mapM { docs =>
        UserRepo usersFromSecondary docs.toSeq.flatMap(_.getAs[String]("user"))
      } &>
      Enumeratee.mapConcat(_.toSeq)
  }
}
