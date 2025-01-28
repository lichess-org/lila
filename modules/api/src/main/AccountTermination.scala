package lila.api

import lila.common.Bus
import lila.core.perm.Granter
import lila.user.UserDelete
import akka.stream.scaladsl.*
import lila.db.dsl.{ *, given }

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
| public studies            | unlisted                         | anonymized            | deleted                        |
| private studies           | hidden                           | deleted               | deleted                        |
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
final class AccountTermination(
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
    tokenApi: lila.oauth.AccessTokenApi,
    roundApi: lila.core.round.RoundApi,
    gameRepo: lila.game.GameRepo,
    analysisRepo: lila.analyse.AnalysisRepo,
    chatApi: lila.chat.ChatApi
)(using Executor, Scheduler, akka.stream.Materializer):

  Bus.subscribeFuns(
    "garbageCollect" -> { case lila.core.security.GarbageCollect(userId) =>
      (modApi.garbageCollect(userId) >> lichessDisable(userId))
    },
    "rageSitClose" -> { case lila.core.playban.RageSitClose(userId) => lichessDisable(userId) }
  )

  def disable(u: User, forever: Boolean)(using me: Me): Funit = for
    playbanned <- playbanApi.hasCurrentPlayban(u.id)
    selfClose    = me.is(u)
    teacherClose = !selfClose && !Granter(_.CloseAccount) && Granter(_.Teacher)
    modClose     = !selfClose && Granter(_.CloseAccount)
    tos          = u.marks.dirty || modClose || playbanned
    _       <- userRepo.disable(u, keepEmail = tos, forever = forever)
    _       <- roundApi.resignAllGamesOf(u.id)
    _       <- relationApi.unfollowAll(u.id)
    _       <- relationApi.removeAllFollowers(u.id)
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

  def scheduleDelete(u: User)(using Me): Funit = for
    _ <- disable(u, forever = false)
    _ <- email.delete(u)
    _ <- userRepo.delete.schedule(u.id, UserDelete(nowInstant, erase = false).some)
  yield ()

  def scheduleErase(u: User)(using Me): Funit = for
    _ <- disable(u, forever = false)
    _ <- email.gdprErase(u)
    _ <- userRepo.delete.schedule(u.id, UserDelete(nowInstant, erase = true).some)
  yield ()

  private def lichessDisable(userId: UserId) =
    userRepo.lichessAnd(userId).flatMapz { (lichess, user) =>
      disable(user, forever = false)(using Me(lichess))
    }

  lila.common.LilaScheduler.variableDelay(
    "accountTermination.delete",
    delay = prev => _.Delay(if prev.isDefined then 1.second else 10.seconds),
    timeout = _.AtMost(1.minute),
    initialDelay = _.Delay(111.seconds)
  ):
    userRepo.delete.findNextScheduled
      .flatMapz: (user, del) =>
        if user.enabled.yes
        then userRepo.delete.schedule(user.id, none).inject(none)
        else doDeleteNow(user, del).inject(user.some)

  private def doDeleteNow(u: User, del: UserDelete): Funit = for
    playbanned  <- playbanApi.hasCurrentPlayban(u.id)
    closedByMod <- modLogApi.closedByMod(u)
    tos = u.marks.dirty || closedByMod || playbanned
    _   = logger.info(s"Deleting user ${u.username} tos=$tos erase=${del.erase}")
    _                   <- if tos then userRepo.delete.nowWithTosViolation(u) else userRepo.delete.nowFully(u)
    _                   <- activityWrite.deleteAll(u)
    singlePlayerGameIds <- gameRepo.deleteAllSinglePlayerOf(u.id)
    _                   <- analysisRepo.remove(singlePlayerGameIds)
    _                   <- deleteAllGameChats(u)
    _                   <- streamerApi.delete(u)
    _                   <- del.erase.so(swissApi.onUserErase(u.id))
    _                   <- teamApi.onUserDelete(u.id)
    _                   <- ublogApi.onAccountDelete(u)
    _ <- u.marks.clean.so:
      securityStore.deleteAllSessionsOf(u.id)
  yield
    // a lot of deletion is done by modules listening to the following event:
    Bus.pub(lila.core.user.UserDelete(u, del.erase))

  private def deleteAllGameChats(u: User) = gameRepo
    .docCursor(lila.game.Query.user(u.id), $id(true).some)
    .documentSource()
    .mapConcat(_.getAsOpt[GameId]("_id").toList)
    .grouped(100)
    .mapAsync(1)(ids => chatApi.userChat.removeMessagesBy(ids, u.id))
    .runWith(Sink.ignore)
