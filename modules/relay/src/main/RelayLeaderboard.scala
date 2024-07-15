package lila.relay

import chess.{ Elo, FideId, Outcome, PlayerName, PlayerTitle }

import lila.core.fide.Federation
import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import lila.study.ChapterRepo

case class RelayLeaderboard(players: List[RelayLeaderboard.Player])

object RelayLeaderboard:
  case class Player(
      name: PlayerName,
      score: Double,
      played: Int,
      rating: Option[Elo],
      title: Option[PlayerTitle],
      fideId: Option[FideId],
      fed: Option[Federation.Id]
  )

  import play.api.libs.json.*
  import lila.common.Json.given
  given OWrites[Player] = OWrites: p =>
    Json
      .obj("name" -> p.name, "score" -> p.score, "played" -> p.played)
      .add("rating", p.rating)
      .add("title", p.title)
      .add("fideId", p.fideId)
      .add("fed", p.fed)

final class RelayLeaderboardApi(
    tourRepo: RelayTourRepo,
    roundRepo: RelayRoundRepo,
    chapterRepo: ChapterRepo,
    federationsOf: Federation.FedsOf,
    cacheApi: CacheApi
)(using Executor, Scheduler):

  import BSONHandlers.given

  import play.api.libs.json.*
  import lila.common.Json.writeAs
  private given Writes[RelayLeaderboard] = writeAs(_.players)

  def apply(tour: RelayTour): Fu[Option[JsonStr]] = tour.autoLeaderboard.soFu:
    cache.get(tour.id)

  private val invalidateDebouncer =
    lila.common.Debouncer[RelayTourId](3 seconds, 32)(id => cache.put(id, computeJson(id)))

  def invalidate(id: RelayTourId) = invalidateDebouncer.push(id)

  private val cache = cacheApi[RelayTourId, JsonStr](32, "relay.leaderboard"):
    _.expireAfterWrite(10 minutes).buildAsyncFuture(computeJson)

  private def computeJson(id: RelayTourId): Fu[JsonStr] =
    compute(id).map(lead => JsonStr(Json.stringify(Json.toJson(lead))))

  private def compute(id: RelayTourId): Fu[RelayLeaderboard] = for
    tour     <- tourRepo.coll.byId[RelayTour](id).orFail(s"No such relay tour $id")
    roundIds <- roundRepo.idsByTourOrdered(tour)
    tags     <- chapterRepo.tagsByStudyIds(roundIds.map(_.into(StudyId)))
    players = tags.foldLeft(
      Map.empty[PlayerName, (Double, Int, Option[Elo], Option[PlayerTitle], Option[FideId])]
    ): (lead, game) =>
      Color.all.foldLeft(lead): (lead, color) =>
        game
          .names(color)
          .fold(lead): name =>
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
                game.elos(color).orElse(prevRating),
                prevTitle.orElse(game.titles(color)),
                prevFideId.orElse(game.fideIds(color))
              )
            )
    federations <- federationsOf(players.values.flatMap(_._5).toList)
  yield RelayLeaderboard:
    players.toList
      .sortBy: (_, player) =>
        (-player._1, -player._3.so(_.value))
      .map:
        case (name, (score, played, rating, title, fideId)) =>
          val fed = fideId.flatMap(federations.get)
          RelayLeaderboard.Player(name, score, played, rating, title, fideId, fed)
