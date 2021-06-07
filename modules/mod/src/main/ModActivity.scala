package lila.mod

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.Json
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext
import scala.util.Try

import lila.db.dsl._
import lila.report.Report
import lila.report.Room
import lila.user.User

final class ModActivity(repo: ModlogRepo, reportApi: lila.report.ReportApi, cacheApi: lila.memo.CacheApi)(
    implicit ec: ExecutionContext
) {

  import ModActivity._

  def apply(who: String, period: String)(me: User): Fu[Result] =
    apply(Who(who, me), Period(period))

  type CacheKey = (Who, Period)

  private val cache = cacheApi.notLoading[CacheKey, Result](64, "mod.activity") {
    _.expireAfter[CacheKey, Result](
      create = (key, _) =>
        key match {
          case (_, Period.Week)  => 15.seconds
          case (_, Period.Month) => 5.minutes
          case (_, Period.Year)  => 1.day
        },
      update = (_, _, current) => current,
      read = (_, _, current) => current
    ).buildAsync()
  }

  private def apply(who: Who, period: Period): Fu[Result] =
    cache.getFuture((who, period), (compute _).tupled)

  private def compute(who: Who, period: Period): Fu[Result] =
    repo.coll
      .aggregateList(
        maxDocs = 10_000,
        readPreference = ReadPreference.secondaryPreferred
      ) { framework =>
        import framework._
        val dateSince = period match {
          case Period.Week  => DateTime.now.minusWeeks(1)
          case Period.Month => DateTime.now.minusMonths(1)
          case Period.Year  => DateTime.now.minusYears(1)
        }
        def dateToString(field: String): Bdoc =
          $doc("$dateToString" -> $doc("format" -> "%Y-%m-%d", "date" -> s"$$$field"))

        val reportPipeline = List(
          Match(
            $doc(
              "open" -> false,
              who match {
                case Who.Me(userId) => "done.by" -> userId
                case Who.Team       => "done.by" $nin List(User.lichessId, "irwin")
              },
              "done.at" $gt dateSince
            )
          ),
          Group($arr(dateToString("done.at"), "$room"))("nb" -> SumAll)
        )

        Match(
          $doc(
            "human" -> true,
            "date" $gt dateSince
          ) ++ (who match {
            case Who.Me(userId) => $doc("mod" -> userId)
            case Who.Team       => $empty
          })
        ) -> List(
          Group($arr(dateToString("date"), "$action"))("nb" -> SumAll),
          PipelineOperator(
            $doc(
              "$unionWith" -> $doc(
                "coll"     -> reportApi.coll.name,
                "pipeline" -> reportPipeline
              )
            )
          ),
          Sort(Descending("_id.0"))
        )
      }
      .map { docs =>
        for {
          doc  <- docs
          id   <- doc.getAsOpt[List[String]]("_id")
          date <- id.headOption
          key  <- id lift 1
          nb   <- doc.int("nb")
        } yield (date, key, nb)
      }
      .map {
        _.foldLeft(Map.empty[String, Day]) { case (acc, (date, key, nb)) =>
          acc.updated(
            date, {
              val row = acc.getOrElse(date, Day(Map.empty, Map.empty))
              Room.byKey
                .get(key)
                .map(row.set(_, nb))
                .orElse(Action.dbMap.get(key).map(row.set(_, nb)))
                .getOrElse(row)
            }
          )
        }
      }
      .map { data =>
        Result(
          who,
          period,
          data.toList.sortBy(_._1).reverse.flatMap { case (date, row) =>
            Try(dateFormat parseDateTime date).toOption map { _ -> row }
          }
        )
      }
}

object ModActivity {

  case class Result(
      who: Who,
      period: Period,
      data: List[(DateTime, Day)]
  )

  case class Day(actions: Map[Action, Int], reports: Map[Room, Int]) {
    def set(action: Action, nb: Int) = copy(actions = actions.updated(action, nb))
    def set(room: Room, nb: Int)     = copy(reports = reports.updated(room, nb))
  }

  private val dateFormat = DateTimeFormat forPattern "yyyy-MM-dd"

  sealed trait Period {
    def key = toString.toLowerCase
  }
  object Period {
    case object Week  extends Period
    case object Month extends Period
    case object Year  extends Period
    def apply(str: String): Period =
      if (str == "year") Year
      else if (str == "month") Month
      else Week
  }

  sealed abstract class Who(val key: String)
  object Who {
    case class Me(userId: User.ID) extends Who("me")
    case object Team               extends Who("team")
    def apply(who: String, me: User) =
      if (who == "me") Me(me.id) else Team
  }

  sealed trait Action
  object Action {
    case object Message      extends Action
    case object MarkCheat    extends Action
    case object MarkTroll    extends Action
    case object MarkBoost    extends Action
    case object CloseAccount extends Action
    case object ChatTimeout  extends Action
    case object Appeal       extends Action
    case object SetEmail     extends Action
    case object Streamer     extends Action
    case object ForumAdmin   extends Action
    val dbMap = Map(
      "modMessage"      -> Message,
      "engine"          -> MarkCheat,
      "unengine"        -> MarkCheat,
      "troll"           -> MarkTroll,
      "untroll"         -> MarkTroll,
      "booster"         -> MarkBoost,
      "unbooster"       -> MarkBoost,
      "alt"             -> CloseAccount,
      "unalt"           -> CloseAccount,
      "closeAccount"    -> CloseAccount,
      "reopenAccount"   -> CloseAccount,
      "chatTimeout"     -> ChatTimeout,
      "appealPost"      -> Appeal,
      "appealClose"     -> Appeal,
      "setEmail"        -> SetEmail,
      "streamerList"    -> Streamer,
      "streamerDecline" -> Streamer,
      "streamerunlist"  -> Streamer,
      "streamerTier"    -> Streamer,
      "deletePost"      -> ForumAdmin,
      "closeTopic"      -> ForumAdmin
    )
    val all = dbMap.values.toList.distinct.sortBy(_.toString)
  }

  object json {
    def apply(result: Result) = Json.obj(
      "common" -> Json.obj(
        "xaxis" -> result.data.map(_._1.getMillis)
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
  }
}
