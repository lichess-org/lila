package lila.study

import chess.format.UciCharPair

opaque type Path = String
object Path extends OpaqueString[Path]:
  def fromId(id: UciCharPair): Path             = id.toString
  def fromIds(ids: Iterable[UciCharPair]): Path = ids.mkString

  extension (e: Path)

    def computeIds: Iterator[UciCharPair] = e.grouped(2).flatMap { strToId(_) }

    def head: Option[UciCharPair] = strToId(e)

    def parent: Path = e dropRight 2

    def split: Option[(UciCharPair, Path)] = head.map(_ -> e.drop(2))

    def isEmpty = e.isEmpty

    def lastId: Option[UciCharPair] = strToId(e.takeRight(2))

    def +(id: UciCharPair): Path = e + id.toString
    def +(node: Node): Path      = e + node.id.toString
    def +(more: Path): Path      = e + more

    def prepend(id: UciCharPair): Path = id.toString + e

    def intersect(other: Path): Path =
      val p = e.zip(other).takeWhile(_ == _).map(_._1)
      // `/ 2 * 2` makes sure the size is even. It's necessary!
      p.take(p.size / 2 * 2).mkString

    def toDbField =
      if e.isEmpty then s"root.${Path.rootDbKey}" else s"root.${encodeDbKey(e)}"

    def depth = e.size / 2

  private[study] def fromDbKey(key: String): Path = decodeDbKey(key)

  private inline def strToId(inline str: String): Option[UciCharPair] = for
    a <- str.headOption
    b <- str.lift(1)
  yield UciCharPair(a, b)

  val root: Path = ""

  // mongodb objects don't support empty keys
  val rootDbKey = "_"

  // mongodb objects don't support '.' and '$' in keys
  private def encodeDbKey(pair: UciCharPair): String = encodeDbKey(pair.toString)
  private[study] def encodeDbKey(path: Path): String =
    path.value.replace('.', 144.toChar).replace('$', 145.toChar)
  private[study] inline def decodeDbKey(inline key: String): String =
    key.replace(144.toChar, '.').replace(145.toChar, '$')

  def isMainline(node: RootOrNode, path: Path): Boolean =
    split(path).fold(true) { (id, rest) =>
      node.children.first ?? { child =>
        child.id == id && isMainline(child, rest)
      }
    }
