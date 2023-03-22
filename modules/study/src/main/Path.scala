package lila.study

import shogi.format.usi.UsiCharPair

case class Path(ids: Vector[UsiCharPair]) extends AnyVal {

  def head: Option[UsiCharPair] = ids.headOption

  // def tail: Path = Path(ids drop 1)

  def parent: Path = Path(ids dropRight 1)

  def take(i: Int) = Path(ids take i)

  def drop(i: Int) = Path(ids drop i)

  def split: Option[(UsiCharPair, Path)] = head.map(_ -> Path(ids.drop(1)))

  def isEmpty = ids.isEmpty

  def isOnPathOf(other: Path): Boolean =
    other.ids.startsWith(this.ids)

  def +(id: UsiCharPair): Path = Path(ids appended id)
  def +(node: Node): Path      = Path(ids appended node.id)
  def +(more: Path): Path      = Path(ids appendedAll more.ids)

  def prepend(id: UsiCharPair) = Path(ids prepended id)

  def intersect(other: Path): Path =
    Path {
      ids zip other.ids takeWhile { case (a, b) =>
        a == b
      } map (_._1)
    }

  def toDbField(root: Node.Root) =
    root.gameMainlinePath.fold {
      if (ids.isEmpty) s"root.${Path.rootDbKey}"
      else s"root.${Path encodeDbKey this}"
    } { gmp =>
      val intersection = this.intersect(gmp)
      if (intersection == this)
        s"root.${Path.gameMainlineExtensionDbKey}.${this.depth + root.ply}"
      else
        s"root.${intersection.depth + root.ply}${Path.gameMainlineSep}${Path encodeDbKey this.drop(intersection.depth)}"
    }

  def depth = ids.size

  override def toString = ids.mkString
}

object Path {

  def apply(str: String): Path =
    Path {
      str
        .grouped(2)
        .flatMap { p =>
          p lift 1 map { b =>
            UsiCharPair(p(0), b)
          }
        }
        .toVector
    }

  def fromDbKey(key: String): Path = apply(decodeDbKey(key))

  val root = Path("")

  // mongodb objects don't support empty keys
  val rootDbKey = 255.toChar.toString

  val gameMainlineDbKey          = 254.toChar.toString
  val gameMainlineExtensionDbKey = 253.toChar.toString
  // $ply$gameMainlineSep$path
  val gameMainlineSep = gameMainlineDbKey

  val dbKeys = List(rootDbKey, gameMainlineDbKey, gameMainlineExtensionDbKey)

  // mongodb objects don't support '.' and '$' in keys
  def encodeDbKey(path: Path): String        = encodeDbKey(path.ids.mkString)
  def encodeDbKey(pair: UsiCharPair): String = encodeDbKey(pair.toString)
  def encodeDbKey(pathStr: String): String   = pathStr.replace('.', 251.toChar).replace('$', 252.toChar)
  def decodeDbKey(key: String): String       = key.replace(251.toChar, '.').replace(252.toChar, '$')

  def isMainline(node: RootOrNode, path: Path): Boolean =
    path.split.fold(true) { case (id, rest) =>
      node.children.first ?? { child =>
        child.id == id && isMainline(child, rest)
      }
    }
}
