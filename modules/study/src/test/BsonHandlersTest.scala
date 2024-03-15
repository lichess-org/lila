package lila.study
import chess.format.pgn.PgnStr

import scala.language.implicitConversions

import lila.db.BSON
import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl.Bdoc
import lila.tree.{ NewRoot, Root }

import BSONHandlers.given

// in lila.study to have access to PgnImport
class BsonHandlersTest extends munit.FunSuite:

  given Conversion[String, PgnStr] = PgnStr(_)
  given Conversion[PgnStr, String] = _.value
  given Conversion[Bdoc, Reader]   = Reader(_)

  import Helpers.*

  val treeBson    = summon[BSON[Root]]
  val newTreeBson = summon[BSON[NewRoot]]

  val w = new Writer

  test("Tree writes.reads == identity"):
    PgnFixtures.all.foreach: pgn =>
      val x = PgnImport(pgn, Nil).toOption.get.root
      val y = treeBson.reads(treeBson.writes(w, x))
      assertEquals(x, y)

  test("NewTree writes.reads == identity"):
    PgnFixtures.all.foreach: pgn =>
      val x = NewPgnImport(pgn, Nil).toOption.get.root
      val y = newTreeBson.reads(newTreeBson.writes(w, x))
      assertEquals(x, y)

  test("NewTree.reads.Tree.writes == identity"):
    PgnFixtures.all.foreach: pgn =>
      val x       = PgnImport(pgn, Nil).toOption.get.root
      val bdoc    = treeBson.writes(w, x)
      val y       = newTreeBson.reads(bdoc)
      val oldRoot = NewRootC.fromRoot(x)
      assertEquals(oldRoot.cleanup, y.cleanup)

  test("Tree.reads.NewTree.writes == identity"):
    PgnFixtures.all.foreach: pgn =>
      val x       = NewPgnImport(pgn, Nil).toOption.get.root
      val bdoc    = newTreeBson.writes(w, x)
      val y       = treeBson.reads(bdoc)
      val oldRoot = NewRootC.fromRoot(y)
      assertEquals(oldRoot.cleanup, x.cleanup)
