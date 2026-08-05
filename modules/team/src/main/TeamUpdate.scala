package lila.team

import org.apache.pekko.stream.Materializer
import reactivemongo.pekkostream.cursorProducer
import reactivemongo.api.bson.Macros.Annotations.Key
import scalalib.paginator.Paginator

import lila.memo.RateLimit.LimitResult
import lila.core.LightUser
import lila.core.notify.NotifyApi
import lila.core.notify.NotificationContent.TeamUpdate as Notification
import lila.core.team.LightTeam
import lila.db.dsl.{ *, given }
import lila.common.String.shorten

case class TeamUpdate[T, U](
    @Key("_id") id: String,
    team: T,
    text: String,
    sender: U,
    date: Instant
    // seenBy: List[UserId] // in DB only, for querying
)

case class TeamUpdates[T](team: T, unread: Int, last: Instant)

case class TeamUpdateSeen[T, U](msg: TeamUpdate[T, U], seen: Boolean)

object TeamUpdate:
  type Recent = Paginator[TeamUpdateSeen[LightTeam, LightUser]]
  type ByTeams = List[TeamUpdates[LightTeam]]
  type DbTeamUpdate = TeamUpdate[TeamId, UserId]
  type DbTeamUpdateSeen = TeamUpdateSeen[TeamId, UserId]

final class TeamUpdateApi(
    updateRepo: TeamUpdateRepo,
    memberRepo: TeamMemberRepo,
    userRepo: lila.core.user.UserRepo,
    cached: TeamCached,
    mongoRateLimitApi: lila.memo.MongoRateLimitApi,
    lightUserApi: lila.core.user.LightUserApi,
    notifyApi: NotifyApi,
    spam: lila.core.security.SpamApi
)(using Executor, Scheduler, Materializer):

  import TeamUpdate.*
  import TeamUpdateApi.*

  export updateRepo.{ markSeen, teamLatest }

  private val maxPerPage = MaxPerPage(6)
  private val dedup = scalalib.cache.OnceEvery.hashCode[(TeamId, String)](10.minutes)

  def teamRecentAndMarkRead(team: Team, page: Int)(using me: Me): Fu[TeamUpdate.Recent] =
    for
      msgs <- Paginator(updateRepo.teamRecent(team.id), page, maxPerPage)
      _ <- msgs.currentPageResults.exists(!_.seen).so(updateRepo.markSeen(team.id))
      senders <- pageSenders(msgs)
    yield msgs.mapList: results =>
      for
        msg <- results
        sender <- senders.get(msg.msg.sender)
      yield TeamUpdateSeen(msg.msg.copy(team = team.light, sender = sender), msg.seen)

  def allRecent(page: Int)(using me: Me): Fu[TeamUpdate.Recent] = for
    allTeamIds <- cached.teamIds(me.userId)
    teamIds <- memberRepo.filterSubscribed(allTeamIds, me.userId)
    msgs <- Paginator(updateRepo.allRecent(teamIds), page, maxPerPage)
    teams <- cached.lightMapById(msgs.currentPageResults.view.map(_.msg.team).toList)
    senders <- lightUserApi.asyncIdMapFallback(msgs.currentPageResults.view.map(_.msg.sender).toSet)
  yield msgs.mapList: results =>
    for
      msg <- results
      team <- teams.get(msg.msg.team)
      sender <- senders.get(msg.msg.sender)
    yield TeamUpdateSeen(msg.msg.copy(team = team, sender = sender), msg.seen)

  private def pageSenders(pager: Paginator[DbTeamUpdateSeen]): Fu[LightUser.IdMap] =
    lightUserApi.asyncIdMapFallback(pager.currentPageResults.view.map(_.msg.sender).toSet)

  def byTeams(using me: Me): Fu[TeamUpdate.ByTeams] = for
    teamIds <- cached.teamIds(me.userId)
    msgs <- updateRepo.byTeams(teamIds)
    teams <- cached.lightMapById(msgs.map(_.team))
  yield
    for
      msg <- msgs
      team <- teams.get(msg.team)
    yield TeamUpdates(team, msg.unread, msg.last)

  def send(team: Team, raw: String)(using me: Me): Either[String, Fu[LimitResult]] =
    val text = raw.replaceAll("\r\n?", "\n")
    if dedup(team.id, text) then
      Right:
        limiter.limit(team.id)(doSend(team, text).inject(LimitResult.Through))(LimitResult.Limited)
    else Left("You already sent this message recently")

  private def doSend(team: Team, text: String)(using me: Me): Funit =
    val msg = TeamUpdate[TeamId, UserId](
      id = scalalib.ThreadLocalRandom.nextString(8),
      team = team.id,
      text = spam.replace(text),
      sender = me.userId,
      date = nowInstant
    )
    for
      unsubed <- memberRepo.listOfUnsubscribed(team.id)
      _ <- updateRepo.send(msg, unsubed)
      notification: Notification = Notification(team.id, team.name, shorten(msg.text, 40))
      _ = notifySubscribers(team.id, notification) // don't await that!
    yield ()

  private def notifySubscribers(teamId: TeamId, notification: Notification): Funit =
    memberRepo.coll
      .aggregateWith[Bdoc](readPreference = ReadPref.sec): framework =>
        import framework.*
        List(
          Match($doc("team" -> teamId, "unsub".$ne(true))),
          Project($doc("user" -> true, "_id" -> false)),
          PipelineOperator:
            $lookup.simple(
              from = userRepo.coll,
              local = "user",
              foreign = "_id",
              as = "recent",
              pipe = List(
                $doc("$match" -> $doc("seenAt".$gt(nowInstant.minusMonths(1)))),
                $doc("$project" -> $id(true))
              )
            )
          ,
          Match("recent".$ne($arr())),
          Project($doc("user" -> true))
        )
      .documentSource()
      .grouped(100)
      .map(_.flatMap(_.getAsOpt[UserId]("user")))
      .throttle(1, 1.second)
      .mapAsync(1)(notifyApi.notifyManyUnlessUnread(_, notification))
      .run()
      .void

  object json:
    import play.api.libs.json.*
    import scalalib.Json.given
    import lila.common.Json.given
    private given OWrites[LightTeam] = Json.writes
    private given OWrites[TeamUpdate[LightTeam, LightUser]] = Json.writes
    private given OWrites[TeamUpdateSeen[LightTeam, LightUser]] = Json.writes
    private given OWrites[TeamUpdates[LightTeam]] = Json.writes
    def teamRecent(
        updates: TeamUpdate.Recent,
        byTeam: TeamUpdate.ByTeams,
        team: LightTeam,
        subscribed: Boolean
    ): JsObject =
      Json.obj(
        "team" -> team,
        "subscribed" -> subscribed,
        "updates" -> updates,
        "byTeam" -> byTeam
      )
    def allRecent(msgs: TeamUpdate.Recent, byTeam: TeamUpdate.ByTeams): JsObject =
      Json.obj(
        "updates" -> msgs,
        "byTeam" -> byTeam
      )

  object limiter:

    private val limiter = mongoRateLimitApi[TeamId](
      "team.pm.all",
      credits = credits * cost,
      duration = days.days
    )

    def limit(id: TeamId)(using me: Me) =
      limiter[LimitResult](id, if me.isVerifiedOrAdmin then 1 else cost)

    def status(id: TeamId): Fu[(Int, Instant)] =
      limiter
        .getSpent(id)
        .map: entry =>
          (credits - entry.v / cost, entry.until)

object TeamUpdateApi:
  val credits = 10
  val days = 7
  private val cost = 5
