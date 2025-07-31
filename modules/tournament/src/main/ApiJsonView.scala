package lila.tournament

import play.api.libs.json.*

import lila.common.Json.given
import lila.core.i18n.Translate
import lila.gathering.Condition
import lila.gathering.ConditionHandlers.JSONHandlers.given
import lila.gathering.GatheringJson.*
import lila.rating.PerfType

final class ApiJsonView(lightUserApi: lila.core.user.LightUserApi)(using Executor):

  import JsonView.{ *, given }

  def apply(tournaments: VisibleTournaments)(using Translate): Fu[JsObject] = for
    created <- tournaments.created.map(fullJson).parallel
    started <- tournaments.started.map(fullJson).parallel
    finished <- tournaments.finished.map(fullJson).parallel
  yield Json.obj(
    "created" -> created,
    "started" -> started,
    "finished" -> finished
  )

  def calendar(tournaments: List[Tournament])(using Translate): JsObject =
    Json.obj(
      "since" -> tournaments.headOption.map(_.startsAt.withTimeAtStartOfDay),
      "to" -> tournaments.lastOption.map(_.finishesAt.withTimeAtStartOfDay.plusDays(1)),
      "tournaments" -> JsArray(tournaments.map(baseJson))
    )

  private def baseJson(tour: Tournament)(using Translate): JsObject =
    Json
      .obj(
        "id" -> tour.id,
        "createdBy" -> tour.createdBy,
        "system" -> "arena", // BC
        "minutes" -> tour.minutes,
        "clock" -> tour.clock,
        "rated" -> tour.rated,
        "fullName" -> tour.name(),
        "nbPlayers" -> tour.nbPlayers,
        "variant" -> Json.obj(
          "key" -> tour.variant.key,
          "short" -> tour.variant.shortName,
          "name" -> tour.variant.name
        ),
        "startsAt" -> tour.startsAt,
        "finishesAt" -> tour.finishesAt,
        "status" -> tour.status.id,
        "perf" -> perfJson(tour.perfType)
      )
      .add("secondsToStart", tour.secondsToStart.some.filter(_ > 0))
      .add("hasMaxRating", tour.conditions.maxRating.isDefined) // BC
      .add[Condition.RatingCondition]("maxRating", tour.conditions.maxRating)
      .add[Condition.RatingCondition]("minRating", tour.conditions.minRating)
      .add("minRatedGames", tour.conditions.nbRatedGame)
      .add("onlyTitled", tour.conditions.titled.isDefined)
      .add("teamMember", tour.conditions.teamMember.map(_.teamId))
      .add("private", tour.isPrivate)
      .add("position", tour.position.map(position))
      .add("schedule", tour.scheduleData.map(scheduleJson))
      .add(
        "teamBattle",
        tour.teamBattle.map { battle =>
          Json.obj(
            "teams" -> battle.teams,
            "nbLeaders" -> battle.nbLeaders
          )
        }
      )

  def fullJson(tour: Tournament)(using Translate): Fu[JsObject] =
    tour.winnerId.so(lightUserApi.async).map { winner =>
      baseJson(tour).add("winner" -> winner)
    }

  def byPlayer(e: LeaderboardApi.TourEntry)(using Translate): JsObject =
    Json.obj(
      "tournament" -> baseJson(e.tour),
      "player" -> Json
        .obj(
          "games" -> e.entry.nbGames,
          "score" -> e.entry.score,
          "rank" -> e.entry.rank
        )
        .add("performance" -> e.performance)
    )

  private val perfPositions: Map[PerfKey, Int] = {
    import PerfKey.*
    List(bullet, blitz, rapid, classical, ultraBullet) ::: lila.rating.PerfType.variants
  }.zipWithIndex.toMap

  private def perfJson(p: PerfType)(using Translate) =
    Json
      .obj(
        "key" -> p.key,
        "name" -> p.trans,
        "position" -> { ~perfPositions.get(p): Int }
      )
      .add("icon" -> mobileBcIcons.get(p)) // mobile BC only
