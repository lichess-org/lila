package lila.swiss

class SwissScoringTest extends munit.FunSuite:

  import SwissScoring.*

  def compute(nbRounds: Int, players: List[SwissPlayer], pairings: List[SwissPairing]) =
    val rounds     = SwissRoundNumber from (1 to nbRounds).toList
    val pairingMap = SwissPairing.toMap(pairings)
    val sheets     = SwissSheet.many(rounds, players, pairingMap)
    val withPoints = (players zip sheets).map: (player, sheet) =>
      player.copy(points = sheet.points)
    computePlayers(withPoints, pairingMap)

  test("empty"):
    assertEquals(compute(1, Nil, Nil), Nil)

  test("one round"):
    val players = List(
      makePlayer('a'),
      makePlayer('b')
    )
    val pairings = List(
      makePairing(1, 'a', 'b', "1-0")
    )
    compute(1, players, pairings) match
      case List(pa, pb) =>
        assertEquals(pa.points.value, 1f)
        assertEquals(pb.points.value, 0f)
      case _ => fail("expected 2 players")

  def makeUserId(name: Char) = UserId(s"user-$name")
  def makeSwissId            = SwissId("swissId")
  def makePlayer(name: Char) = SwissPlayer(
    id = SwissPlayer.Id(s"swissId:${makeUserId(name)}"),
    swissId = makeSwissId,
    userId = makeUserId(name),
    rating = IntRating(1500),
    provisional = RatingProvisional.No,
    points = SwissPoints.fromDoubled(0),
    tieBreak = Swiss.TieBreak(0),
    performance = None,
    score = Swiss.Score(0),
    absent = false,
    byes = Set.empty
  )
  def makePairing(round: Int, white: Char, black: Char, outcome: String) = SwissPairing(
    id = GameId(s"game-$round$white$black"),
    swissId = makeSwissId,
    round = SwissRoundNumber(round),
    white = makeUserId(white),
    black = makeUserId(black),
    status = Right(chess.Outcome.fromResult(outcome).flatMap(_.winner).pp),
    isForfeit = false
  )
