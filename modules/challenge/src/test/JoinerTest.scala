package lila.challenge

import chess.variant.{ FromPosition, Standard }
import chess.{ Clock, Ply }

final class JoinerTest extends munit.FunSuite:

  val timeControl =
    Challenge.TimeControl.Clock(Clock.Config(Clock.LimitSeconds(300), Clock.IncrementSeconds(0)))

  test("started at turn 0"):
    val challenge = Challenge.make(
      variant = Standard,
      initialFen = None,
      timeControl = timeControl,
      rated = chess.Rated.No,
      color = "white",
      challenger = Challenge.Challenger.Anonymous("secret"),
      destUser = None,
      rematchOf = None
    )
    assertEquals(ChallengeJoiner.createGame(challenge, None, None).chess.startedAtPly, Ply.initial)
  test("started at turn from position"):
    val position = "r1bqkbnr/ppp2ppp/2npp3/8/8/2NPP3/PPP2PPP/R1BQKBNR w KQkq - 2 4"
    val challenge = Challenge.make(
      variant = FromPosition,
      initialFen = Some(chess.format.Fen.Full(position)),
      timeControl = timeControl,
      rated = chess.Rated.No,
      color = "white",
      challenger = Challenge.Challenger.Anonymous("secret"),
      destUser = None,
      rematchOf = None
    )
    assertEquals(ChallengeJoiner.createGame(challenge, None, None).chess.startedAtPly, Ply(6))
