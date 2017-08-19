package lila.activity

import org.joda.time.DateTime
import play.api.libs.json._

import lila.common.Iso
import lila.common.PimpedJson._
import lila.rating.PerfType
import lila.tournament.JsonView._
import lila.tournament.LeaderboardApi.{ Entry => TourEntry, Ratio => TourRatio }
import lila.tournament.Tournament
import lila.tournament.{ Cached => TourCached }

import activities._
import model._

final class JsonView(
    lightUserApi: lila.user.LightUserApi,
    getTourName: Tournament.ID => Option[String]
) {

  private object Writers {
    implicit val perfTypeWrites = Writes[PerfType](pt => JsString(pt.key))
    implicit val ratingWrites = intIsoWriter(Iso.int[Rating](Rating.apply, _.value))
    implicit val ratingProgWrites = Json.writes[RatingProg]
    implicit val scoreWrites = Json.writes[Score]
    implicit val gamesWrites = OWrites[Games] { games =>
      JsObject { games.value.map { case (pt, score) => pt.key -> scoreWrites.writes(score) } }
    }
    implicit val dateWrites = Writes[DateTime] { date =>
      JsNumber(date.getSeconds)
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
  }
  import Writers._

  def apply(a: ActivityView): Fu[JsObject] = fuccess {
    Json.obj()
      .add("games", a.games map gamesWrites.writes)
      .add("puzzles", a.puzzles map puzzlesWrites.writes)
      .add("tournaments", a.tours map toursWrites.writes)
  }
}
