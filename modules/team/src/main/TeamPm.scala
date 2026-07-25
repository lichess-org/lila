package lila.team

import lila.memo.RateLimit.LimitResult

final class TeamPm(
    mongoRateLimitApi: lila.memo.MongoRateLimitApi,
    routeUrl: lila.core.config.RouteUrl
)(using Executor, Scheduler):

  import TeamPm.*

  def decorateMsg(team: Team, msg: String) =
    val normalized = msg.replaceAll("\r\n?", "\n")
    val url = routeUrl(routes.Team.show(team.id))
    s"""$normalized
  ---
  You received this because you are subscribed to messages of the team $url."""

  private val dedup = scalalib.cache.OnceEvery.hashCode[(TeamId, String)](10.minutes)

  private val limiter = mongoRateLimitApi[TeamId](
    "team.pm.all",
    credits = pmAllCredits * pmAllCost,
    duration = pmAllDays.days
  )

  private def limit(id: TeamId)(using me: Me) =
    limiter[LimitResult](id, if me.isVerifiedOrAdmin then 1 else pmAllCost)

  def dedupAndLimit(id: TeamId, message: String)(
      sendAll: () => Funit
  )(using me: Me): Either[String, Fu[LimitResult]] =
    if dedup(id, message) then
      Right:
        limit(id) {
          sendAll() // we don't wait for the stream to complete, it would make lichess time out
          fuccess(LimitResult.Through)
        }(LimitResult.Limited)
    else Left("You already sent this message recently")

  def status(id: TeamId): Fu[(Int, Instant)] =
    limiter
      .getSpent(id)
      .map: entry =>
        (pmAllCredits - entry.v / pmAllCost, entry.until)

object TeamPm:
  val pmAllCredits = 8
  val pmAllDays = 7
  private val pmAllCost = 5
