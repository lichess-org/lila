package lila.api

import lila.common.Bus
import lila.core.perm.Granter

enum Termination:
  case disable, delete, erase

/* There are 3 stages to account termination.
|                           | disable                          | delete                | erase                          |
|---------------------------|----------------------------------|-----------------------|--------------------------------|
| how                       | from settings menu               | from /account/delete  | request to contact@lichess.org |
| reopen                    | available to user                | strictly impossible   | strictly impossible            |
| games                     | intact                           | anonymized            | anonymized                     |
| username                  | intact, no reuse                 | anonymized, no reuse  | anonymized, no reuse           |
| email                     | kept for reopening, no reuse[^1] | deleted, no reuse[^1] | deleted, no reuse[^1]          |
| profile data              | hidden                           | deleted               | deleted                        |
| sessions and oauth tokens | closed                           | deleted               | deleted                        |
| patron subscription       | canceled                         | canceled              | canceled                       |
| blog posts                | unlisted                         | deleted               | deleted                        |
| studies                   | hidden                           | deleted               | deleted                        |
| activity                  | hidden                           | deleted               | deleted                        |
| coach/streamer profiles   | hidden                           | deleted               | deleted                        |
| tournaments joined        | unlisted                         | anonymized            | anonymized                     |
| tournaments created       | hidden                           | anonymized            | anonymized                     |
| forum posts               | intact                           | anonymized            | deleted                        |
| teams/classes joined      | quit                             | quit                  | quit                           |
| team/classes created      | intact[^2]                       | intact[^2]            | intact[^2]                     |
| classes joiated           | intact[^2]                       | intact[^2]            | intact[^2]                     |
| puzzle history            | hidden                           | deleted               | deleted                        |
| follows and blocks        | deleted                          | deleted               | deleted                        |

[^1] the email address of a closed account can be re-used to make a new account, up to 4 times per month.
[^2] classes and teams have a life of their own. Close them manually if you want to, before deleting your account.
 */
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
    ublogApi: lila.ublog.UblogApi,
    activityWrite: lila.activity.ActivityWriteApi,
    email: lila.mailer.AutomaticEmail,
    tokenApi: lila.oauth.AccessTokenApi
)(using Executor, Scheduler):

  Bus.subscribeFuns(
    "garbageCollect" -> { case lila.core.security.GarbageCollect(userId) =>
      (modApi.garbageCollect(userId) >> lichessClose(userId))
    },
    "rageSitClose" -> { case lila.core.playban.RageSitClose(userId) => lichessClose(userId) }
  )

  lila.common.LilaScheduler.variableDelay(
    "accountTermination.erase",
    prev => _.Delay(if prev.isDefined then 1.second else 10.seconds),
    timeout = _.AtMost(1.minute),
    initialDelay = _.Delay(111.seconds)
  )(findAndErase)

  def close(u: User)(using me: Me): Funit = for
    playbanned <- playbanApi.hasCurrentPlayban(u.id)
    selfClose    = me.is(u)
    teacherClose = !selfClose && !Granter(_.CloseAccount) && Granter(_.Teacher)
    modClose     = !selfClose && Granter(_.CloseAccount)
    badApple     = u.lameOrTroll || u.marks.alt || modClose
    _       <- userRepo.disable(u, keepEmail = badApple || playbanned)
    _       <- relationApi.unfollowAll(u.id)
    _       <- rankingApi.remove(u.id)
    teamIds <- teamApi.quitAllOnAccountClosure(u.id)
    _       <- challengeApi.removeByUserId(u.id)
    _       <- tournamentApi.withdrawAll(u)
    _       <- swissApi.withdrawAll(u, teamIds)
    _       <- planApi.cancelIfAny(u).recoverDefault
    _       <- seekApi.removeByUser(u)
    _       <- securityStore.closeAllSessionsOf(u.id)
    _       <- tokenApi.revokeAllByUser(u)
    _       <- pushEnv.webSubscriptionApi.unsubscribeByUser(u)
    _       <- pushEnv.unregisterDevices(u)
    _       <- streamerApi.demote(u.id)
    reports <- reportApi.processAndGetBySuspect(lila.report.Suspect(u))
    _ <-
      if selfClose then modLogApi.selfCloseAccount(u.id, reports)
      else if teacherClose then modLogApi.teacherCloseAccount(u.id)
      else modLogApi.closeAccount(u.id)
    _ <- appealApi.onAccountClose(u)
    _ <- ublogApi.onAccountClose(u)
    _ <- u.marks.troll.so:
      relationApi.fetchFollowing(u.id).flatMap(activityWrite.unfollowAll(u, _))
  yield Bus.publish(lila.core.security.CloseAccount(u.id), "accountClose")

  private def lichessClose(userId: UserId) =
    userRepo.lichessAnd(userId).flatMapz { (lichess, user) => close(user)(using Me(lichess)) }

  def scheduleErasure(user: User)(using Me): Funit = for
    _ <- user.enabled.yes.so(close(user))
    _ <- email.gdprErase(user)
    _ <- userRepo.scheduleErasure(user.id, true)
  yield ()

  private def findAndErase: Fu[Option[User]] =
    userRepo.findNextToErase.flatMapz: user =>
      doEraseNow(user).inject(user.some)

  private def doEraseNow(user: User): Funit =
    if user.enabled.yes
    then userRepo.scheduleErasure(user.id, false)
    else
      fuccess:
        println(s"Time to wipe $user")
