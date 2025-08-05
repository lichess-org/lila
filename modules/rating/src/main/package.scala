package lila.rating

import alleycats.Zero
import chess.IntRating

export lila.core.lilaism.Lilaism.{ Perf as _, *, given }
export lila.common.extensions.*

type UserRankMap = Map[PerfKey, Int]

val formMapping: play.api.data.Mapping[IntRating] =
  import play.api.data.Forms.number
  import lila.common.Form.into
  number(min = Glicko.minRating.value, max = Glicko.maxRating.value).into[IntRating]

val ratingApi: lila.ui.RatingApi = new:
  val toNameKey = PerfType(_).nameKey
  val toDescKey = PerfType(_).descKey
  val toIcon = PerfType(_).icon
  val bestRated = UserPerfsExt.bestRatedPerf
  val dubiousPuzzle = UserPerfs.dubiousPuzzle
