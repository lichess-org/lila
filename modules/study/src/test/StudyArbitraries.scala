package lila.study

import chess.ChessTreeArbitraries.*
import chess.CoreArbitraries.given
import chess.format.pgn.{ Glyphs, Comment as CommentStr }
import chess.format.{ Fen, Uci, UciPath }
import chess.{ Centis, FromMove, Move, Ply, Position, Square, WithMove }
import org.scalacheck.{ Arbitrary, Gen }

import lila.tree.Node.{ Comment, Comments, Shape, Shapes }
import lila.tree.{ Metas, NewBranch, NewRoot, NewTree, Clock }

object StudyArbitraries:

  given Arbitrary[NewRoot] = Arbitrary(genRoot(chess.variant.Standard.initialPosition))
  type RootWithPath = (NewRoot, UciPath)
  given Arbitrary[RootWithPath] = Arbitrary(genRootWithPath(chess.variant.Standard.initialPosition))
  given Arbitrary[Option[NewTree]] = Arbitrary(genTree(chess.variant.Standard.initialPosition))

  given Arbitrary[Clock] = Arbitrary:
    for
      centis <- Arbitrary.arbitrary[Centis]
      trust <- Arbitrary.arbitrary[Option[Boolean]]
    yield Clock(centis, trust)

  def genRoot(seed: Position): Gen[NewRoot] =
    for
      tree <- genTree(seed)
      metas <- genMetas(seed, Ply.initial)
      pgn = NewRoot(metas, tree)
    yield pgn

  def genRootWithPath(seed: Position): Gen[(NewRoot, UciPath)] =
    for
      tree <- genNodeWithPath(seed)
      pgnTree = tree._1.map(_.map(_.data))
      metas <- genMetas(seed, Ply.initial)
      pgn = NewRoot(metas, pgnTree)
      path = tree._2.map(_.id)
    yield (pgn, UciPath.fromIds(path))

  def genTree(seed: Position): Gen[Option[NewTree]] =
    genNode(seed).map(_.map(_.map(_.data)))

  given FromMove[NewBranch] with
    extension (move: Move)
      def next(branch: Option[NewBranch]): Gen[WithMove[NewBranch]] =
        for
          metas <- genMetas(move.after, branch.fold(Ply.initial)(_.ply))
          uci = move.toUci
        yield WithMove[NewBranch](
          move,
          NewBranch(
            move = Uci.WithSan(uci, move.toSanStr),
            comp = false,
            forceVariation = false,
            metas = metas
          )
        )

  def genMetas(board: Position, ply: Ply): Gen[Metas] =
    for
      comments <- genComments(5)
      glyphs <- Arbitrary.arbitrary[Glyphs]
      clock <- Arbitrary.arbitrary[Option[Clock]]
      shapes <- Arbitrary.arbitrary[Shapes]
    yield Metas(
      ply,
      Fen.write(board, ply.fullMoveNumber),
      None,
      shapes,
      comments,
      None,
      glyphs,
      clock,
      board.crazyData
    )

  def genComments(size: Int) =
    for
      commentSize <- Gen.choose(0, size)
      xs <- Gen.listOfN(commentSize, Gen.alphaStr)
      texts = CommentStr.from(xs).trimNonEmpty
      comments = texts.map(Comment(Comment.Id.make, _, Comment.Author.Lichess))
    yield Comments(comments)

  given Arbitrary[Shape] = Arbitrary(Gen.oneOf(genCircle, genArrow))
  given Arbitrary[Shapes] = Arbitrary(Gen.listOf[Shape](Arbitrary.arbitrary[Shape]).map(Shapes(_)))

  def genCircle: Gen[Shape.Circle] =
    for
      brush <- Arbitrary.arbitrary[Shape.Brush]
      orig <- Arbitrary.arbitrary[Square]
    yield Shape.Circle(brush, orig)

  def genArrow: Gen[Shape.Arrow] =
    for
      brush <- Arbitrary.arbitrary[Shape.Brush]
      orig <- Arbitrary.arbitrary[Square]
      dest <- Arbitrary.arbitrary[Square]
    yield Shape.Arrow(brush, orig, dest)

  given Arbitrary[Shape.Brush] = Arbitrary(Gen.oneOf('G', 'R', 'Y', 'B').map(toBrush))
  private def toBrush(color: Char): Shape.Brush =
    color match
      case 'G' => "green"
      case 'R' => "red"
      case 'Y' => "yellow"
      case _ => "blue"
