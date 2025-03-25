package lila.analyse

import chess.{ ByColor, Color }
import chess.eval.Eval.Cp
import scalalib.Maths.isCloseTo

class AccuracyPercentTest extends munit.FunSuite:

  import AccuracyPercent.*
  type AccMap = ByColor[AccuracyPercent]

  def compute(cps: List[Int]): Option[AccMap] =
    gameAccuracy(Color.white, cps.map(Cp(_)))

  test("empty game"):
    assertEquals(compute(Nil), None)
  test("single move"):
    assertEquals(compute(List(15)), None)
  test("two good moves"):
    val a = compute(List(15, 15)).get
    assert(isCloseTo(a.white.value, 100d, 1))
    assert(isCloseTo(a.black.value, 100d, 1))
  test("white blunders on first move"):
    val a = compute(List(-900, -900)).get
    assert(isCloseTo(a.white.value, 10d, 5d))
    assert(isCloseTo(a.black.value, 100d, 1d))
  test("black blunders on first move"):
    val a = compute(List(15, 900)).get
    assert(isCloseTo(a.white.value, 100d, 1d))
    assert(isCloseTo(a.black.value, 10d, 5d))
  test("both blunder on first move"):
    val a = compute(List(-900, 0)).get
    assert(isCloseTo(a.white.value, 10d, 5d))
    assert(isCloseTo(a.black.value, 10d, 5d))
  test("20 perfect moves"):
    val a = compute(List.fill(20)(15)).get
    assert(isCloseTo(a.white.value, 100d, 1d))
    assert(isCloseTo(a.black.value, 100d, 1d))
  test("20 perfect moves and a white blunder"):
    val a = compute(List.fill(20)(15) :+ -900).get
    assert(isCloseTo(a.white.value, 50d, 5d))
    assert(isCloseTo(a.black.value, 100d, 1d))
  test("21 perfect moves and a black blunder"):
    val a = compute(List.fill(21)(15) :+ 900).get
    assert(isCloseTo(a.white.value, 100d, 1d))
    assert(isCloseTo(a.black.value, 50d, 5d))
  test("5 average moves (65 cpl) on each side"):
    val a = compute(List.fill(5)(List(-50, 15)).flatten).get
    assert(isCloseTo(a.white.value, 76d, 8d))
    assert(isCloseTo(a.black.value, 76d, 8d))
  test("50 average moves (65 cpl) on each side"):
    val a = compute(List.fill(50)(List(-50, 15)).flatten).get
    assert(isCloseTo(a.white.value, 76d, 8d))
    assert(isCloseTo(a.black.value, 76d, 8d))
  test("50 mediocre moves (150 cpl) on each side"):
    val a = compute(List.fill(50)(List(-135, 15)).flatten).get
    assert(isCloseTo(a.white.value, 54d, 8d))
    assert(isCloseTo(a.black.value, 54d, 8d))
  test("50 terrible moves (500 cpl) on each side"):
    val a = compute(List.fill(50)(List(-435, 15)).flatten).get
    assert(isCloseTo(a.white.value, 20d, 8d))
    assert(isCloseTo(a.black.value, 20d, 8d))

  def computeBlack(cps: List[Int]) = gameAccuracy(Color.black, cps.map(Cp(_)))

  test("black moves first, empty game"):
    assertEquals(computeBlack(Nil), None)
  test("black moves first, single move"):
    assertEquals(computeBlack(List(15)), None)
  test("black moves first, two good moves"):
    val a = computeBlack(List(15, 15)).get
    assert(isCloseTo(a.black.value, 100d, 1d))
    assert(isCloseTo(a.white.value, 100d, 1d))
  test("black moves first, black blunders on first move"):
    val a = computeBlack(List(900, 900)).get
    assert(isCloseTo(a.black.value, 10d, 5d))
    assert(isCloseTo(a.white.value, 100d, 1d))
  test("black moves first, white blunders on first move"):
    val a = computeBlack(List(15, -900)).get
    assert(isCloseTo(a.black.value, 100d, 1d))
    assert(isCloseTo(a.white.value, 10d, 5d))
  test("black moves first, both blunder on first move"):
    val a = computeBlack(List(900, 0)).get
    assert(isCloseTo(a.black.value, 10d, 5d))
    assert(isCloseTo(a.white.value, 10d, 5d))
