package lila.mod

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import reactivemongo.api.ReadPreference
import scala.concurrent.ExecutionContext
import scala.util.Try

import lila.db.dsl._
import lila.user.User

final class ModProgress(repo: ModlogRepo)(implicit ec: ExecutionContext) {

  import ModProgress._

  def apply(period: String, who: Option[User.ID]): Fu[Result] =
    apply(Period(period), Who(who))

  def apply(period: Period, who: Who): Fu[Result] =
    repo.coll
      .aggregateList(
        maxDocs = 10_000,
        readPreference = ReadPreference.secondaryPreferred
      ) { framework =>
        import framework._
        Match(
          $doc(
            "human" -> true,
            "date" $gt (period match {
              case Period.Week  => DateTime.now.minusWeeks(1)
              case Period.Month => DateTime.now.minusMonths(1)
              case Period.Year  => DateTime.now.minusYears(1)
            })
          ) ++ (who match {
            case Who.Me(userId) => $doc("mod" -> userId)
            case Who.Team       => $empty
          })
        ) -> List(
          Group(
            $arr(
              $doc("$dateToString" -> $doc("format" -> "%Y-%m-%d", "date" -> "$date")),
              "$action"
            )
          )("nb" -> SumAll)
        )
      }
      .map { docs =>
        for {
          doc       <- docs
          id        <- doc.getAsOpt[List[String]]("_id")
          date      <- id.headOption
          actionKey <- id lift 1
          action    <- Action.dbMap get actionKey
          nb        <- doc.int("nb")
        } yield (date, action, nb)
      }
      .map {
        _.foldLeft(Map.empty[String, Map[Action, Int]]) { case (acc, (date, action, nb)) =>
          acc.updated(date, acc.getOrElse(date, Map.empty).updated(action, nb))
        }
      }
      .map { data =>
        Result(
          period,
          who,
          data.toList.sortBy(_._1).reverse.flatMap { case (date, actions) =>
            Try(dateFormat parseDateTime date).toOption map { _ -> Row(actions) }
          }
        )
      }
}

object ModProgress {

  case class Result(
      period: Period,
      who: Who,
      data: List[(DateTime, Row)]
  )

  case class Row(actions: Map[Action, Int])

  private val dateFormat = DateTimeFormat forPattern "yyyy-MM-dd"

  sealed trait Period
  object Period {
    case object Week  extends Period
    case object Month extends Period
    case object Year  extends Period
    def apply(str: String): Period =
      if (str == "year") Year
      else if (str == "month") Month
      else Week
  }

  sealed trait Who
  object Who {
    case class Me(userId: User.ID) extends Who
    case object Team               extends Who
    def apply(me: Option[User.ID]) = me.fold[Who](Team)(Me)
  }

  sealed trait Action
  object Action {
    case object Message      extends Action
    case object MarkCheat    extends Action
    case object MarkTroll    extends Action
    case object MarkBoost    extends Action
    case object CloseAccount extends Action
    case object CloseTeam    extends Action
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
      "disableTeam"     -> CloseTeam,
      "enableTeam"      -> CloseTeam,
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
}
