package lila.team

import lila.memo.RateLimit.LimitResult

import reactivemongo.api.bson.Macros.Annotations.Key
import lila.core.team.LightTeam

case class TeamMsg[T](
    @Key("_id") id: String,
    team: T,
    text: String,
    senderId: UserId,
    date: Instant
    // seenBy: List[UserId] // in DB only, for querying
)

case class TeamMsgs[T](team: T, unread: Int, last: Instant)

case class TeamMsgSeen[T](msg: TeamMsg[T], seen: Boolean)

object TeamMsg:
  type Recent = List[TeamMsgSeen[LightTeam]]
  type ByTeam = List[TeamMsgs[LightTeam]]

final class TeamMsgApi(
    msgRepo: TeamMsgRepo,
    cached: TeamCached,
    mongoRateLimitApi: lila.memo.MongoRateLimitApi
)(using Executor, Scheduler):

  import TeamMsgApi.*

  private val dedup = scalalib.cache.OnceEvery.hashCode[(TeamId, String)](10.minutes)

  def allRecent(using me: Me): Fu[TeamMsg.Recent] = for
    teamIds <- cached.teamIds(me.userId)
    msgs <- msgRepo.allRecent(teamIds)
    teams <- cached.lightMapById(msgs.map(_.msg.team))
  yield
    for
      msg <- msgs
      team <- teams.get(msg.msg.team)
    yield TeamMsgSeen(msg.msg.copy(team = team), msg.seen)

  def byTeam(using me: Me): Fu[TeamMsg.ByTeam] = for
    teamIds <- cached.teamIds(me.userId)
    msgs <- msgRepo.byTeam(teamIds)
    teams <- cached.lightMapById(msgs.map(_.team))
  yield
    for
      msg <- msgs
      team <- teams.get(msg.team)
    yield TeamMsgs(team, msg.unread, msg.last)

  def send(team: Team, raw: String)(using me: Me): Either[String, Fu[LimitResult]] =
    val text = raw.replaceAll("\r\n?", "\n")
    if dedup(team.id, text) then
      Right:
        limiter.limit(team.id)(doSend(team.id, text).inject(LimitResult.Through))(LimitResult.Limited)
    else Left("You already sent this message recently")

  private def doSend(id: TeamId, text: String)(using me: Me): Funit =
    val msg = TeamMsg[TeamId](
      id = scalalib.ThreadLocalRandom.nextString(8),
      team = id,
      text = text,
      senderId = me.userId,
      date = nowInstant
    )
    msgRepo.send(msg).void

  object limiter:

    private val limiter = mongoRateLimitApi[TeamId](
      "team.pm.all",
      credits = pmAllCredits * pmAllCost,
      duration = pmAllDays.days
    )

    def limit(id: TeamId)(using me: Me) =
      limiter[LimitResult](id, if me.isVerifiedOrAdmin then 1 else pmAllCost)

    def status(id: TeamId): Fu[(Int, Instant)] =
      limiter
        .getSpent(id)
        .map: entry =>
          (pmAllCredits - entry.v / pmAllCost, entry.until)

object TeamMsgApi:
  val pmAllCredits = 8
  val pmAllDays = 7
  private val pmAllCost = 5
