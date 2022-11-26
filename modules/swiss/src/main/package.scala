package lila.swiss

import lila.user.User
export lila.Lila.{ *, given }

val lichessTeamId = TeamId("lichess-swiss")

type Ranking = Map[lila.user.User.ID, Rank]

private val logger = lila.log("swiss")

// FIDE TRF player IDs
private type PlayerIds = Map[User.ID, Int]
private type IdPlayers = Map[Int, User.ID]

opaque type SwissPoints = Int
object SwissPoints:
  def fromDouble(d: Int): SwissPoints = d
  extension (p: SwissPoints)
    def doubled: Int      = p
    def value: Float      = p / 2f
    def +(o: SwissPoints) = SwissPoints.doubled(p + o)
