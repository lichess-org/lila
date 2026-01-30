package lila.relay

class RelayPlayerTest extends munit.FunSuite:

  test("dr. and prof."):
    assertEquals(RelayPlayerLine.tokenize("Pieper, Thomas Dr."), "pieper thomas")
    assertEquals(RelayPlayerLine.tokenize("Dr. Pieper Thomas"), "pieper thomas")
    assertEquals(RelayPlayerLine.tokenize("Prof. Pieper Thomas"), "pieper thomas")
    assertEquals(RelayPlayerLine.tokenize("Prof. Pieper Thomas Dr."), "pieper thomas")

  test("chess title"):
    assertEquals(RelayPlayerLine.tokenize("GM Pieper Thomas"), "pieper thomas")
    assertEquals(RelayPlayerLine.tokenize("WIM Allahverdiyeva, Ayan"), "allahverdiyeva ayan")
    assertEquals(RelayPlayerLine.tokenize("Alice the BOT"), "alice bot the")
    assertEquals(RelayPlayerLine.tokenize("Some IM name"), "im name some")

  test("comma"):
    assertEquals(RelayPlayerLine.tokenize("Zimmer, Gerald"), "gerald zimmer")
    assertEquals(RelayPlayerLine.tokenize("Zimmer,Gerald"), "gerald zimmer")

  test("sorting by score"):
    val p1 = RelayPlayer(
      player = dummyPlayer("Alice", 2000),
      ratingsMap = ratingsMap(2000),
      score = Some(3.0f),
      ratingDiffs = Map.empty,
      performances = Map.empty,
      tiebreaks = None,
      rank = None,
      games = Vector.empty
    )
    val p2 = p1.copy(player = dummyPlayer("Bob", 2100), score = Some(4.0f))
    val p3 = p1.copy(player = dummyPlayer("Carol", 2200), score = Some(2.0f))
    val sorted = List(p1, p2, p3).sorted
    assertEquals(sorted.map(_.player.player.name.map(_.value)), List("Bob".some, "Alice".some, "Carol".some))

  test("sorting by tiebreakpoints"):
    import chess.tiebreak.*
    val tiebreaks = Tiebreak.preset.take(2)
    val tb1 = Seq(tiebreaks(0) -> TiebreakPoint(10), tiebreaks(1) -> TiebreakPoint(5))
    val tb2 = Seq(tiebreaks(0) -> TiebreakPoint(12), tiebreaks(1) -> TiebreakPoint(4))
    val p1 = RelayPlayer(
      dummyPlayer("Alice", 2000),
      ratingsMap(2000),
      Some(3.0f),
      Map.empty,
      Map.empty,
      Some(tb1),
      None,
      Vector.empty
    )
    val p2 = RelayPlayer(
      dummyPlayer("Bob", 2100),
      ratingsMap(2100),
      Some(3.0f),
      Map.empty,
      Map.empty,
      Some(tb2),
      None,
      Vector.empty
    )
    val sorted = List(p1, p2).sorted
    assertEquals(sorted.map(_.player.player.name.map(_.value)), List("Bob".some, "Alice".some))

  test("sorting by rating"):
    val p1 = RelayPlayer(
      dummyPlayer("Alice", 2000),
      ratingsMap(2000),
      Some(3.0f),
      Map.empty,
      Map.empty,
      None,
      None,
      Vector.empty
    )
    val p2 = RelayPlayer(
      dummyPlayer("Bob", 2100),
      ratingsMap(2100),
      Some(3.0f),
      Map.empty,
      Map.empty,
      None,
      None,
      Vector.empty
    )
    val sorted = List(p1, p2).sorted
    assertEquals(sorted.map(_.player.player.name.map(_.value)), List("Bob".some, "Alice".some))

  test("sorting by name"):
    val p1 = RelayPlayer(
      dummyPlayer("Alice", 2000),
      ratingsMap(2000),
      Some(3.0f),
      Map.empty,
      Map.empty,
      None,
      None,
      Vector.empty
    )
    val p2 = RelayPlayer(
      dummyPlayer("Bob", 2000),
      ratingsMap(2000),
      Some(3.0f),
      Map.empty,
      Map.empty,
      None,
      None,
      Vector.empty
    )
    val sorted = List(p2, p1).sorted
    assertEquals(sorted.map(_.player.player.name.map(_.value)), List("Alice".some, "Bob".some))

  def dummyPlayer(name: String, rating: Int): lila.study.StudyPlayer.WithFed =
    import lila.study.StudyPlayer
    StudyPlayer.WithFed(
      player = StudyPlayer(
        fideId = None,
        name = Some(chess.PlayerName(name)),
        rating = Some(chess.IntRating(rating)),
        title = None,
        team = None
      ),
      fed = None
    )

  def ratingsMap(rating: Int): Map[chess.FideTC, chess.IntRating] =
    Map(chess.FideTC.standard -> chess.IntRating(rating))
