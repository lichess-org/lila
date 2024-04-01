package lila.history

export lila.Core.{ *, given }

private type ID         = String
private type Date       = Int
private type RatingsMap = List[(Date, IntRating)]
