package lila.study

import chess.format.{ Uci, FEN }
import chess.Pos
import chess.variant.Variant
import org.joda.time.DateTime

import lila.user.User

case class Study(
    _id: Study.ID,
    owner: User.ID,
    chapters: Map[Chapter.ID, Chapter],
    createdAt: DateTime) {

  import Study._

  def id = _id

  def location(chapterId: Chapter.ID): Option[Location] =
    chapters get chapterId map { Location(this, chapterId, _) }
}

object Study {

  type ID = String

  val idSize = 8

  def make(
    owner: User.ID,
    setup: Chapter.Setup) = Study(
    _id = scala.util.Random.alphanumeric take idSize mkString,
    owner = owner,
    chapters = Map(Chapter.makeId -> Chapter.make(setup, Node.Root.default)),
    createdAt = DateTime.now)
}
