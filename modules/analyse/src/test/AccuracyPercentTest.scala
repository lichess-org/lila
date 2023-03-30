package lila.analyse
import org.specs2.mutable.Specification

import chess.Color
import lila.tree.Eval.Cp

class AccuracyPercentTest extends Specification {

  import AccuracyPercent._
  type AccMap = Color.Map[AccuracyPercent]

  "game accuracy" >> {

    def compute(cps: List[Int]): Option[AccMap] =
      gameAccuracy(Color.white, cps.map(Cp(_)))

    "empty game" >> {
      compute(Nil) must beNone
    }
    "single move" >> {
      compute(List(15)) must beNone
    }
    "two good moves" >> {
      compute(List(15, 15)) must beSome.like { (a: AccMap) =>
        a.white.value must beCloseTo(100d, 1d)
        a.black.value must beCloseTo(100d, 1d)
      }
    }
    "white blunders on first move" >> {
      compute(List(-900, -900)) must beSome.like { a =>
        a.white.value must beCloseTo(10d, 5d)
        a.black.value must beCloseTo(100d, 1d)
      }
    }
    "black blunders on first move" >> {
      compute(List(15, 900)) must beSome.like { a =>
        a.white.value must beCloseTo(100d, 1d)
        a.black.value must beCloseTo(10d, 5d)
      }
    }
    "both blunder on first move" >> {
      compute(List(-900, 0)) must beSome.like { a =>
        a.white.value must beCloseTo(10d, 5d)
        a.black.value must beCloseTo(10d, 5d)
      }
    }
    "20 perfect moves" >> {
      compute(List.fill(20)(15)) must beSome.like { a =>
        a.white.value must beCloseTo(100d, 1d)
        a.black.value must beCloseTo(100d, 1d)
      }
    }
    "20 perfect moves and a white blunder" >> {
      compute(List.fill(20)(15) :+ -900) must beSome.like { a =>
        a.white.value must beCloseTo(50d, 5d)
        a.black.value must beCloseTo(100d, 1d)
      }
    }
    "21 perfect moves and a black blunder" >> {
      compute(List.fill(21)(15) :+ 900) must beSome.like { a =>
        a.white.value must beCloseTo(100d, 1d)
        a.black.value must beCloseTo(50d, 5d)
      }
    }
    "5 average moves (65 cpl) on each side" >> {
      compute(List.fill(5)(List(-50, 15)).flatten) must beSome.like { a =>
        a.white.value must beCloseTo(76d, 8d)
        a.black.value must beCloseTo(76d, 8d)
      }
    }
    "50 average moves (65 cpl) on each side" >> {
      compute(List.fill(50)(List(-50, 15)).flatten) must beSome.like { a =>
        a.white.value must beCloseTo(76d, 8d)
        a.black.value must beCloseTo(76d, 8d)
      }
    }
    "50 mediocre moves (150 cpl) on each side" >> {
      compute(List.fill(50)(List(-135, 15)).flatten) must beSome.like { a =>
        a.white.value must beCloseTo(54d, 8d)
        a.black.value must beCloseTo(54d, 8d)
      }
    }
    "50 terrible moves (500 cpl) on each side" >> {
      compute(List.fill(50)(List(-435, 15)).flatten) must beSome.like { a =>
        a.white.value must beCloseTo(20d, 8d)
        a.black.value must beCloseTo(20d, 8d)
      }
    }
  }

  "game accuracy, black moves first" >> {

    def compute(cps: List[Int]) = gameAccuracy(Color.black, cps.map(Cp(_)))

    "empty game" >> {
      compute(Nil) must beNone
    }
    "single move" >> {
      compute(List(15)) must beNone
    }
    "two good moves" >> {
      compute(List(15, 15)) must beSome.like { a =>
        a.black.value must beCloseTo(100d, 1d)
        a.white.value must beCloseTo(100d, 1d)
      }
    }
    "black blunders on first move" >> {
      compute(List(900, 900)) must beSome.like { a =>
        a.black.value must beCloseTo(10d, 5d)
        a.white.value must beCloseTo(100d, 1d)
      }
    }
    "white blunders on first move" >> {
      compute(List(15, -900)) must beSome.like { a =>
        a.black.value must beCloseTo(100d, 1d)
        a.white.value must beCloseTo(10d, 5d)
      }
    }
    "both blunder on first move" >> {
      compute(List(900, 0)) must beSome.like { a =>
        a.black.value must beCloseTo(10d, 5d)
        a.white.value must beCloseTo(10d, 5d)
      }
    }
  }

}
