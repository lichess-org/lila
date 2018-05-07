package lila.team

import play.api.libs.iteratee._
import reactivemongo.api.ReadPreference
import reactivemongo.play.iteratees.cursorProducer
import scala.concurrent.duration._

import lila.common.MaxPerSecond
import lila.db.dsl._
import lila.user.{ User, UserRepo }

final class TeamMemberStream(coll: Coll)(implicit system: akka.actor.ActorSystem) {

  def apply(team: Team, perSecond: MaxPerSecond): Enumerator[User] = {
    val query = coll.find($doc("team" -> team.id), $doc("user" -> true))
      .sort($sort desc "date")
    query.copy(options = query.options.batchSize(perSecond.value))
      .cursor[Bdoc](readPreference = ReadPreference.secondaryPreferred)
      .bulkEnumerator() &>
      lila.common.Iteratee.delay(1 second) &>
      Enumeratee.mapM { docs =>
        UserRepo usersFromSecondary docs.toSeq.flatMap(_.getAs[User.ID]("user"))
      } &>
      Enumeratee.mapConcat(_.toSeq)
  }
}
