package lila.api

import lila.common.Bus
import lila.security.Granter
import lila.user.Me
import lila.user.{ User, Me }

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
    pushEnv: lila.push.Env,
    streamerApi: lila.streamer.StreamerApi,
    reportApi: lila.report.ReportApi,
    modApi: lila.mod.ModApi,
    modLogApi: lila.mod.ModlogApi,
    appealApi: lila.appeal.AppealApi,
    activityWrite: lila.activity.ActivityWriteApi,
    email: lila.mailer.AutomaticEmail
)(using Executor):

  Bus.subscribeFuns(
    "garbageCollect" -> { case lila.hub.actorApi.security.GarbageCollect(userId) =>
      (modApi.garbageCollect(userId) >> lichessClose(userId)).unit
    },
    "rageSitClose" -> { case lila.hub.actorApi.playban.RageSitClose(userId) => lichessClose(userId).unit }
  )

  def close(u: User)(using me: Me): Funit = for
    playbanned <- playbanApi.hasCurrentBan(u)
    selfClose = me is u
    modClose  = !selfClose && Granter(_.CloseAccount)
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
    _       <- pushEnv.webSubscriptionApi.unsubscribeByUser(u)
    _       <- pushEnv.unregisterDevices(u)
    _       <- streamerApi.demote(u.id)
    reports <- reportApi.processAndGetBySuspect(lila.report.Suspect(u))
    _ <-
      if (selfClose) modLogApi.selfCloseAccount(u.id, reports)
      else modLogApi.closeAccount(u.id)
    _ <- appealApi.onAccountClose(u)
    _ <- u.marks.troll so relationApi.fetchFollowing(u.id).flatMap {
      activityWrite.unfollowAll(u, _)
    }
  yield Bus.publish(lila.hub.actorApi.security.CloseAccount(u.id), "accountClose")

  private def lichessClose(userId: UserId) =
    userRepo.lichessAnd(userId) flatMapz { (lichess, user) => close(user)(using Me(lichess)) }

  def eraseClosed(username: UserId): Fu[Either[String, String]] =
    userRepo byId username map {
      case None => Left("No such user.")
      case Some(user) =>
        userRepo setEraseAt user
        email gdprErase user
        lila.common.Bus.publish(lila.user.User.GDPRErase(user), "gdprErase")
        Right(s"Erasing all data about $username in 24h")
    }

  def closeThenErase(username: UserStr)(using Me): Fu[Either[String, String]] =
    userRepo byId username flatMap {
      case None    => fuccess(Left("No such user."))
      case Some(u) => (u.enabled.yes so close(u)) >> eraseClosed(u.id)
    }
