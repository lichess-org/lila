package lila.shutup

import lila.core.shutup.PublicSource as Source

case class PublicLine(text: String, from: Source, date: Instant)

object PublicLine:

  def make(text: String, from: Source): PublicLine =
    PublicLine(text.take(200), from, nowInstant)

  import reactivemongo.api.bson.*
  import PublicSource.given
  given BSONHandler[PublicLine] = Macros.handler

  object merge:
    val sep = "_@_"
    def apply(prev: PublicLine, next: PublicLine): Option[PublicLine] =
      if prev.from != next.from then none
      else if split(prev.text).contains(next.text) then prev.some
      else prev.copy(text = s"${prev.text}$sep${next.text}").some
    def split(text: String): List[String] = text.split(sep).toList
