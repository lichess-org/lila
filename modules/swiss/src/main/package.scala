package lila.swiss

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

val lichessTeamId = TeamId("lichess-swiss")

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
