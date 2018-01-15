package lila.study

import chess.opening._
import lila.analyse.{ Analysis, Info, Advice, InfoAdvices }
import lila.tree
import lila.tree.Eval
import lila.tree.Node.Comment

object TreeBuilder {

  private val initialStandardDests = chess.Game(chess.variant.Standard).situation.destinations

  def apply(root: Node.Root, variant: chess.variant.Variant, analysis: Option[Analysis]) = {
    val dests =
      if (variant.standard && root.fen.value == chess.format.Forsyth.initial) initialStandardDests
      else {
        val sit = chess.Game(variant.some, root.fen.value.some).situation
        sit.playable(false) ?? sit.destinations
      }
    makeRoot(root, analysis).copy(dests = dests.some)
  }

  def toBranch(node: Node, infos: Option[InfoAdvices]): tree.Branch = tree.Branch(
    id = node.id,
    ply = node.ply,
    move = node.move,
    fen = node.fen.value,
    check = node.check,
    shapes = node.shapes,
    comments = infos.flatMap(_.headOption.map(_._2)).fold(node.comments) { advice =>
      node.comments ++ tree.Node.Comments {
        advice.map(_.makeComment(false, true)).toList.map { text =>
          Comment(Comment.Id.make, Comment.Text(text), Comment.Author.Lichess)
        }
      }
    },
    gamebook = node.gamebook,
    glyphs = node.glyphs,
    clock = node.clock,
    crazyData = node.crazyData,
    eval = infos.flatMap(_.headOption.map(_._1)) map makeEval,
    children = toBranches(node.children, infos.map(_ drop 1)),
    opening = FullOpeningDB findByFen node.fen.value
  )

  def toBranches(children: Node.Children, infos: Option[InfoAdvices]): List[tree.Branch] = children.nodes match {
    case Vector() => Nil
    case mainline +: rest => toBranch(mainline, infos) :: rest.map { toBranch(_, none) }.toList
  }

  def makeRoot(root: Node.Root, analysis: Option[Analysis]) =
    tree.Root(
      ply = root.ply,
      fen = root.fen.value,
      check = root.check,
      shapes = root.shapes,
      comments = root.comments,
      gamebook = root.gamebook,
      glyphs = root.glyphs,
      clock = root.clock,
      crazyData = root.crazyData,
      children = toBranches(root.children, analysis.map(_.infoAdvices)),
      opening = FullOpeningDB findByFen root.fen.value
    )

  private def makeEval(info: Info) = Eval(
    cp = info.cp,
    mate = info.mate,
    best = info.best
  )
}
