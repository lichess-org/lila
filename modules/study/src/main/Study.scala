package lila.study

import chess.format.Uci
import chess.Pos
import org.joda.time.DateTime

import lila.user.User

case class Study(
    _id: Study.ID,
    owner: User.ID,
    chapters: Map[Chapter.ID, Chapter],
    createdAt: DateTime) {

  import Study._

  def id = _id

  def location(chapterId: Chapter.ID) =
    chapters get chapterId map { Location(this, chapterId, _) }
}

object Study {

  type ID = String

  val idSize = 8

  def make(owner: User.ID, gameId: Option[String]) = Study(
    _id = scala.util.Random.alphanumeric take idSize mkString,
    owner = owner,
    chapters = Map(Chapter.makeId -> Chapter.make(gameId, Node.Root.default)),
    createdAt = DateTime.now)
}
