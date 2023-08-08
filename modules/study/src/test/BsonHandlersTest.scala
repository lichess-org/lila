package lila.study

import monocle.syntax.all.*
import chess.{ Centis, ErrorStr, Node as ChessNode }
import chess.format.pgn.{ Glyphs, ParsedPgn, San, Tags, PgnStr, PgnNodeData, Comment as ChessComment }
import chess.format.{ Fen, Uci, UciCharPair, UciPath }
import chess.MoveOrDrop.*

import lila.importer.{ ImportData, Preprocessed }
import lila.tree.Node.{ Comment, Comments, Shapes }

import scala.language.implicitConversions

import lila.tree.{ Branch, Branches, Root, Metas, NewTree, NewBranch, NewRoot, Node }
import lila.db.BSON
import BSONHandlers.given
import lila.db.BSON.Writer
import lila.db.BSON.Reader
import lila.db.dsl.Bdoc

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
