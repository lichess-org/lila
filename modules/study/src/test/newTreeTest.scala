package lila.study

import chess.Node as PgnNode
import chess.format.pgn.{PgnStr, Tags}

import scala.language.implicitConversions

import lila.tree.{NewBranch, NewRoot, Root}

// in lila.study to have access to PgnImport
class NewTreeTest extends munit.FunSuite:

  import PgnImport.*
  import Helpers.*

  given Conversion[String, PgnStr] = PgnStr(_)
  given Conversion[PgnStr, String] = _.value

  test("valid tree -> newTree first move"):
    val x       = PgnImport("1. e4 *", Nil).toOption.get
    val newRoot = NewRootC.fromRoot(x.root)
    assertEquals(newRoot.tree.get.size, 1L)
    assertEquals(newRoot.tree.get.mainline.map(sanStr), List("e4"))
    assertEquals(newRoot.toRoot, x.root)

  test("valid tree -> newTree first move with variation"):
    val x       = PgnImport("1. e4 (1. d4??) *", Nil).toOption.get
    val newRoot = NewRootC.fromRoot(x.root)
    assertEquals(newRoot.tree.get.size, 2L)
    assertEquals(newRoot.tree.get.variations.map(sanStr), List("d4"))
    assertEquals(newRoot.toRoot, x.root)

  test("valid tree -> newTree two moves"):
    val x       = PgnImport("1. e4 e6 *", Nil).toOption.get
    val newRoot = NewRootC.fromRoot(x.root)
    assertEquals(newRoot.tree.get.size, 2L)
    assertEquals(newRoot.tree.get.mainline.map(sanStr), List("e4", "e6"))
    assertEquals(newRoot.toRoot, x.root)

  test("valid tree <-> newTree more realistic conversion"):
    PgnFixtures.all.foreach: pgn =>
      val x       = PgnImport(pgn, Nil).toOption.get
      val newRoot = NewRootC.fromRoot(x.root)
      assertEquals(newRoot.toRoot, x.root)

  test("PgnImport works"):
    PgnFixtures.all.foreach: pgn =>
      val x = PgnImport(pgn, Nil).toOption.get
      val y = NewPgnImport(pgn, Nil).toOption.get
      assertEquals(y.end, x.end)
      assertEquals(y.variant, x.variant)
      assertEquals(y.tags, x.tags)
      val oldRoot = NewRootC.fromRoot(x.root).cleanup
      assertEquals(y.root.cleanup, oldRoot)
