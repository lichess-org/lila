package lila.study

import chess.{ Centis, Move, Square, Ply, Game as ChessGame, Situation }
import chess.{ WithMove, FromMove, Generator, GameTree, ChessTreeArbitraries, NodeArbitraries }
import chess.ChessTreeArbitraries.{ given, * }
import chess.CoreArbitraries.{ given, * }
import chess.format.{ Fen, Uci, UciCharPair, UciPath }
import chess.format.pgn.{ Pgn, Move as PgnMove, Tags, InitialComments, Glyph, Glyphs }
import org.scalacheck.{ Arbitrary, Gen }
import chess.bitboard.Bitboard
import lila.tree.{ NewTree, NewRoot, NewBranch, Metas }
import lila.tree.Node.{ Comments, Comment, Shapes, Shape }
import org.scalacheck.Cogen

object StudyArbitraries:

  given Arbitrary[NewRoot] = Arbitrary(genRoot(Situation(chess.variant.Standard)))
  type RootWithPath = (NewRoot, UciPath)
  given Arbitrary[RootWithPath]    = Arbitrary(genRootWithPath(Situation(chess.variant.Standard)))
  given Arbitrary[Option[NewTree]] = Arbitrary(genTree(Situation(chess.variant.Standard)))

  // TODO remove after new scalachess version
  given Cogen[Centis] = Cogen(_.value.toLong)

  def genRoot(seed: Situation): Gen[NewRoot] =
    for
      tree  <- genTree(seed)
      metas <- genMetas(seed, Ply.initial)
      pgn = NewRoot(metas, tree)
    yield pgn

  def genRootWithPath(seed: Situation): Gen[(NewRoot, UciPath)] =
    for
      tree <- genNodeWithPath(seed)
      pgnTree = tree._1.map(_.map(_.data))
      metas <- genMetas(seed, Ply.initial)
      pgn  = NewRoot(metas, pgnTree)
      path = tree._2.map(_.id)
    yield (pgn, UciPath.fromIds(path))

  def genTree(seed: Situation): Gen[Option[NewTree]] =
    genNode(seed).map(_.map(_.map(_.data)))

  given FromMove[NewBranch] with
    override def ply(a: NewBranch): Ply = a.ply
    extension (move: Move)
      def next(ply: Ply): Gen[WithMove[NewBranch]] =
        for
          metas <- genMetas(move.situationAfter, ply)
          uci = move.toUci
        yield WithMove[NewBranch](
          move,
          NewBranch(
            id = UciCharPair(move.toUci),
            move = Uci.WithSan(uci, move.san),
            comp = false,
            forceVariation = false,
            metas = metas
          )
        )

  def genMetas(situation: Situation, ply: Ply): Gen[Metas] =
    for
      comments <- genComments(5)
      glyphs   <- Arbitrary.arbitrary[Glyphs]
      clock    <- Arbitrary.arbitrary[Option[Centis]]
      shapes   <- Arbitrary.arbitrary[Shapes]
    yield Metas(
      ply,
      Fen.write(ChessGame(situation, ply = ply)),
      situation.check,
      None,
      None,
      None,
      shapes,
      comments,
      None,
      glyphs,
      None,
      clock,
      situation.board.crazyData
    )

  def genComments(size: Int) =
    for
      commentSize <- Gen.choose(0, size)
      xs          <- Gen.listOfN(commentSize, Gen.alphaStr)
      texts    = xs.collect { case s if s.nonEmpty => Comment.Text(s) }
      comments = texts.map(Comment(Comment.Id.make, _, Comment.Author.Lichess))
    yield Comments(comments)

  given Arbitrary[Shape]  = Arbitrary(Gen.oneOf(genCircle, genArrow))
  given Arbitrary[Shapes] = Arbitrary(Gen.listOf[Shape](Arbitrary.arbitrary[Shape]).map(Shapes(_)))

  def genCircle: Gen[Shape.Circle] =
    for
      brush <- Arbitrary.arbitrary[Shape.Brush]
      orig  <- Arbitrary.arbitrary[Square]
    yield Shape.Circle(brush, orig)

  def genArrow: Gen[Shape.Arrow] =
    for
      brush <- Arbitrary.arbitrary[Shape.Brush]
      orig  <- Arbitrary.arbitrary[Square]
      dest  <- Arbitrary.arbitrary[Square]
    yield Shape.Arrow(brush, orig, dest)

  given Arbitrary[Shape.Brush] = Arbitrary(Gen.oneOf('G', 'R', 'Y', 'B').map(toBrush))
  private def toBrush(color: Char): Shape.Brush =
    color match
      case 'G' => "green"
      case 'R' => "red"
      case 'Y' => "yellow"
      case _   => "blue"
