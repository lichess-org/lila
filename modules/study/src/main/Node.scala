package lila.study

object Node:

  val MAX_PLIES = 600

  object BsonFields:
    val ply = "p"
    val uci = "u"
    val san = "s"
    val fen = "f"
    val check = "c"
    val shapes = "h"
    val comments = "co"
    val gamebook = "ga"
    val glyphs = "g"
    val score = "e"
    val static = "st"
    val clock = "l"
    val crazy = "z"
    val forceVariation = "fv"
    val comp = "cp"

  object extensions:

    import lila.tree.Branches
    import chess.format.UciPath

    extension (nodes: Branches)

      def deleteNodeAt(path: UciPath): Option[Branches] =
        path.split.flatMap:
          case (head, p) if p.isEmpty && nodes.hasNode(head) => nodes.map(_.filter(_.id != head)).some
          case (_, p) if p.isEmpty => none
          case (head, tail) => nodes.updateChildren(head, _.deleteNodeAt(tail))

      def deleteNodeAtAndPruneComp(path: UciPath): Option[Branches] =
        path.split.flatMap:
          case (head, p) if p.isEmpty && nodes.hasNode(head) => nodes.map(_.filter(_.id != head)).some
          case (_, p) if p.isEmpty => none
          case (head, tail) =>
            for
              node <- nodes.get(head)
              children <- node.children.deleteNodeAtAndPruneComp(tail)
              updated = node.copy(children = children)
            yield
              if updated.comp && !updated.children.hasNonComp then nodes.map(_.filter(_.id != head))
              else nodes.map(_.map(n => if n.id == head then updated else n))
