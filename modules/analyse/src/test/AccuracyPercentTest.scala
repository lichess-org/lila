package lila.analyse
import org.specs2.mutable.Specification

import chess.Color
import lila.tree.Eval.{ Cp, Mate }

class AccuracyPercentTest extends Specification {

  import AccuracyPercent._

  "game accuracy" should {

    def compute(cps: List[Int]) = gameAccuracy(Color.white, cps.map(Cp.apply))

    "empty game" in {
      compute(Nil) must beNone
    }
    "single move" in {
      compute(List(15)) must beNone
    }
    "two good moves" in {
      compute(List(15, 15)) must beSome.like { a =>
        a.white.value must beCloseTo(100d, 1)
        a.black.value must beCloseTo(100d, 1)
      }
    }
    "white blunders on first move" in {
      compute(List(-900, -900)) must beSome.like { a =>
        a.white.value must beCloseTo(10d, 5)
        a.black.value must beCloseTo(100d, 1)
      }
    }
    "black blunders on first move" in {
      compute(List(15, 900)) must beSome.like { a =>
        a.white.value must beCloseTo(100d, 1)
        a.black.value must beCloseTo(10d, 5)
      }
    }
    "both blunder on first move" in {
      compute(List(-900, 0)) must beSome.like { a =>
        a.white.value must beCloseTo(10d, 5)
        a.black.value must beCloseTo(10d, 5)
      }
    }
    "20 perfect moves" in {
      compute(List.fill(20)(15)) must beSome.like { a =>
        a.white.value must beCloseTo(100d, 1)
        a.black.value must beCloseTo(100d, 1)
      }
    }
    "20 perfect moves and a white blunder" in {
      compute(List.fill(20)(15) :+ -900) must beSome.like { a =>
        a.white.value must beCloseTo(50d, 5)
        a.black.value must beCloseTo(100d, 1)
      }
    }
    "21 perfect moves and a black blunder" in {
      compute(List.fill(21)(15) :+ 900) must beSome.like { a =>
        a.white.value must beCloseTo(100d, 1)
        a.black.value must beCloseTo(50d, 5)
      }
    }
    "5 average moves (65 cpl) on each side" in {
      compute(List.fill(5)(List(-50, 15)).flatten) must beSome.like { a =>
        a.white.value must beCloseTo(76d, 8)
        a.black.value must beCloseTo(76d, 8)
      }
    }
    "50 average moves (65 cpl) on each side" in {
      compute(List.fill(50)(List(-50, 15)).flatten) must beSome.like { a =>
        a.white.value must beCloseTo(76d, 8)
        a.black.value must beCloseTo(76d, 8)
      }
    }
    "50 mediocre moves (150 cpl) on each side" in {
      compute(List.fill(50)(List(-135, 15)).flatten) must beSome.like { a =>
        a.white.value must beCloseTo(54d, 8)
        a.black.value must beCloseTo(54d, 8)
      }
    }
    "50 terrible moves (500 cpl) on each side" in {
      compute(List.fill(50)(List(-435, 15)).flatten) must beSome.like { a =>
        a.white.value must beCloseTo(20d, 8)
        a.black.value must beCloseTo(20d, 8)
      }
    }
  }

  "game accuracy, black moves first" should {

    def compute(cps: List[Int]) = gameAccuracy(Color.black, cps.map(Cp.apply))

    "empty game" in {
      compute(Nil) must beNone
    }
    "single move" in {
      compute(List(15)) must beNone
    }
    "two good moves" in {
      compute(List(15, 15)) must beSome.like { a =>
        a.black.value must beCloseTo(100d, 1)
        a.white.value must beCloseTo(100d, 1)
      }
    }
    "black blunders on first move" in {
      compute(List(900, 900)) must beSome.like { a =>
        a.black.value must beCloseTo(10d, 5)
        a.white.value must beCloseTo(100d, 1)
      }
    }
    "white blunders on first move" in {
      compute(List(15, -900)) must beSome.like { a =>
        a.black.value must beCloseTo(100d, 1)
        a.white.value must beCloseTo(10d, 5)
      }
    }
    "both blunder on first move" in {
      compute(List(900, 0)) must beSome.like { a =>
        a.black.value must beCloseTo(10d, 5)
        a.white.value must beCloseTo(10d, 5)
      }
    }
  }

}
