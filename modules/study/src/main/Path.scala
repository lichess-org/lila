package lila.study

import chess.format.UciCharPair

case class Path(ids: List[UciCharPair]) extends AnyVal {

  def head: Option[UciCharPair] = ids.headOption

  def tail: Path = Path(ids drop 1)

  def init: Path = Path(ids take (ids.length - 1))

  def split: Option[(UciCharPair, Path)] = head.map(_ -> tail)

  def isEmpty = ids.isEmpty

  def +(node: Node): Path = Path(ids :+ node.id)
  def +(more: Path): Path = Path(ids ::: more.ids)

  override def toString = ids.mkString
}

object Path {

  def apply(str: String): Path = Path {
    str.grouped(2).flatMap { p =>
      p lift 1 map { b =>
        UciCharPair(p(0), b)
      }
    }.toList
  }

  val root = Path("")

  def isMainline(node: RootOrNode, path: Path): Boolean = path.split.fold(true) {
    case (id, rest) => node.children.first ?? { child =>
      child.id == id && isMainline(child, rest)
    }
  }
}
