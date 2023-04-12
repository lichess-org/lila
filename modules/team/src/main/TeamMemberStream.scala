package lila.team

import akka.stream.scaladsl.*
import reactivemongo.akkastream.cursorProducer

import lila.common.config.MaxPerSecond
import lila.db.dsl.{ *, given }
import lila.user.{ User, UserRepo }

final class TeamMemberStream(
    memberRepo: MemberRepo,
    userRepo: UserRepo
)(using Executor, akka.stream.Materializer):

  def apply(team: Team, perSecond: MaxPerSecond): Source[(User, Instant), ?] =
    idsBatches(team, perSecond)
      .mapAsync(1) { members =>
        userRepo.usersFromSecondary(members.map(_._1)).map(_ zip members.map(_._2))
      }
      .mapConcat(identity)

  def subscribedIds(team: Team, perSecond: MaxPerSecond): Source[UserId, ?] =
    idsBatches(team, perSecond, $doc("unsub" $ne true))
      .map(_.map(_._1))
      .mapConcat(identity)

  private def idsBatches(
      team: Team,
      perSecond: MaxPerSecond,
      selector: Bdoc = $empty
  ): Source[Seq[(UserId, Instant)], ?] =
    memberRepo.coll
      .find($doc("team" -> team.id) ++ selector, $doc("user" -> true, "date" -> true).some)
      .sort($sort desc "date")
      .batchSize(perSecond.value)
      .cursor[Bdoc](temporarilyPrimary)
      .documentSource()
      .grouped(perSecond.value)
      .map(_.flatMap(u => u.getAsOpt[UserId]("user").zip(u.getAsOpt[Instant]("date"))))
      .throttle(1, 1 second)
