package lila.study
import chess.format.pgn.PgnStr

import scala.language.implicitConversions

class NewTreeTest extends munit.FunSuite:

  import Helpers.*

  given Conversion[String, PgnStr] = PgnStr(_)
  given Conversion[PgnStr, String] = _.value

  // test("tree <-> newTree conversion"):
  //   PgnFixtures.all.foreach: pgn =>
  //     val x       = StudyPgnImport.result(pgn, Nil).toOption.get
  //     val newRoot = x.root.toNewRoot
  //     assertEquals(newRoot.toRoot, x.root)

  // test("PgnImport works"):
  //   PgnFixtures.all.foreach: pgn =>
  //     val x = StudyPgnImport.result(pgn, Nil).toOption.get
  //     val y = StudyPgnImportNew(pgn, Nil).toOption.get
  //     assertEquals(y.end, x.ending)
  //     assertEquals(y.variant, x.variant)
  //     assertEquals(y.tags, x.tags)
  //     val oldRoot = x.root.toNewRoot.cleanup
  //     assertEquals(y.root.cleanup, oldRoot)
