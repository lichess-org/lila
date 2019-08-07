package lidraughts.study

import draughts.format.{ Uci, Forsyth, FEN }

private final class ChapterUciFixer(repo: ChapterRepo) {

  def apply(chapter: Chapter): Fu[Chapter] =
    fixChapter(chapter).fold(fuccess(chapter)) {
      newChapter =>
        {
          logger.info(s"Fixed chapter ${newChapter._id} from ${newChapter.studyId}")
          repo update newChapter inject newChapter
        }
    }

  private def fixChapter(c: Chapter): Option[Chapter] = c.root.children.first.fold(none[Chapter]) {
    firstNode =>
      val fixedFirst = fixChildren(firstNode)
      if (!fixedFirst._2) none[Chapter]
      else c.copy(
        root = c.root.withChildren {
          children => Node.Children(fixedFirst._1 +: children.nodes.tail).some
        } get
      ).some
  }

  private def fixChildren(node: Node): (Node, Boolean) = {
    var didFix = false
    node.withChildrenAndFen((children, fen) => children.updateAllWithFen((n, f) => fixUci(n, f) match {
      case Some(fixedNode) =>
        didFix = true
        fixedNode
      case _ => n
    }, fen).some) match {
      case Some(fixedNode) if didFix => (fixedNode, true)
      case _ => (node, false)
    }
  }

  private def fixUci(n: Node, fen: FEN): Option[Node] = {
    val fullUci = n.move.uci.uci
    if (fullUci.length > 6) {
      Forsyth << fen.value match {
        case Some(sit) if sit.ghosts == 0 =>
          if (sit.validMoves.filterValues(_.exists(m => m.toUci.uci == fullUci)).nonEmpty) none
          else {
            val sameMove = sit.validMoves.values.flatMap { mvs => mvs.filter(m => m.toUci.uci.length > 6 && fullUci.startsWith(m.toUci.uci)) }
            if (sameMove.size == 1) n.copy(move = Uci.WithSan(sameMove.head.toUci, n.move.san)).some
            else none
          }
        case _ => none
      }
    } else none
  }

}