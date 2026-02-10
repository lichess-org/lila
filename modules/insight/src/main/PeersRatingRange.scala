package lila.insight

import chess.IntRating
import lila.rating.Glicko

opaque type PeersRatingRange = PairOf[IntRating]

object PeersRatingRange extends TotalWrapper[PeersRatingRange, PairOf[IntRating]]:

  private val distribution =
    // > db.insight.estimatedDocumentCount()
    // 2853219252
    // > db.insight.aggregate([{$sample:{size:50000000}},{$project:{_id:0,mr:1}},{$match:{mr:{$exists:1}}},{$group:{_id:null,ratings:{$avg:'$mr'}}}])
    // { "_id" : null, "ratings" : 1823.3090773968 }
    // > db.insight.aggregate([{$sample:{size:50000000}},{$project:{_id:0,mr:1}},{$match:{mr:{$exists:1}}},{$group:{_id:null,ratings:{$stdDevSamp:'$mr'}}}])
    // { "_id" : null, "ratings" : 364.74285981482296 }
    lila.rating.Gaussian(1823.31, 364.74d)

  private val minRating = Glicko.minRating
  private val maxRating = IntRating(3200)

  def of(rating: MeanRating): PeersRatingRange =
    val normalize = rating.into(IntRating).atLeast(minRating).atMost(maxRating)
    val cdf = distribution.cdf(normalize.value) // percentile of player given rating
    val proportion = 0.001 // probability density function, i.e. proportion of players
    val (lower, upper) = (cdf - proportion, cdf + proportion)
    expandToMinWidth(IntRating(30)):
      center(rating.into(IntRating)):
        (
          IntRating(distribution.inverseCdf(lower.atLeast(0)).toInt).atLeast(minRating),
          IntRating(distribution.inverseCdf(upper.atMost(1)).toInt).atMost(maxRating)
        )

  private def center(rating: IntRating)(range: PeersRatingRange): PeersRatingRange =
    val (low, up) = range.pairMap(_.value)
    val maxDist = (rating.value - low).abs.atLeast((up - rating.value).abs)
    (
      IntRating(rating.value - maxDist).atLeast(minRating),
      IntRating(rating.value + maxDist).atMost(maxRating)
    )

  private def expandToMinWidth(minWidth: IntRating)(range: PeersRatingRange): PeersRatingRange =
    val (low, up) = range
    val currentWidth = up - low
    if currentWidth >= minWidth then range
    else
      val extra = (minWidth - currentWidth).map(_ / 2)
      ((low - extra), (up + extra))
