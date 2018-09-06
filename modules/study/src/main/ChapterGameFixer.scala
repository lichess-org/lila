package lidraughts.study

import draughts.format.{ Uci, Forsyth }

private final class ChapterGameFixer(repo: ChapterRepo) {

  def apply(chapter: Chapter): Fu[Chapter] =
    if (chapter.setup.gameId.isEmpty) fuccess(chapter)
    else mergeCaptures(chapter).fold(fuccess(chapter)) {
      newChapter =>
        {
          logger.info(s"Fixed chapter ${newChapter._id} from ${newChapter.studyId}")
          repo update newChapter inject newChapter
        }
    }

  private def mergeCaptures(c: Chapter): Option[Chapter] = c.root.children.first.fold(none[Chapter]) {
    firstNode =>
      val mergedFirst = mergeChildren(firstNode)
      if (!mergedFirst._2) none[Chapter]
      else c.copy(
        root = c.root.withChildren {
          children => Node.Children(mergedFirst._1 +: children.nodes.tail).some
        } get
      ).some
  }

  private def mergeChildren(node: Node): (Node, Boolean) =
    node.children.first.fold((node, false)) {
      firstChild =>
        mergeOrNot(node, firstChild) match {
          case Some(mergedNode) =>
            (mergeChildren(mergedNode)._1, true)
          case _ =>
            var mergedNext = false
            val newNode = node.withChildren {
              children =>
                val newFirstChild = mergeChildren(firstChild)
                mergedNext = newFirstChild._2
                Node.Children(newFirstChild._1 +: children.nodes.tail).some
            }
            (newNode.getOrElse(node), newNode.isDefined && mergedNext)
        }
    }

  private def mergeOrNot(n: Node, fc: Node): Option[Node] =
    (fc.move.san.contains('x') && n.move.san.contains('x') && fc.move.san.take(fc.move.san.indexOf('x')) == n.move.san.drop(n.move.san.indexOf('x') + 1)).fold(
      {
        val whichN = if (Forsyth.countGhosts(n.fen.value) == 1) n.copy(move = Uci.WithSan(Uci(n.move.uci.shortUci).get, n.move.san)) else n
        val shortFc = fc.copy(move = Uci.WithSan(Uci(fc.move.uci.shortUci).get, fc.move.san))
        whichN.withoutChildren.mergeCapture(shortFc).withChildren {
          children =>
            val newChildren = children.nodes ++: n.children.nodes.tail
            Node.Children(newChildren).some
        }
      },
      none[Node]
    )

}
