package lila.history

export lila.Lila.{ *, given }

private type ID         = String
private type Date       = Int
private type RatingsMap = List[(Date, IntRating)]
