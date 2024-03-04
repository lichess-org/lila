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

  private val peerDistribution =
    // > db.insight.estimatedDocumentCount()
    // 277226337
    // > db.insight.aggregate([{$sample:{size:10000000}},{$project:{_id:0,mr:1}},{$match:{mr:{$exists:1}}},{$group:{_id:null,ratings:{$avg:'$mr'}}}])
    // { "_id" : null, "ratings" : 1878.1484907826184 }
    // > db.insight.aggregate([{$sample:{size:10000000}},{$project:{_id:0,mr:1}},{$match:{mr:{$exists:1}}},{$group:{_id:null,ratings:{$stdDevSamp:'$mr'}}}])
    // { "_id" : null, "ratings" : 357.42969844387625 }
    lila.rating.Gaussian(1878d, 357d)

  case class Peers(rating: MeanRating):
    lazy val ratingRange: Range =
      val cdf = peerDistribution.cdf(rating.value) // percentile of player given rating
      val proportion     = 0.01 // probability density function, i.e. proportion of players
      val (lower, upper) = (cdf - proportion, cdf + proportion)
      Range(
        peerDistribution.inverseCdf(lower.atLeast(0)).toInt.atLeast(0),
        peerDistribution.inverseCdf(upper.atMost(1)).toInt.atMost(4000)
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
