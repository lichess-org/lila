package lila.insight

import chess.IntRating

opaque type PeersRatingRange = PairOf[IntRating]

object PeersRatingRange extends TotalWrapper[PeersRatingRange, PairOf[IntRating]]:

  private val distribution =
    // > db.insight.estimatedDocumentCount()
    // 2853219252
    // > db.insight.aggregate([{$sample:{size:50000000}},{$project:{_id:0,mr:1}},{$match:{mr:{$exists:1}}},{$group:{_id:null,ratings:{$avg:'$mr'}}}])
    // { "_id" : null, "ratings" : 1878.1484907826184 }
    // > db.insight.aggregate([{$sample:{size:10000000}},{$project:{_id:0,mr:1}},{$match:{mr:{$exists:1}}},{$group:{_id:null,ratings:{$stdDevSamp:'$mr'}}}])
    // { "_id" : null, "ratings" : 357.42969844387625 }
    lila.rating.Gaussian(1878d, 357d)

  def of(rating: MeanRating): PeersRatingRange =
    val cdf = distribution.cdf(rating.value) // percentile of player given rating
    val proportion = 0.01 // probability density function, i.e. proportion of players
    val (lower, upper) = (cdf - proportion, cdf + proportion)
    (
      IntRating(distribution.inverseCdf(lower.atLeast(0)).toInt.atLeast(0)),
      IntRating(distribution.inverseCdf(upper.atMost(1)).toInt.atMost(4000))
    )
