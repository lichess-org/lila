package lila.swiss

import chess.IntRating
import chess.rating.RatingProvisional

class SwissScoringTest extends munit.FunSuite:

  import SwissScoring.*

  def compute(nbRounds: Int, players: List[SwissPlayer], pairings: List[SwissPairing]) =
    val rounds = SwissRoundNumber.from((1 to nbRounds).toList)
    val pairingMap = SwissPairing.toMap(pairings)
    val sheets = SwissSheet.many(rounds, players, pairingMap)
    val withSheets = players.zip(sheets).map(SwissSheet.OfPlayer.withSheetPoints)
    computePlayers(SwissRoundNumber(nbRounds), withSheets, pairingMap)

  test("empty"):
    assertEquals(compute(1, Nil, Nil), Nil)

  test("one round, one win"):
    val players = List(player('a'), player('b'))
    val pairings = List(
      pairing(1, 'a', 'b', "1-0")
    )
    compute(1, players, pairings).map(_.pointsAndTieBreak) match
      case List(a, b) =>
        assertEquals(a, (1f, 0f))
        assertEquals(b, (0f, 0f))
      case _ => fail("expected 2 players")

  test("one round, one draw"):
    val players = List(player('a'), player('b'))
    val pairings = List(
      pairing(1, 'a', 'b', "1/2-1/2")
    )
    compute(1, players, pairings).map(_.pointsAndTieBreak) match
      case List(a, b) =>
        assertEquals(a, (0.5f, 0.25f))
        assertEquals(b, (0.5f, 0.25f))
      case _ => fail("expected 2 players")

  test("two rounds"):
    val players = List(player('a'), player('b'))
    val pairings = List(
      pairing(1, 'a', 'b', "1-0"),
      pairing(2, 'b', 'a', "1/2-1/2")
    )
    compute(2, players, pairings).map(_.pointsAndTieBreak) match
      case List(a, b) =>
        assertEquals(a, (1.5f, 0.75f))
        assertEquals(b, (0.5f, 0.75f))
      case _ => fail("expected 2 players")

  test("three rounds"):
    val players = List(player('a'), player('b'))
    val pairings = List(
      pairing(1, 'a', 'b', "1-0"),
      pairing(2, 'b', 'a', "1/2-1/2"),
      pairing(3, 'a', 'b', "1-0")
    )
    compute(3, players, pairings).map(_.pointsAndTieBreak) match
      case List(a, b) =>
        assertEquals(a, (2.5f, 1.25f))
        assertEquals(b, (0.5f, 1.25f))
      case _ => fail("expected 2 players")

  test("three rounds, three players, one gets a bye on first round"):
    val players = List(player('a'), player('b'), player('c', Set(1)))
    val pairings = List(
      pairing(1, 'a', 'b', "1-0"),
      pairing(2, 'c', 'a', "1-0"),
      pairing(3, 'b', 'c', "1-0")
    )
    compute(3, players, pairings).map(_.pointsAndTieBreak) match
      case List(a, b, c) =>
        assertEquals(a, (1f, 1f))
        assertEquals(b, (1f, 2f))
        assertEquals(c, (2f, 2f))
      case _ => fail("expected 3 players")

  test("three rounds, three players, three byes"):
    val players = List(player('a', Set(3)), player('b', Set(2)), player('c', Set(1)))
    val pairings = List(
      pairing(1, 'a', 'b', "1-0"),
      pairing(2, 'c', 'a', "1-0"),
      pairing(3, 'b', 'c', "1-0")
    )
    compute(3, players, pairings).map(_.pointsAndTieBreak) match
      case List(a, b, c) =>
        assertEquals(a, (2f, 3f))
        assertEquals(b, (2f, 2.5f))
        assertEquals(c, (2f, 3f))
      case _ => fail("expected 3 players")

  extension (e: SwissPlayer)
    def pointsAndTieBreak: (Float, Float) = (e.points.value, e.tieBreak.value.toFloat)
    def print =
      println(e); e

  def makeUserId(name: Char) = UserId(s"user-$name")
  def makeSwissId = SwissId("swissId")
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
    byes = SwissRoundNumber.from(byes)
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
