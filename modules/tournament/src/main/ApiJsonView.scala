package lila.tournament

import play.api.i18n.Lang
import play.api.libs.json.*

import lila.common.Json.given
import lila.rating.PerfType
import lila.user.LightUserApi
import lila.gathering.Condition
import lila.gathering.ConditionHandlers.JSONHandlers.given

final class ApiJsonView(lightUserApi: LightUserApi)(using Executor):

  import JsonView.{ *, given }

  def apply(tournaments: VisibleTournaments)(using Lang): Fu[JsObject] = for
    created  <- tournaments.created.map(fullJson).parallel
    started  <- tournaments.started.map(fullJson).parallel
    finished <- tournaments.finished.map(fullJson).parallel
  yield Json.obj(
    "created"  -> created,
    "started"  -> started,
    "finished" -> finished
  )

  def featured(tournaments: List[Tournament])(using Lang): Fu[JsObject] =
    tournaments.map(fullJson).parallel map { objs =>
      Json.obj("featured" -> objs)
    }

  def calendar(tournaments: List[Tournament])(using Lang): JsObject =
    Json.obj(
      "since"       -> tournaments.headOption.map(_.startsAt.withTimeAtStartOfDay),
      "to"          -> tournaments.lastOption.map(_.finishesAt.withTimeAtStartOfDay plusDays 1),
      "tournaments" -> JsArray(tournaments.map(baseJson))
    )

  private def baseJson(tour: Tournament)(using Lang): JsObject =
    Json
      .obj(
        "id"        -> tour.id,
        "createdBy" -> tour.createdBy,
        "system"    -> "arena", // BC
        "minutes"   -> tour.minutes,
        "clock"     -> tour.clock,
        "rated"     -> tour.mode.rated,
        "fullName"  -> tour.name(),
        "nbPlayers" -> tour.nbPlayers,
        "variant" -> Json.obj(
          "key"   -> tour.variant.key,
          "short" -> tour.variant.shortName,
          "name"  -> tour.variant.name
        ),
        "startsAt"   -> tour.startsAt,
        "finishesAt" -> tour.finishesAt,
        "status"     -> tour.status.id,
        "perf"       -> perfJson(tour.perfType)
      )
      .add("secondsToStart", tour.secondsToStart.some.filter(0 <))
      .add("hasMaxRating", tour.conditions.maxRating.isDefined) // BC
      .add[Condition.RatingCondition]("maxRating", tour.conditions.maxRating)
      .add[Condition.RatingCondition]("minRating", tour.conditions.minRating)
      .add("minRatedGames", tour.conditions.nbRatedGame)
      .add("onlyTitled", tour.conditions.titled.isDefined)
      .add("teamMember", tour.conditions.teamMember.map(_.teamId))
      .add("private", tour.isPrivate)
      .add("position", tour.position.map(positionJson))
      .add("schedule", tour.schedule map scheduleJson)
      .add(
        "teamBattle",
        tour.teamBattle.map { battle =>
          Json.obj(
            "teams"     -> battle.teams,
            "nbLeaders" -> battle.nbLeaders
          )
        }
      )

  def fullJson(tour: Tournament)(using Lang): Fu[JsObject] =
    (tour.winnerId so lightUserApi.async) map { winner =>
      baseJson(tour).add("winner" -> winner.map(userJson))
    }

  private def userJson(u: lila.common.LightUser) =
    Json.obj(
      "id"    -> u.id,
      "name"  -> u.name,
      "title" -> u.title
    )

  private val perfPositions: Map[PerfType, Int] = {
    import PerfType.*
    List(Bullet, Blitz, Rapid, Classical, UltraBullet) ::: variants
  }.zipWithIndex.toMap

  private def perfJson(p: PerfType)(using Lang) =
    Json
      .obj(
        "key"      -> p.key,
        "name"     -> p.trans,
        "position" -> { ~perfPositions.get(p): Int }
      )
      .add("icon" -> mobileBcIcons.get(p)) // mobile BC only
