package lila.insight

case class Question[X](
    dimension: InsightDimension[X],
    metric: InsightMetric,
    filters: List[Filter[?]] = Nil
):
  def filter(f: Filter[?]) = copy(filters = f :: filters)

  def withMetric(m: InsightMetric) = copy(metric = m)

  def monKey = s"${dimension.key}_${metric.key}"

object Question:

  private val peerDistribution = {
    // based on https://lichess.org/stat/rating/distribution/blitz
    // https://hq.lichess.ovh/#narrow/stream/1-general/topic/tutor.2C.20let's.20build.20it.20together/near/2159478
    import breeze.stats.distributions.*
    Gaussian(1465d, 394d)(using Rand)
  }

  case class Peers(rating: MeanRating):
    def ratingRange: Range =
      val cdf = peerDistribution.cdf(rating.value) // percentile of player given rating
      val proportion     = 0.01 // probability density function, i.e. proportion of players
      val (lower, upper) = (cdf - proportion, cdf + proportion)
      Range(
        peerDistribution.inverseCdf(lower atLeast 0).toInt atLeast 0,
        peerDistribution.inverseCdf(upper atMost 1).toInt atMost 4000
      )
    def showRatingRange =
      val rr = ratingRange
      s"${rr.min}-${rr.max}"

case class Filter[A](
    dimension: InsightDimension[A],
    selected: List[A]
):

  def isEmpty = selected.isEmpty || selected.sizeIs == InsightDimension.valuesOf(dimension).size

  import reactivemongo.api.bson.*

  def matcher: BSONDocument = InsightDimension.filtersOf(dimension, selected)
