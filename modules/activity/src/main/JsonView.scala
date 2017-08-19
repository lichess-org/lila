package lila.activity

import org.joda.time.{ DateTime, Interval }
import play.api.libs.json._

import lila.common.Iso
import lila.common.PimpedJson._
import lila.rating.PerfType
import lila.simul.Simul
import lila.tournament.JsonView._
import lila.tournament.LeaderboardApi.{ Entry => TourEntry, Ratio => TourRatio }
import lila.tournament.Tournament
import lila.tournament.{ Cached => TourCached }
import lila.user.User

import activities._
import model._

final class JsonView(
    lightUserApi: lila.user.LightUserApi,
    getTourName: Tournament.ID => Option[String]
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
    implicit val variantWrites: Writes[chess.variant.Variant] = Writes { v =>
      JsString(v.key)
    }
    implicit val tourRatioWrites = intIsoWriter(Iso.int[TourRatio](r => TourRatio(r.toDouble), _.value.toInt))
    implicit val tourEntryWrites = OWrites[TourEntry] { e =>
      Json.obj(
        "tournament" -> Json.obj(
          "id" -> e.tourId,
          "name" -> ~getTourName(e.tourId)
        ),
        "nbGames" -> e.nbGames,
        "score" -> e.score,
        "rank" -> e.rank,
        "rankRatio" -> e.rankRatio
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
  }
  import Writers._

  def apply(a: ActivityView, user: User): Fu[JsObject] = fuccess {
    Json.obj("interval" -> a.interval)
      .add("games", a.games map gamesWrites.writes)
      .add("puzzles", a.puzzles map puzzlesWrites.writes)
      .add("tournaments", a.tours map toursWrites.writes)
      .add("practice", a.practice.map(_.toList.sortBy(-_._2) map {
        case (study, nb) => Json.obj(
          "url" -> s"/practice/-/${study.slug}/${study.id}",
          "name" -> study.name,
          "nbPositions" -> nb
        )
      }))
      .add("simuls", a.simuls.map(_ map simulWrites(user).writes))
  }
}
