package lila.history

import chess.IntRating
export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private type ID         = String
private type Date       = Int
private type RatingsMap = List[(Date, IntRating)]
