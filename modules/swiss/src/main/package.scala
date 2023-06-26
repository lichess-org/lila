package lila.swiss

export lila.Lila.{ *, given }

val lichessTeamId = TeamId("lichess-swiss")

type Ranking = Map[UserId, Rank]

private val logger = lila.log("swiss")

// FIDE TRF player IDs
private type PlayerIds = Map[UserId, Int]
private type IdPlayers = Map[Int, UserId]

opaque type SwissPoints = Int
object SwissPoints:
  def fromDoubled(d: Int): SwissPoints = d
  extension (p: SwissPoints)
    def doubled: Int                   = p
    def value: Float                   = p / 2f
    def +(o: SwissPoints): SwissPoints = SwissPoints.doubled(p + o)
