package lila.team

import akka.stream.scaladsl.*
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.ReadPreference
import scala.concurrent.duration.*
import org.joda.time.DateTime

import lila.common.config.MaxPerSecond
import lila.db.dsl.{ *, given }
import lila.user.{ User, UserRepo }

import scala.concurrent.Future

final class TeamMemberStream(
    memberRepo: MemberRepo,
    userRepo: UserRepo
)(using
    ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer
):

  def apply(team: Team, perSecond: MaxPerSecond): Source[(User, DateTime), ?] =
    idsBatches(team, perSecond)
      //.mapAsync(1)(x => (
      //        userRepo.usersFromSecondary(x.map(y => y._1)), x.map(y => y._2)
      //      ))
      .mapAsync(1)(x => userRepo.usersFromSecondary(x.map(y => y._1)), x.map(y => y._2))
      //.mapAsync(1)(x => userRepo.usersFromSecondary(x.map(y => y._1)))
      .mapConcat(identity)

  def subscribedIds(team: Team, perSecond: MaxPerSecond): Source[UserId, ?] =
    idsBatchesWithoutTime(team, perSecond, $doc("unsub" $ne true))
      .mapConcat(identity)

  private def idsBatches(
      team: Team,
      perSecond: MaxPerSecond,
      selector: Bdoc = $empty
  ): Source[Seq[(UserId, DateTime)], ?] =
    memberRepo.coll
      .find($doc("team" -> team.id) ++ selector, $doc("user" -> true, "date" -> true).some)
      .sort($sort desc "date")
      .batchSize(perSecond.value)
      .cursor[Bdoc](temporarilyPrimary)
      .documentSource()
      .grouped(perSecond.value)
      .map(_.flatMap(u => u.getAsOpt[UserId]("user").zip(u.getAsOpt[DateTime]("date"))))
      .throttle(1, 1 second)

  private def idsBatchesWithoutTime(
      team: Team,
      perSecond: MaxPerSecond,
      selector: Bdoc = $empty
  ): Source[Seq[UserId], ?] =
    memberRepo.coll
      .find($doc("team" -> team.id) ++ selector, $doc("user" -> true, "date" -> true).some)
      .sort($sort desc "date")
      .batchSize(perSecond.value)
      .cursor[Bdoc](temporarilyPrimary)
      .documentSource()
      .grouped(perSecond.value)
      .map(_.flatMap(_.getAsOpt[UserId]("user")))
      .throttle(1, 1 second)
