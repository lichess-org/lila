package lila.team

import akka.stream.scaladsl._
import reactivemongo.akkastream.{ AkkaStreamCursor, cursorProducer }
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._

import lila.common.config.MaxPerSecond
import lila.db.dsl._
import lila.user.{ User, UserRepo }

final class TeamMemberStream(
    memberRepo: MemberRepo,
    userRepo: UserRepo
)(implicit mat: akka.stream.Materializer) {

  def apply(team: Team, perSecond: MaxPerSecond): Source[User, _] =
    memberRepo.coll
      .ext.find($doc("team" -> team.id), $doc("user" -> true))
      .sort($sort desc "date")
      .batchSize(perSecond.value)
      .cursor[Bdoc](ReadPreference.secondaryPreferred)
      .documentSource()
      .grouped(perSecond.value)
      .delay(1 second)
      .mapAsync(1) { docs =>
        userRepo usersFromSecondary docs.toSeq.flatMap(_.getAsOpt[User.ID]("user"))
      }
      .mapConcat(identity)
}
