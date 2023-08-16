package lila.study

import chess.{ Move, Ply, Game as ChessGame, Situation }
import chess.{ WithMove, FromMove, Generator, GameTree }
import chess.ChessTreeArbitraries
import chess.NodeArbitraries
import chess.ChessTreeArbitraries.{ given, * }
import chess.format.{ Fen, Uci, UciCharPair, UciPath }
import chess.format.pgn.{ Pgn, Move as PgnMove, Tags, InitialComments, Glyphs }
import org.scalacheck.{ Arbitrary, Gen }
import chess.bitboard.Bitboard
import lila.tree.{ NewRoot, NewBranch, Metas }
import lila.tree.Node.{ Comments, Shapes, Comment }

object StudyArbitraries:

  def genRoot(seed: Situation): Gen[NewRoot] =
    val ply = Ply.initial
    for
      tree <- genNode(seed)
      pgnTree = tree.map(_.map(_.data))
      metas <- genMetas(seed, ply)
      pgn = NewRoot(metas, pgnTree)
    yield pgn

  def genRootWithPath(seed: Situation): Gen[(NewRoot, UciPath)] =
    val ply = Ply.initial
    for
      tree <- genNodeWithPath(seed)
      pgnTree = tree._1.map(_.map(_.data))
      metas <- genMetas(seed, ply)
      pgn = NewRoot(metas, pgnTree)
      path = tree._2.map(_.id)
    yield (pgn, UciPath.fromIds(path))

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
            path = UciPath.root,
            move = Uci.WithSan(uci, move.san),
            comp = false,
            forceVariation = false,
            metas = metas
          )
        )

  def genMetas(situation: Situation, ply: Ply): Gen[Metas] =
    for
      comments <- genComments(5)
      glyphs   <- Gen.someOf(Glyphs.all).map(xs => Glyphs.fromList(xs.toList))
      clock    <- Gen.posNum[Int]
    yield Metas(
      ply.next,
      Fen.write(ChessGame(situation, ply = ply.next)),
      situation.check,
      None,
      None,
      None,
      Shapes.empty,
      comments,
      None,
      glyphs,
      None,
      chess.Centis(clock).some,
      situation.board.crazyData
    )

  def genComments(size: Int) =
    for
      commentSize <- Gen.choose(0, size)
      xs          <- Gen.listOfN(commentSize, Gen.alphaStr)
      texts    = xs.collect { case s if s.nonEmpty => Comment.Text(s) }
      comments = texts.map(Comment(Comment.Id.make, _, Comment.Author.Lichess))
    yield Comments(comments)
