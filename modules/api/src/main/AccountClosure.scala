package lila.api

import akka.actor.Scheduler
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.Bus
import lila.security.Granter
import lila.user.Holder
import lila.user.User

final class AccountClosure(
    userRepo: lila.user.UserRepo,
    playbanApi: lila.playban.PlaybanApi,
    relationApi: lila.relation.RelationApi,
    rankingApi: lila.user.RankingApi,
    teamApi: lila.team.TeamApi,
    challengeApi: lila.challenge.ChallengeApi,
    tournamentApi: lila.tournament.TournamentApi,
    swissApi: lila.swiss.SwissApi,
    planApi: lila.plan.PlanApi,
    seekApi: lila.lobby.SeekApi,
    securityStore: lila.security.Store,
    webSubscriptionApi: lila.push.WebSubscriptionApi,
    streamerApi: lila.streamer.StreamerApi,
    reportApi: lila.report.ReportApi,
    modApi: lila.mod.ModApi,
    modLogApi: lila.mod.ModlogApi,
    appealApi: lila.appeal.AppealApi,
    activityWrite: lila.activity.ActivityWriteApi,
    email: lila.mailer.AutomaticEmail
)(implicit ec: ExecutionContext, scheduler: Scheduler) {

  Bus.subscribeFuns(
    "garbageCollect" -> { case lila.hub.actorApi.security.GarbageCollect(userId) =>
      (modApi.garbageCollect(userId) >> lichessClose(userId)).unit
    },
    "rageSitClose" -> { case lila.hub.actorApi.playban.RageSitClose(userId) => lichessClose(userId).unit }
  )

  def close(u: User, by: Holder): Funit =
    for {
      playbanned <- playbanApi.hasCurrentBan(u.id)
      selfClose = u.id == by.id
      modClose  = !selfClose && Granter(_.CloseAccount)(by.user)
      badApple  = u.lameOrTrollOrAlt || modClose
      _       <- userRepo.disable(u, keepEmail = badApple || playbanned)
      _       <- relationApi.unfollowAll(u.id)
      _       <- rankingApi.remove(u.id)
      teamIds <- teamApi.quitAllOnAccountClosure(u.id)
      _       <- challengeApi.removeByUserId(u.id)
      _       <- tournamentApi.withdrawAll(u)
      _       <- swissApi.withdrawAll(u, teamIds)
      _       <- planApi.cancel(u).recoverDefault
      _       <- seekApi.removeByUser(u)
      _       <- securityStore.closeAllSessionsOf(u.id)
      _       <- webSubscriptionApi.unsubscribeByUser(u)
      _       <- streamerApi.demote(u.id)
      reports <- reportApi.processAndGetBySuspect(lila.report.Suspect(u))
      _ <- if (selfClose) modLogApi.selfCloseAccount(u.id, reports) else modLogApi.closeAccount(by.id, u.id)
      _ <- appealApi.onAccountClose(u)
      _ <- u.marks.troll ?? relationApi.fetchFollowing(u.id).flatMap {
        activityWrite.unfollowAll(u, _)
      }
    } yield Bus.publish(lila.hub.actorApi.security.CloseAccount(u.id), "accountClose")

  private def lichessClose(userId: User.ID) =
    userRepo.lichessAnd(userId) flatMap {
      _ ?? { case (lichess, user) => close(user, lichess) }
    }

  def eraseClosed(username: String): Fu[Either[String, String]] =
    userRepo named username map {
      case None => Left("No such user.")
      case Some(user) =>
        userRepo setEraseAt user
        email gdprErase user
        lila.common.Bus.publish(lila.user.User.GDPRErase(user), "gdprErase")
        Right(s"Erasing all data about $username in 24h")
    }

  def closeThenErase(username: String, by: Holder): Fu[Either[String, String]] =
    userRepo named username flatMap {
      case None    => fuccess(Left("No such user."))
      case Some(u) => (u.enabled ?? close(u, by)) >> eraseClosed(u.username)
    }
}
