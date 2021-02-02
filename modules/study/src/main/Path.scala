package lila.study

import chess.format.UciCharPair

case class Path(ids: Vector[UciCharPair]) extends AnyVal {

  def head: Option[UciCharPair] = ids.headOption

  // def tail: Path = Path(ids drop 1)

  def parent: Path = Path(ids dropRight 1)

  def split: Option[(UciCharPair, Path)] = head.map(_ -> Path(ids.drop(1)))

  def isEmpty = ids.isEmpty

  def +(id: UciCharPair): Path = Path(ids appended id)
  def +(node: Node): Path      = Path(ids appended node.id)
  def +(more: Path): Path      = Path(ids appendedAll more.ids)

  def prepend(id: UciCharPair) = Path(ids prepended id)

  def intersect(other: Path): Path =
    Path {
      ids zip other.ids takeWhile { case (a, b) =>
        a == b
      } map (_._1)
    }

  def toDbField =
    if (ids.isEmpty) s"root.${Path.rootDbKey}"
    else s"root.${ids.mkString}"

  override def toString = ids.mkString
}

object Path {

  def apply(str: String): Path =
    Path {
      str
        .grouped(2)
        .flatMap { p =>
          p lift 1 map { b =>
            UciCharPair(p(0), b)
          }
        }
        .toVector
    }

  val root = Path("")

  // mongodb objects don't support empty keys
  val rootDbKey = "_"

  def isMainline(node: RootOrNode, path: Path): Boolean =
    path.split.fold(true) { case (id, rest) =>
      node.children.first ?? { child =>
        child.id == id && isMainline(child, rest)
      }
    }
}
