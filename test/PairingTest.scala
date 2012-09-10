package lila
package tournament

import tournament._

class PairingTest extends LilaSpec {

  import Pairing._

  val u1 = "u1"
  val u2 = "u2"
  val u3 = "u3"
  val u4 = "u4"
  val u5 = "u5"

  "Create new pairings" should {
    "2 players" in {
      val users = List(u1, u2)
      "first pairing" in {
        val pairings = List[Pairing]()
        pairs(users, pairings) must_== List(u1 -> u2)
      }
      "finished pairing" in {
        val pairings = List(mate(u1, u2))
        pairs(users, pairings) must_== List(u1 -> u2)
      }
      "started pairing" in {
        val pairings = List(started(u1, u2))
        pairs(users, pairings) must_== Nil
      }
      "finished and started pairing" in {
        val pairings = List(mate(u1, u2), started(u1, u2))
        pairs(users, pairings) must_== Nil
      }
    }
  }
  "3 players" should {
    val users = List(u1, u2, u3)
    "first pairing" in {
      val pairings = List[Pairing]()
      pairs(users, pairings) must beOneOf(List(u1 -> u2), List(u1 -> u3), List(u2 -> u3))
    }
    "finished pairing" in {
      val pairings = List(mate(u1, u2))
      pairs(users, pairings) must_== List(u1 -> u3)
    }
    "started pairing" in {
      val pairings = List(started(u1, u2))
      pairs(users, pairings) must_== Nil
    }
    "finished and started pairing" in {
      val pairings = List(mate(u1, u2), started(u1, u3))
      pairs(users, pairings) must_== Nil
    }
    "many finished pairings" in {
      "without ambiguity" in {
        val pairings = List(mate(u1, u2), mate(u1, u3))
        pairs(users, pairings) must_== List(u2 -> u3)
      }
      "favor longer idle" in {
        val pairings = List(mate(u1, u2), mate(u1, u3), mate(u2, u3))
        pairs(users, pairings) must_== List(u1 -> u2)
      }
    }
  }
  "4 players" should {
    val users = List(u1, u2, u3, u4)
    "finished pairing" in {
      val pairings = List(mate(u1, u2))
      pairs(users, pairings) must_== List(u3 -> u4)
    }
    "started pairing" in {
      val pairings = List(started(u1, u2))
      pairs(users, pairings) must_== List(u3 -> u4)
    }
    "finished and started pairing" in {
      val pairings = List(mate(u1, u2), started(u3, u4))
      pairs(users, pairings) must_== Nil
    }
  }

  private def pairs(users: List[String], pairings: Pairings) =
    createNewPairings(users, pairings) map (_.usersPair) map {
      case (a, b) ⇒ (a < b).fold(a -> b, b -> a)
    }

  private def started(a: String, b: String) =
    Pairing(a, b) withStatus chess.Status.Started
  private def mate(a: String, b: String) =
    Pairing(a, b) withStatus chess.Status.Mate
}
