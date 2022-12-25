package lila.team

import akka.stream.scaladsl.*
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.ReadPreference
import scala.concurrent.duration.*

import lila.common.config.MaxPerSecond
import lila.db.dsl.{ *, given }
import lila.user.{ User, UserRepo }

final class TeamMemberStream(
    memberRepo: MemberRepo,
    userRepo: UserRepo
)(using
    ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer
):

  def apply(team: Team, perSecond: MaxPerSecond): Source[User, ?] =
    idsBatches(team, perSecond)
      .mapAsync(1)(userRepo.usersFromSecondary)
      .mapConcat(identity)

  def subscribedIds(team: Team, perSecond: MaxPerSecond): Source[UserId, ?] =
    idsBatches(team, perSecond, $doc("unsub" $ne true))
      .mapConcat(identity)

  private def idsBatches(
      team: Team,
      perSecond: MaxPerSecond,
      selector: Bdoc = $empty
  ): Source[Seq[UserId], ?] =
    memberRepo.coll
      .find($doc("team" -> team.id) ++ selector, $doc("user" -> true).some)
      .sort($sort desc "date")
      .batchSize(perSecond.value)
      .cursor[Bdoc](temporarilyPrimary)
      .documentSource()
      .grouped(perSecond.value)
      .map(_.flatMap(_.getAsOpt[UserId]("user")))
      .throttle(1, 1 second)
