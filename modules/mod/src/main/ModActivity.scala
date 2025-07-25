package lila.mod

import play.api.libs.json.Json

import scala.util.Try

import lila.db.dsl.{ *, given }
import lila.report.Room

final class ModActivity(repo: ModlogRepo, reportApi: lila.report.ReportApi, cacheApi: lila.memo.CacheApi)(
    using Executor
):

  import ModActivity.*

  def apply(who: String, period: String)(me: User): Fu[Result] =
    cache.get((Who(who, me), Period(period)))

  type CacheKey = (Who, Period)

  private val cache = cacheApi[CacheKey, Result](64, "mod.activity"):
    _.expireAfter[CacheKey, Result](
      create = (key, _) =>
        key match
          case (_, Period.Week) => 15.seconds
          case (_, Period.Month) => 5.minutes
          case (_, Period.Year) => 1.day
      ,
      update = (_, _, current) => current,
      read = (_, _, current) => current
    ).buildAsyncFuture((compute).tupled)

  private val maxDocs = 10_000

  private def compute(who: Who, period: Period): Fu[Result] =
    repo.coll
      .aggregateList(maxDocs = maxDocs, _.sec): framework =>
        import framework.*
        def dateToString(field: String): Bdoc =
          $doc("$dateToString" -> $doc("format" -> "%Y-%m-%d", "date" -> s"$$$field"))

        val reportPipeline = List(
          Match(
            $doc(
              "open" -> false,
              who match
                case Who.Me(userId) => "done.by" -> userId
                case Who.Team => "done.by".$nin(List(UserId.lichess, UserId.irwin))
              ,
              "done.at".$gt(Period.dateSince(period))
            )
          ),
          Group($arr(dateToString("done.at"), "$room"))("nb" -> SumAll)
        )

        Match(
          $doc(
            "human" -> true,
            "date".$gt(Period.dateSince(period))
          ) ++ who.match
            case Who.Me(userId) => $doc("mod" -> userId)
            case Who.Team => $empty
        ) -> List(
          Group($arr(dateToString("date"), "$action"))("nb" -> SumAll),
          PipelineOperator(
            $doc(
              "$unionWith" -> $doc(
                "coll" -> reportApi.coll.name,
                "pipeline" -> reportPipeline
              )
            )
          ),
          Sort(Descending("_id.0")),
          Limit(maxDocs)
        )
      .map: docs =>
        for
          doc <- docs
          id <- doc.getAsOpt[List[String]]("_id")
          date <- id.headOption
          key <- id.lift(1)
          nb <- doc.int("nb")
        yield (date, key, nb)
      .map:
        _.foldLeft(Map.empty[String, Day]):
          case (acc, (date, key, nb)) =>
            acc.updatedWith(date): prev =>
              val row = prev | Day(Map.empty, Map.empty)
              Room.byKey
                .get(key)
                .map(row.set(_, nb))
                .orElse(Action.dbMap.get(key).map(row.set(_, nb)))
                .orElse(row.some)
      .map: data =>
        Result(
          who,
          period,
          data.toList.sortBy(_._1).reverse.flatMap { (date, row) =>
            Try(java.time.LocalDate.parse(date, dateFormat)).toOption.map {
              _.atStartOfDay.instant -> row
            }
          }
        )

object ModActivity:

  case class Result(
      who: Who,
      period: Period,
      data: List[(Instant, Day)]
  )

  case class Day(actions: Map[Action, Int], reports: Map[Room, Int]):
    def set(action: Action, nb: Int) = copy(actions = actions.updated(action, nb))
    def set(room: Room, nb: Int) = copy(reports = reports.updated(room, nb))

  val dateFormat = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")

  enum Period:
    def key = toString.toLowerCase
    case Week, Month, Year
  object Period:
    def apply(str: String): Period =
      if str == "year" then Year
      else if str == "month" then Month
      else Week
    def dateSince(period: Period) = period match
      case Period.Week => nowInstant.minusWeeks(1)
      case Period.Month => nowInstant.minusMonths(1)
      case Period.Year => nowInstant.minusYears(1)

  enum Who(val key: String):
    case Me(userId: UserId) extends Who("me")
    case Team extends Who("team")
  object Who:
    def apply(who: String, me: User) = if who == "me" then Me(me.id) else Team

  enum Action:
    case Message
    case MarkCheat
    case MarkTroll
    case MarkBoost
    case CloseAccount
    case ChatTimeout
    case Appeal
    case SetEmail
    case Streamer
    case Blog
    case ForumAdmin

  object Action:
    val dbMap = Map(
      "modMessage" -> Message,
      "engine" -> MarkCheat,
      "troll" -> MarkTroll,
      "booster" -> MarkBoost,
      "alt" -> CloseAccount,
      "closeAccount" -> CloseAccount,
      "chatTimeout" -> ChatTimeout,
      "appealClose" -> Appeal,
      "setEmail" -> SetEmail,
      "streamerList" -> Streamer,
      "streamerDecline" -> Streamer,
      "streamerunlist" -> Streamer,
      "streamerTier" -> Streamer,
      "blogTier" -> Blog,
      "blogPostEdit" -> Blog,
      "deletePost" -> ForumAdmin,
      "closeTopic" -> ForumAdmin
    )
    val all = dbMap.values.toList.distinct.sortBy(_.toString)

  object json:
    def apply(result: Result) = Json.obj(
      "common" -> Json.obj(
        "xaxis" -> result.data.map(_._1.toMillis)
      ),
      "reports" -> Json.obj(
        "series" -> Room.allButXfiles.map { room =>
          serie(room.name, result.data.map(_._2.reports.getOrElse(room, 0)))
        }
      ),
      "actions" -> Json.obj(
        "series" -> ModActivity.Action.all.map { act =>
          serie(act.toString, result.data.map(_._2.actions.getOrElse(act, 0)))
        }
      )
    )
    private def serie(name: String, data: List[Int]) = Json.obj(
      "name" -> name,
      "data" -> data
    )
