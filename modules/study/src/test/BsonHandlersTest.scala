package lila.study

import chess.format.pgn.PgnStr

import scala.language.implicitConversions

import lila.db.BSON
import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl.Bdoc
import lila.study.BSONHandlers.given
import lila.tree.{ NewRoot, Root }

// in lila.study to have access to PgnImport
class BsonHandlersTest extends munit.FunSuite:

  given Conversion[String, PgnStr] = PgnStr(_)
  given Conversion[PgnStr, String] = _.value
  given Conversion[Bdoc, Reader] = Reader(_)

  import Helpers.*

  val treeBson = summon[BSON[Root]]
  val newTreeBson = summon[BSON[NewRoot]]
  val w = new Writer

  test("Tree writes.reads == identity"):
    List(PgnFixtures.pgn8).foreach: pgn =>
      val x = StudyPgnImport.result(pgn, Nil).toOption.get.root
      val y = treeBson.reads(treeBson.writes(w, x))
      assertEquals(y, x.withoutClockTrust)

  test("NewTree writes.reads == identity".ignore):
    PgnFixtures.all.foreach: pgn =>
      val x = StudyPgnImportNew(pgn, Nil).toOption.get.root
      val y = newTreeBson.reads(newTreeBson.writes(w, x))
      assertEquals(x.withoutClockTrust, y)

  test("NewTree.reads.Tree.writes == identity".ignore):
    PgnFixtures.all.foreach: pgn =>
      val x = StudyPgnImport.result(pgn, Nil).toOption.get.root
      val bdoc = treeBson.writes(w, x)
      val y = newTreeBson.reads(bdoc)
      assertEquals(x.toNewRoot.cleanup.withoutClockTrust, y.cleanup)

  test("Tree.reads.NewTree.writes == identity".ignore):
    PgnFixtures.all.foreach: pgn =>
      val x = StudyPgnImportNew(pgn, Nil).toOption.get.root
      val bdoc = newTreeBson.writes(w, x)
      val y = treeBson.reads(bdoc)
      assertEquals(y.toNewRoot.cleanup, x.cleanup.withoutClockTrust)
