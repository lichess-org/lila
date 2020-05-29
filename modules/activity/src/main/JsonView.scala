package lidraughts.activity

import org.joda.time.{ DateTime, Interval }
import play.api.libs.json._

import lidraughts.common.Iso
import lidraughts.common.PimpedJson._
import lidraughts.game.JsonView.colorWrites
import lidraughts.game.{ Pov, LightPov }
import lidraughts.rating.PerfType
import lidraughts.simul.Simul
import lidraughts.study.JsonView.studyIdNameWrites
import lidraughts.team.Team
import lidraughts.tournament.LeaderboardApi.{ Entry => TourEntry, Ratio => TourRatio }
import lidraughts.tournament.Tournament
import lidraughts.user.User

import activities._
import model._

final class JsonView(
    lightUserApi: lidraughts.user.LightUserApi,
    getTourName: Tournament.ID => Option[String],
    getTeamName: Team.ID => Option[String]
) {

  private object Writers {
    implicit val dateWrites = Writes[DateTime] { date =>
      JsNumber(date.getSeconds)
    }
    implicit val intervalWrites = OWrites[Interval] { i =>
      Json.obj("start" -> i.getStart, "end" -> i.getEnd)
    }
    implicit val perfTypeWrites = Writes[PerfType](pt => JsString(pt.key))
    implicit val ratingWrites = intIsoWriter(Iso.int[Rating](Rating.apply, _.value))
    implicit val ratingProgWrites = Json.writes[RatingProg]
    implicit val scoreWrites = Json.writes[Score]
    implicit val gamesWrites = OWrites[Games] { games =>
      JsObject {
        games.value.toList.sortBy(-_._2.size).map {
          case (pt, score) => pt.key -> scoreWrites.writes(score)
        }
      }
    }
    implicit val variantWrites: Writes[draughts.variant.Variant] = Writes { v =>
      JsString(v.key)
    }
    // writes as percentage
    implicit val tourRatioWrites = Writes[TourRatio] { r =>
      JsNumber((r.value * 100).toInt atLeast 1)
    }
    implicit val tourEntryWrites = OWrites[TourEntry] { e =>
      Json.obj(
        "tournament" -> Json.obj(
          "id" -> e.tourId,
          "name" -> ~getTourName(e.tourId)
        ),
        "nbGames" -> e.nbGames,
        "score" -> e.score,
        "rank" -> e.rank,
        "rankPercent" -> e.rankRatio
      )
    }
    implicit val toursWrites = Json.writes[ActivityView.Tours]
    implicit val puzzlesWrites = Json.writes[Puzzles]
    implicit def simulWrites(user: User) = OWrites[Simul] { s =>
      Json.obj(
        "id" -> s.id,
        "name" -> s.name,
        "isHost" -> (s.hostId == user.id),
        "variants" -> s.variants,
        "score" -> Score(s.wins, s.losses, s.draws, none)
      )
    }
    implicit val playerWrites = OWrites[lidraughts.game.Player] { p =>
      Json.obj()
        .add("aiLevel" -> p.aiLevel)
        .add("user" -> p.userId)
        .add("rating" -> p.rating)
    }
    implicit val lightPovWrites = OWrites[LightPov] { p =>
      Json.obj(
        "id" -> p.game.id,
        "color" -> p.color,
        "url" -> s"/${p.game.id}/${p.color.name}",
        "opponent" -> p.opponent
      )
    }
    implicit val followListWrites = Json.writes[FollowList]
    implicit val followsWrites = Json.writes[Follows]
    implicit val teamsWrites = Writes[Teams] { s =>
      JsArray(s.value.map { id =>
        Json.obj("url" -> s"/team/$id", "name" -> getTeamName(id))
      })
    }
    implicit val patronWrites = Json.writes[Patron]
  }
  import Writers._

  def apply(a: ActivityView, user: User): Fu[JsObject] = fuccess {
    Json.obj("interval" -> a.interval)
      .add("games", a.games)
      .add("puzzles", a.puzzles)
      .add("puzzlesFrisian", a.puzzlesFrisian)
      .add("puzzlesRussian", a.puzzlesRussian)
      .add("tournaments", a.tours)
      .add("practice", a.practice.map(_.toList.sortBy(-_._2) map {
        case (study, nb) => Json.obj(
          "url" -> s"/practice/-/${study.slug}/${study.id}",
          "name" -> study.name,
          "nbPositions" -> nb
        )
      }))
      .add("simuls", a.simuls.map(_ map simulWrites(user).writes))
      .add("correspondenceMoves", a.corresMoves.map {
        case (nb, povs) => Json.obj("nb" -> nb, "games" -> povs)
      })
      .add("correspondenceEnds", a.corresEnds.map {
        case (score, povs) => Json.obj("score" -> score, "games" -> povs)
      })
      .add("follows" -> a.follows)
      .add("studies" -> a.studies)
      .add("teams" -> a.teams)
      .add("posts" -> a.posts.map(_ map {
        case (topic, posts) => Json.obj(
          "topicUrl" -> s"/forum/${topic.categId}/${topic.slug}",
          "topicName" -> topic.name,
          "posts" -> posts.map { p =>
            Json.obj(
              "url" -> s"/forum/redirect/post/${p.id}",
              "text" -> p.text.take(500)
            )
          }
        )
      }))
      .add("patron" -> a.patron)
  }
}
