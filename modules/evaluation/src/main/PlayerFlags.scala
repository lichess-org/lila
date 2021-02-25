package lila.evaluation

import lila.db.dsl._
import lila.db.BSONReadOnly

case class PlayerFlags(
    suspiciousErrorRate: Boolean,
    alwaysHasAdvantage: Boolean,
    highBlurRate: Boolean,
    moderateBlurRate: Boolean,
    highlyConsistentMoveTimes: Boolean,
    moderatelyConsistentMoveTimes: Boolean,
    noFastMoves: Boolean,
    suspiciousHoldAlert: Boolean
)
