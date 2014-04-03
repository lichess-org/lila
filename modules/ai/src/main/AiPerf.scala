package lila.ai

import lila.db.Types.Coll
import lila.memo.AsyncCache
import lila.rating.{ Perf, Glicko }
import org.goochjs.glicko2._
import org.joda.time.DateTime
import reactivemongo.bson.BSONDocument
import scala.concurrent.duration.Duration

case class AiPerf(_id: Int, perf: Perf) {

  def level = _id

  def intRating = perf.intRating
}

final class AiPerfApi(coll: Coll, cacheTtl: Duration) {

  import reactivemongo.bson.Macros
  private implicit val aiPerfBSONHandler = Macros.handler[AiPerf]

  private val VOLATILITY = Glicko.default.volatility
  private val TAU = 0.75d
  private val system = new RatingCalculator(VOLATILITY, TAU)

  private val levels = Config.levels.toSet

  private def default(level: Int) = AiPerf(level, Perf.default)

  def all = AsyncCache.single[Map[Int, AiPerf]](
    coll.find(BSONDocument()).cursor[AiPerf].collect[List]() map { perfs =>
      levels.map { l =>
        l -> (perfs find (_.level == l) getOrElse default(l))
      }.toMap
    },
    timeToLive = cacheTtl)

  def intRatings: Fu[Map[Int, Int]] = all(true) map {
    _ map {
      case (level, perf) => level -> perf.intRating
    }
  }

  def get(level: Int): Fu[Option[AiPerf]] = levels(level) ?? {
    all(true) map (_ get level)
  }

  def add(level: Int, opponent: Perf, result: Glicko.Result): Funit = get(level) flatMap {
    _ ?? { aiPerf =>
      val aiRating = mkRating(aiPerf.perf)
      val huRating = mkRating(opponent)
      val results = new RatingPeriodResults()
      result match {
        case Glicko.Result.Draw => results.addDraw(aiRating, huRating)
        case Glicko.Result.Win  => results.addResult(aiRating, huRating)
        case Glicko.Result.Loss => results.addResult(huRating, aiRating)
      }
      system.updateRatings(results)
      val newAiPerf = mkPerf(aiRating)
      coll.update(
        BSONDocument("_id" -> level),
        BSONDocument("$set" -> BSONDocument("perf" -> newAiPerf))
      ).void
    }
  }

  private def mkRating(perf: Perf) = new Rating(
    perf.glicko.rating, perf.glicko.deviation, perf.glicko.volatility, perf.nb)

  private def mkPerf(rating: Rating): Perf = Perf(
    Glicko(rating.getRating, rating.getRatingDeviation, rating.getVolatility),
    nb = rating.getNumberOfResults,
    latest = DateTime.now.some)
}
