package lila.tournament

import chess.IntRating

import lila.core.chess.Rank

class DuelTest extends munit.FunSuite:

  val store = new DuelStore
  val tourId = TourId("tour")
  val p1 = Duel.DuelPlayer(UserName("p1"), IntRating(1900), Rank(1))
  val p2 = Duel.DuelPlayer(UserName("p2"), IntRating(1800), Rank(2))

  test("duel store"):
    assertEquals(store.get(tourId), None)

    val duel1 = Duel(GameId("game1"), p1, p2, IntRating(1850))

    store.add(tourId, duel1)
    assertEquals(store.get(tourId).map(_.toList), Some(List(duel1)))

    store.remove(GameId("game1"), tourId)
    assertEquals(store.get(tourId), None)
