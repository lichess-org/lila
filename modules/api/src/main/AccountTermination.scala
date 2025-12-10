package lila.api

import lila.common.Bus
import lila.core.perm.Granter
import lila.user.UserDelete
import lila.db.dsl.{ *, given }

/* There are 2 flavours of account termination.
|                           | disable                          | delete                |
|---------------------------|----------------------------------|-----------------------|
| how                       | from settings menu               | from /account/delete  |
| reopen                    | available to user                | strictly impossible   |
| games                     | intact                           | anonymized            |
| username                  | intact, no reuse                 | anonymized, no reuse  |
| email                     | kept for reopening, no reuse[^1] | deleted, no reuse[^1] |
| profile data              | hidden                           | deleted               |
| sessions and oauth tokens | closed                           | deleted               |
| patron subscription       | canceled                         | canceled              |
| blog posts                | unlisted                         | deleted               |
| public studies            | unlisted                         | anonymized            |
| private studies           | hidden                           | deleted               |
| activity                  | hidden                           | deleted               |
| coach/streamer profiles   | hidden                           | deleted               |
| tournaments joined        | unlisted                         | anonymized            |
| tournaments created       | hidden                           | anonymized            |
| forum posts               | intact                           | deleted               |
| teams/classes joined      | quit                             | quit                  |
| team/classes created      | intact[^2]                       | intact[^2]            |
| classes joiated           | intact[^2]                       | intact[^2]            |
| puzzle history            | hidden                           | deleted               |
| follows and blocks        | deleted                          | deleted               |

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
    securityStore: lila.security.SessionStore,
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

  def disable(u: User, forever: Boolean)(using me: Me): Funit = for
    _ <- isEssential(u.id).so:
      fufail[Unit](s"Cannot disable essential account ${u.username}")
    playbanned <- playbanApi.hasCurrentPlayban(u.id)
    selfClose = me.is(u)
    teacherClose = !selfClose && !Granter(_.CloseAccount) && Granter(_.Teacher)
    modClose = !selfClose && Granter(_.CloseAccount)
    tos = u.marks.dirty || modClose || playbanned
    _ <- userRepo.disable(u, keepEmail = tos, forever = forever)
    _ <- roundApi.resignAllGamesOf(u.id)
    followedIds <- relationApi.accountTermination(u)
    _ <- rankingApi.remove(u.id)
    teamIds <- teamApi.quitAllOnAccountClosure(u.id)
    _ <- tos.so(teamApi.deleteNewlyCreatedBy(u.id))
    _ <- challengeApi.removeByUserId(u.id)
    _ <- tournamentApi.withdrawAll(u)
    _ <- swissApi.withdrawAll(u, teamIds)
    _ <- planApi.cancelIfAny(u).recoverDefault
    _ <- seekApi.removeByUser(u)
    _ <- securityStore.closeAllSessionsOf(u.id)
    _ <- selfClose.so(tokenApi.revokeAllByUser(u.id))
    _ <- pushEnv.webSubscriptionApi.unsubscribeByUser(u)
    _ <- pushEnv.unregisterDevices(u)
    _ <- streamerApi.demote(u.id)
    reports <- reportApi.processAndGetBySuspect(lila.report.Suspect(u))
    _ <-
      if selfClose then modLogApi.selfCloseAccount(u.id, forever, reports)
      else if teacherClose then modLogApi.teacherCloseAccount(u.id)
      else modLogApi.closeAccount(u.id)
    _ <- appealApi.onAccountClose(u)
    _ <- ublogApi.onAccountClose(u)
    _ <- (u.marks.troll || u.marks.alt).so(activityWrite.unfollowAll(u, followedIds))
  yield Bus.pub(lila.core.security.CloseAccount(u.id))

  def scheduleDelete(u: User)(using Me): Funit = for
    _ <- disable(u, forever = false)
    _ <- email.delete(u)
    _ <- userRepo.delete.schedule(u.id, UserDelete(nowInstant).some)
  yield ()

  private[api] def garbageCollect(userId: UserId) =
    modApi.garbageCollect(userId) >> lichessDisable(userId)

  private[api] def lichessDisable(userId: UserId) =
    userRepo.lichessAnd(userId).flatMapz { (lichess, user) =>
      disable(user, forever = false)(using Me(lichess))
    }

  lila.common.LilaScheduler.variableDelay(
    "accountTermination.delete",
    delay = prev => _.Delay(if prev.isDefined then 1.second else 10.seconds),
    timeout = _.AtMost(1.minute),
    initialDelay = _.Delay(111.seconds)
  ):
    userRepo.delete.findNextScheduled.flatMapz: user =>
      if user.enabled.yes
      then userRepo.delete.schedule(user.id, none).inject(none)
      else doDeleteNow(user).inject(user.some)

  private def doDeleteNow(u: User): Funit = for
    _ <- isEssential(u.id).so:
      fufail[Unit](s"Cannot delete essential account ${u.username}")
    playbanned <- playbanApi.hasCurrentPlayban(u.id)
    tos = u.marks.dirty || playbanned
    _ = logger.info(s"Deleting user ${u.username} tos=$tos")
    _ <- if tos then userRepo.delete.nowWithTosViolation(u) else userRepo.delete.nowFully(u)
    _ <- activityWrite.deleteAll(u)
    singlePlayerGameIds <- gameRepo.deleteAllSinglePlayerOf(u.id)
    _ <- analysisRepo.remove(singlePlayerGameIds)
    _ <- deleteAllGameChats(u)
    _ <- streamerApi.repo.delete(u)
    swissIds <- gameRepo.swissIdsOf(u.id)
    _ <- swissIds.nonEmpty.so(swissApi.onUserDelete(u.id, swissIds))
    _ <- teamApi.onUserDelete(u.id)
    _ <- ublogApi.onAccountDelete(u)
    _ <- tokenApi.revokeAllByUser(u.id)
    _ <- u.marks.clean.so:
      securityStore.deleteAllSessionsOf(u.id)
  yield
    // a lot of deletion is done by modules listening to the following event:
    Bus.pub(lila.core.user.UserDelete(u))

  private def deleteAllGameChats(u: User) = gameRepo
    .docCursor(lila.game.Query.user(u.id), $id(true).some)
    .documentSource()
    .mapConcat(_.getAsOpt[GameId]("_id").toList)
    .grouped(100)
    .mapAsync(1)(ids => chatApi.userChat.removeMessagesBy(ids, u.id))
    .run()

  private val isEssential: Set[UserId] =
    Set(
      UserId.lichess,
      UserId.broadcaster,
      UserId.irwin,
      UserId.kaladin,
      UserId.explorer,
      UserId.ai,
      UserId.lichess4545,
      UserId.watcherbot
    )
