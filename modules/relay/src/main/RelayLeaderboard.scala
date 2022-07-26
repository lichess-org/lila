package lila.relay

import chess.format.pgn.Tags
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.db.dsl._
import lila.memo.CacheApi
import lila.study.ChapterRepo
import akka.actor.Scheduler

case class RelayLeaderboard(players: List[RelayLeaderboard.Player])

object RelayLeaderboard {
  case class Player(name: String, score: Double, played: Int, rating: Option[Int])

  import play.api.libs.json._
  implicit def playerWrites = Writes[Player] { p =>
    Json.obj("name" -> p.name, "score" -> p.score, "played" -> p.played).add("rating", p.rating)
  }
}

final class RelayLeaderboardApi(
    tourRepo: RelayTourRepo,
    roundRepo: RelayRoundRepo,
    chapterRepo: ChapterRepo,
    cacheApi: CacheApi
)(implicit
    ec: ExecutionContext,
    scheduler: Scheduler
) {

  import BSONHandlers._

  def apply(tour: RelayTour): Fu[Option[RelayLeaderboard]] = tour.autoLeaderboard ?? {
    cache get tour.id dmap some
  }

  private val invalidateDebouncer =
    new lila.common.Debouncer[RelayTour.Id](10 seconds, 64)(id => cache.put(id, compute(id)))

  def invalidate(id: RelayTour.Id) = invalidateDebouncer push id

  private val cache = cacheApi[RelayTour.Id, RelayLeaderboard](256, "relay.leaderboard") {
    _.expireAfterWrite(10 minutes).buildAsyncFuture(compute)
  }

  private def compute(id: RelayTour.Id): Fu[RelayLeaderboard] = for {
    tour     <- tourRepo.coll.byId[RelayTour, RelayTour.Id](id) orFail s"No such relay tour $id"
    roundIds <- roundRepo.idsByTourOrdered(tour)
    tags     <- chapterRepo.tagsByStudyIds(roundIds.map(_.studyId))
    players = tags.foldLeft(Map.empty[String, (Double, Int, Option[Int], Option[String])]) {
      case (lead, game: Tags) =>
        chess.Color.all.foldLeft(lead) { case (lead, color) =>
          game(color.name).fold(lead) { name =>
            val (score, played) = game.resultColor.fold((0d, 0)) {
              case None                            => (0.5, 1)
              case Some(winner) if winner == color => (1d, 1)
              case _                               => (0d, 1)
            }
            val rating = game(s"${color}Elo").flatMap(_.toIntOption)
            val title  = game(s"${color}Title")
            lead.getOrElse(name, (0d, 0, none, none)) match {
              case (prevScore, prevPlayed, prevRating, prevTitle) =>
                lead.updated(
                  name,
                  (prevScore + score, prevPlayed + played, rating orElse prevRating, title orElse prevTitle)
                )
            }
          }
        }
    }
  } yield RelayLeaderboard {
    players.toList.sortBy(-_._2._1) map { case (name, (score, played, rating, title)) =>
      val fullName = title.fold(name)(t => s"$t $name")
      RelayLeaderboard.Player(fullName, score, played, rating)
    }
  }
}
