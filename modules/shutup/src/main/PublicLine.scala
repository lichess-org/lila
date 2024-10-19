package lila.shutup

import lila.core.shutup.PublicSource as Source

case class PublicLine(text: String, from: Source, date: Instant)

object PublicLine:

  def make(text: String, from: Source): PublicLine =
    PublicLine(text.take(200), from, nowInstant)

  import reactivemongo.api.bson.*
  import PublicSource.given
  given BSONHandler[PublicLine] = Macros.handler

  def merge(prev: PublicLine, next: PublicLine): Option[PublicLine] =
    if prev.from != next.from then none
    else if prev.text.split(" \\| ").toList.contains(next.text) then prev.some
    else prev.copy(text = s"${prev.text} | ${next.text}").some
