package lila.relay

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import lila.study.ChapterRepo
import chess.{ Outcome, FideId }

case class RelayLeaderboard(players: List[RelayLeaderboard.Player])

object RelayLeaderboard:
  case class Player(
      name: String,
      score: Double,
      played: Int,
      rating: Option[Int],
      title: Option[UserTitle],
      fideId: Option[FideId]
  )

  import play.api.libs.json.*
  import lila.common.Json.given
  given OWrites[Player] = OWrites: p =>
    Json
      .obj("name" -> p.name, "score" -> p.score, "played" -> p.played)
      .add("rating", p.rating)
      .add("title", p.title)
      .add("fideId", p.fideId)

final class RelayLeaderboardApi(
    tourRepo: RelayTourRepo,
    roundRepo: RelayRoundRepo,
    chapterRepo: ChapterRepo,
    cacheApi: CacheApi
)(using Executor, Scheduler):

  import BSONHandlers.given

  def apply(tour: RelayTour): Fu[Option[RelayLeaderboard]] = tour.autoLeaderboard.soFu:
    cache get tour.id

  private val invalidateDebouncer =
    lila.common.Debouncer[RelayTour.Id](10 seconds, 64)(id => cache.put(id, compute(id)))

  def invalidate(id: RelayTour.Id) = invalidateDebouncer push id

  private val cache = cacheApi[RelayTour.Id, RelayLeaderboard](256, "relay.leaderboard"):
    _.expireAfterWrite(10 minutes).buildAsyncFuture(compute)

  private def compute(id: RelayTour.Id): Fu[RelayLeaderboard] = for
    tour     <- tourRepo.coll.byId[RelayTour](id) orFail s"No such relay tour $id"
    roundIds <- roundRepo.idsByTourOrdered(tour)
    tags     <- chapterRepo.tagsByStudyIds(roundIds.map(_ into StudyId))
    players = tags.foldLeft(Map.empty[String, (Double, Int, Option[Int], Option[UserTitle], Option[FideId])]):
      (lead, game) =>
        chess.Color.all.foldLeft(lead): (lead, color) =>
          game(color.name).fold(lead): name =>
            val (score, played) = game.outcome.fold((0d, 0)):
              case Outcome(None)                            => (0.5, 1)
              case Outcome(Some(winner)) if winner == color => (1d, 1)
              case _                                        => (0d, 1)
            val (prevScore, prevPlayed, prevRating, prevTitle, prevFideId) =
              lead.getOrElse(name, (0d, 0, none, none, none))
            lead.updated(
              name,
              (
                prevScore + score,
                prevPlayed + played,
                game.elos(color) orElse prevRating,
                prevTitle orElse UserTitle.from(game.titles(color)),
                prevFideId orElse game.fideIds(color)
              )
            )
  yield RelayLeaderboard:
    players.toList.sortBy(-_._2._1) map:
      case (name, (score, played, rating, title, fideId)) =>
        RelayLeaderboard.Player(name, score, played, rating, title, fideId)
