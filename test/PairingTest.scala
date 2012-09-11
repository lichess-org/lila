package lila
package tournament

import tournament._

import ornicar.scalalib.Random.nextString

class PairingTest extends LilaSpec {

  import Pairing._

  val u1 = "u1"
  val u2 = "u2"
  val u3 = "u3"
  val u4 = "u4"
  val u5 = "u5"
  val u6 = "u6"

  "All pair combinations" should {
    //"2 elems" in {
    //val elems = List(u1, u2)
    //allPairCombinations(elems) must contain(List(u1 -> u2)).only
    //}
    "4 elems" in {
      val elems = List(u1, u2, u3, u4)
      allPairCombinations(elems) must contain(
        List(u1 -> u2, u3 -> u4),
        List(u1 -> u3, u2 -> u4),
        List(u1 -> u4, u2 -> u3)).only
    }
    "6 elems" in {
      val elems = List(u1, u2, u3, u4, u5, u6)
      "size" in {
        allPairCombinations(elems).size must_== 15
      }
      "pairings" in {
        allPairCombinations(elems) must contain(
          List(u1 -> u2, u3 -> u4, u5 -> u6),
          List(u1 -> u2, u3 -> u5, u4 -> u6),
          List(u1 -> u2, u3 -> u6, u4 -> u5),
          List(u1 -> u3, u2 -> u4, u5 -> u6),
          List(u1 -> u3, u2 -> u5, u4 -> u6),
          List(u1 -> u3, u2 -> u6, u4 -> u5),
          List(u1 -> u4, u2 -> u3, u5 -> u6),
          List(u1 -> u4, u2 -> u5, u3 -> u6),
          List(u1 -> u4, u2 -> u6, u3 -> u5),
          List(u1 -> u4, u2 -> u3, u5 -> u6),
          List(u1 -> u5, u2 -> u3, u4 -> u6),
          List(u1 -> u6, u2 -> u3, u4 -> u5))
      }
    }
    "14 elems" in {
      val elems = List.fill(14)(nextString(8)).pp
      "size" in {
        allPairCombinations(elems).size must_== 3 * 5 * 7 * 9 * 11 * 13
      }
    }
  }
  "Create new pairings" should {
    "initial pairings" in {
      "even players" in {
        val users = List.fill(100)(nextString(8))
        pairs(users, Nil) must have size 50
      }
      "odd players" in {
        val users = List.fill(99)(nextString(8))
        pairs(users, Nil) must have size 49
      }
    }
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
      pairs(users, pairings) must beOneOf(List(u1 -> u3), List(u2 -> u3))
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
        val pairings = List(mate(u1, u3), mate(u1, u2))
        pairs(users, pairings) must beOneOf(List(u2 -> u3), List(u1 -> u2))
      }
      "favor longer idle" in {
        val pairings = List(mate(u2, u3), mate(u1, u3), mate(u1, u2))
        pairs(users, pairings) must beOneOf(List(u1 -> u2), List(u1 -> u3))
      }
    }
  }
  "4 players" should {
    val users = List(u1, u2, u3, u4)
    "finished pairing" in {
      "one finished" in {
        val pairings = List(mate(u1, u2))
        "size" in {
          pairs(users, pairings) must have size 2
        }
        "no rematch" in {
          pairs(users, pairings) must not contain(u1 -> u2)
        }
      }
      "two finished" in {
        val pairings = List(mate(u1, u2), mate(u3, u4))
        "size" in {
          pairs(users, pairings) must have size 2
        }
        "no rematch" in {
          pairs(users, pairings) must beOneOf(
            List(u1 -> u3, u2 -> u4),
            List(u1 -> u4, u2 -> u3))
        }
      }
    }
    "started pairing" in {
      val pairings = List(started(u1, u2))
      pairs(users, pairings) must_== List(u3 -> u4)
    }
    "finished and started pairing" in {
      val pairings = List(started(u3, u4), mate(u1, u2))
      pairs(users, pairings) must_== Nil
    }
  }

  private def pairs(users: List[String], pairings: Pairings): List[(String, String)] =
    createNewPairings(users, pairings) map (_.usersPair) map {
      case (a, b) ⇒ (a < b).fold(a -> b, b -> a)
    }

  private def started(a: String, b: String) =
    Pairing(a, b) withStatus chess.Status.Started
  private def mate(a: String, b: String) =
    Pairing(a, b) withStatus chess.Status.Mate
}
