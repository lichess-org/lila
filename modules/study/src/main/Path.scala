package lila.study

import chess.format.UciCharPair

opaque type Path = String
object Path:
  def apply(p: String): Path                  = p
  def apply(ids: Iterable[UciCharPair]): Path = ids.mkString

  extension (e: Path)

    // def allIds: Vector[UciCharPair] = e
    //   .grouped(2)
    //   .flatMap { p =>
    //     p lift 1 map { b =>
    //       UciCharPair(p(0), b)
    //     }
    //   }
    //   .toVector

    def value: String = e

    def head: Option[UciCharPair] = e.sizeIs > 1 option UciCharPair(e(0), e(1))

    def parent: Path = Path(e dropRight 2)

    def split: Option[(UciCharPair, Path)] = head.map(_ -> e.drop(2))

    def isEmpty = e.isEmpty

    def +(id: UciCharPair): Path = e + id.toString
    def +(node: Node): Path      = e + node.id.toString
    def +(more: Path): Path      = e + more

    def prepend(id: UciCharPair): Path = id.toString + e

    def intersect(other: Path): Path =
      val p = e.zip(other).takeWhile(_ == _).map(_._1)
      Path(p.take(p.size / 2).mkString)

    def toDbField =
      if e.isEmpty then s"root.${Path.rootDbKey}" else s"root.${encodeDbKey(e)}"

    def depth = e.size / 2

  private[study] def fromDbKey(key: String): Path = apply(decodeDbKey(key))

  val root = Path("")

  // mongodb objects don't support empty keys
  val rootDbKey = "_"

  // mongodb objects don't support '.' and '$' in keys
  private def encodeDbKey(pair: UciCharPair): String = encodeDbKey(pair.toString)
  private def encodeDbKey(path: Path): String = path.value.replace('.', 144.toChar).replace('$', 145.toChar)
  private inline def decodeDbKey(inline key: String): String =
    key.replace(144.toChar, '.').replace(145.toChar, '$')

  def isMainline(node: RootOrNode, path: Path): Boolean =
    split(path).fold(true) { (id, rest) =>
      node.children.first ?? { child =>
        child.id == id && isMainline(child, rest)
      }
    }
