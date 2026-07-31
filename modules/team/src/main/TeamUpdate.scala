package lila.team

import org.apache.pekko.stream.Materializer
import reactivemongo.pekkostream.cursorProducer
import reactivemongo.api.bson.Macros.Annotations.Key
import scalalib.paginator.Paginator

import lila.memo.RateLimit.LimitResult
import lila.core.notify.{ NotifyApi, NotificationContent }
import lila.core.team.LightTeam
import lila.db.dsl.{ *, given }

case class TeamUpdate[T](
    @Key("_id") id: String,
    team: T,
    text: String,
    senderId: UserId,
    date: Instant
    // seenBy: List[UserId] // in DB only, for querying
)

case class TeamUpdates[T](team: T, unread: Int, last: Instant)

case class TeamUpdateSeen[T](msg: TeamUpdate[T], seen: Boolean)

object TeamUpdate:
  type Recent = Paginator[TeamUpdateSeen[LightTeam]]
  type ByTeams = List[TeamUpdates[LightTeam]]

final class TeamUpdateApi(
    msgRepo: TeamUpdateRepo,
    memberRepo: TeamMemberRepo,
    userRepo: lila.core.user.UserRepo,
    cached: TeamCached,
    mongoRateLimitApi: lila.memo.MongoRateLimitApi,
    notifyApi: NotifyApi
)(using Executor, Scheduler, Materializer):

  import TeamUpdateApi.*

  export msgRepo.{ markSeen, teamLatest }

  private val maxPerPage = MaxPerPage(6)
  private val dedup = scalalib.cache.OnceEvery.hashCode[(TeamId, String)](10.minutes)

  def teamRecentAndMarkRead(team: Team, page: Int)(using me: Me): Fu[TeamUpdate.Recent] =
    for
      msgs <- Paginator(msgRepo.teamRecent(team.id), page, maxPerPage)
      _ <- msgs.currentPageResults.exists(!_.seen).so(msgRepo.markSeen(team.id))
    yield msgs.map(msg => TeamUpdateSeen(msg.msg.copy(team = team.light), msg.seen))

  def allRecent(page: Int)(using me: Me): Fu[TeamUpdate.Recent] = for
    teamIds <- cached.teamIds(me.userId)
    msgs <- Paginator(msgRepo.allRecent(teamIds), page, maxPerPage)
    teams <- cached.lightMapById(msgs.currentPageResults.view.map(_.msg.team).toList)
  yield msgs.mapList: results =>
    for
      msg <- results
      team <- teams.get(msg.msg.team)
    yield TeamUpdateSeen(msg.msg.copy(team = team), msg.seen)

  def byTeams(using me: Me): Fu[TeamUpdate.ByTeams] = for
    teamIds <- cached.teamIds(me.userId)
    msgs <- msgRepo.byTeams(teamIds)
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
        limiter.limit(team.id)(doSend(team.id, text).inject(LimitResult.Through))(LimitResult.Limited)
    else Left("You already sent this message recently")

  private def doSend(id: TeamId, text: String)(using me: Me): Funit =
    val msg = TeamUpdate[TeamId](
      id = scalalib.ThreadLocalRandom.nextString(8),
      team = id,
      text = text,
      senderId = me.userId,
      date = nowInstant
    )
    for
      unsubed <- memberRepo.listOfUnsubscribed(id)
      _ <- msgRepo.send(msg, unsubed)
      _ <- notifySubscribers(id)
    yield ()

  private def notifySubscribers(teamId: TeamId): Funit =
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
      .map(_.flatMap(_.getAsOpt[UserId]("user")).pp)
      .throttle(1, 1.second)
      .mapAsync(1)(notifyApi.notifyMany(_, NotificationContent.TeamUpdate))
      .run()
      .void

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
  val credits = 15
  val days = 7
  private val cost = 5
