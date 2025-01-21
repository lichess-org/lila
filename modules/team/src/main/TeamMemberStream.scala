package lila.team

import akka.stream.scaladsl.*
import reactivemongo.akkastream.cursorProducer

import lila.core.LightUser
import lila.core.perf.UserWithPerfs
import lila.db.dsl.{ *, given }

final class TeamMemberStream(
    memberRepo: TeamMemberRepo,
    userApi: lila.core.user.UserApi,
    lightApi: lila.core.user.LightUserApi
)(using Executor, akka.stream.Materializer):

  def apply(team: Team, fullUser: Boolean): Source[(UserWithPerfs | LightUser, Instant), ?] =
    idsBatches(team, MaxPerSecond(if fullUser then 20 else 50))
      .limit(if fullUser then 1000 else 5000)
      .mapAsync(1): members =>
        val users =
          if fullUser
          then userApi.listWithPerfs(members.view.map(_._1).toList)
          else lightApi.asyncManyFallback(members.view.map(_._1).toList)
        users.map(_.zip(members.map(_._2)))
      .mapConcat(identity)

  def subscribedIds(team: Team, perSecond: MaxPerSecond): Source[UserId, ?] =
    idsBatches(team, perSecond, $doc("unsub".$ne(true)))
      .map(_.map(_._1))
      .mapConcat(identity)

  private def idsBatches(
      team: Team,
      perSecond: MaxPerSecond,
      selector: Bdoc = $empty
  ): Source[Seq[(UserId, Instant)], ?] =
    memberRepo.coll
      .find($doc("team" -> team.id) ++ selector, $doc("user" -> true, "date" -> true).some)
      .sort($sort.desc("date"))
      .batchSize(perSecond.value)
      .cursor[Bdoc](ReadPref.priTemp)
      .documentSource()
      .grouped(perSecond.value)
      .map(_.flatMap(u => u.getAsOpt[UserId]("user").zip(u.getAsOpt[Instant]("date"))))
      .throttle(1, 1.second)
