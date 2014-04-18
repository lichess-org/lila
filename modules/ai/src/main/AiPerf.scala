package lila.ai

import lila.db.Types.Coll
import lila.memo.MixedCache
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

  private val cache = MixedCache.single[Map[Int, AiPerf]](
    coll.find(BSONDocument()).cursor[AiPerf].collect[List]() map { perfs =>
      levels.map { l =>
        l -> (perfs find (_.level == l) getOrElse default(l))
      }.toMap
    },
    timeToLive = cacheTtl,
    default = levels.map { i => i -> default(i) }.toMap)

  def all: Map[Int, AiPerf] = cache get true

  def intRatings: Map[Int, Int] = all map {
    case (level, perf) => level -> perf.intRating
  }

  def get(level: Int): Option[AiPerf] = all get level

  def add(level: Int, opponent: Perf, result: Glicko.Result): Funit = get(level) ?? { aiPerf =>
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

  private def mkRating(perf: Perf) = new Rating(
    perf.glicko.rating, perf.glicko.deviation, perf.glicko.volatility, perf.nb)

  private def mkPerf(rating: Rating): Perf = Perf(
    Glicko(rating.getRating, rating.getRatingDeviation, rating.getVolatility),
    nb = rating.getNumberOfResults,
    latest = DateTime.now.some)
}
