package lila.study

import chess.format.UciCharPair

case class Path(ids: List[UciCharPair]) extends AnyVal {

  def head: Option[UciCharPair] = ids.headOption

  def tail: Path = Path(ids drop 1)

  def split: Option[(UciCharPair, Path)] = head.map(_ -> tail)

  def isEmpty = ids.isEmpty

  override def toString = ids.mkString
}

object Path {

  def apply(str: String): Path = Path {
    str.toList.grouped(2).toList.flatMap {
      case List(a, b) => UciCharPair(a, b).some
      case _          => none[UciCharPair]
    }
  }

  val root = Path("")
}
