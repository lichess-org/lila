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

  test("one round, one win"):
    val players = List(player('a'), player('b'))
    val pairings = List(
      pairing(1, 'a', 'b', "1-0")
    )
    compute(1, players, pairings) match
      case List(a, b) =>
        assertEquals(a.points.value, 1f)
        assertEquals(b.points.value, 0f)
      case _ => fail("expected 2 players")

  test("one round, one draw"):
    val players = List(player('a'), player('b'))
    val pairings = List(
      pairing(1, 'a', 'b', "1/2-1/2")
    )
    compute(1, players, pairings) match
      case List(a, b) =>
        assertEquals(a.points.value, 0.5f)
        assertEquals(b.points.value, 0.5f)
      case _ => fail("expected 2 players")

  test("two rounds"):
    val players = List(player('a'), player('b'))
    val pairings = List(
      pairing(1, 'a', 'b', "1-0"),
      pairing(2, 'b', 'a', "1/2-1/2")
    )
    compute(2, players, pairings) match
      case List(a, b) =>
        assertEquals(a.points.value, 1.5f)
        assertEquals(b.points.value, 0.5f)
      case _ => fail("expected 2 players")

  test("three rounds"):
    val players = List(player('a'), player('b'))
    val pairings = List(
      pairing(1, 'a', 'b', "1-0"),
      pairing(2, 'b', 'a', "1/2-1/2"),
      pairing(3, 'a', 'b', "1-0")
    )
    compute(3, players, pairings) match
      case List(a, b) =>
        assertEquals(a.points.value, 2.5f)
        assertEquals(b.points.value, 0.5f)
      case _ => fail("expected 2 players")

  test("three rounds, three players"):
    val players = List(player('a', Set(3)), player('b', Set(2)), player('c', Set(1)))
    val pairings = List(
      pairing(1, 'a', 'b', "1-0"),
      pairing(2, 'c', 'a', "1-0"),
      pairing(3, 'b', 'c', "1-0")
    )
    compute(3, players, pairings) match
      case List(a, b, c) =>
        assertEquals(a.pointsAndTieBreak, (2f, 2f))
        assertEquals(b.pointsAndTieBreak, (2f, 2f))
        assertEquals(c.pointsAndTieBreak, (2f, 2f))
      case _ => fail("expected 3 players")

  extension (e: SwissPlayer)
    def pointsAndTieBreak: (Float, Float) = (e.points.value, e.tieBreak.value.toFloat)
    def print                             = { println(e); e }

  def makeUserId(name: Char) = UserId(s"user-$name")
  def makeSwissId            = SwissId("swissId")
  def player(name: Char, byes: Set[Int] = Set.empty) = SwissPlayer(
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
    byes = SwissRoundNumber from byes
  )
  def pairing(round: Int, white: Char, black: Char, outcome: String) = SwissPairing(
    id = GameId(s"game-$round$white$black"),
    swissId = makeSwissId,
    round = SwissRoundNumber(round),
    white = makeUserId(white),
    black = makeUserId(black),
    status = Right(chess.Outcome.fromResult(outcome).flatMap(_.winner)),
    isForfeit = false
  )
