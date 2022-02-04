package shogi

class HistoryTest extends ShogiTest {

  "fourfold repetition" should {
    def toHash(a: Int) = Array(a.toByte, 0.toByte, 0.toByte)
    def makeHistory(positions: List[Int]) =
      (positions map toHash).foldLeft(History.empty) { case (history, hash) =>
        history.copy(positionHashes = hash ++ history.positionHashes)
      }
    "empty history" in {
      History.empty.fourfoldRepetition must_== false
    }
    "not 4 same elements" in {
      val history = makeHistory(List(1, 2, 3, 4, 5, 2, 5, 6, 16, 2, 23, 55))
      history.fourfoldRepetition must_== false
    }
    "not 4 elements same to the last one" in {
      val history = makeHistory(List(1, 2, 3, 4, 5, 2, 5, 6, 23, 2, 55, 2, 33))
      history.fourfoldRepetition must_== false
    }
    "positive" in {
      val history = makeHistory(List(1, 2, 3, 4, 5, 6, 7, 2, 5, 6, 3, 2, 6, 2))
      history.fourfoldRepetition must_== true
    }
  }
}
