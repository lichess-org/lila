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
    ownerChapterId: Chapter.ID,
    createdAt: DateTime) {

  import Study._

  def id = _id

  def orderedChapters: List[(Chapter.ID, Chapter)] =
    chapters.toList.sortBy(_._2.order)

  def firstChapter = orderedChapters.headOption.map(_._2)

  def location(chapterId: Chapter.ID): Option[Location] =
    chapters get chapterId map { Location(this, chapterId, _) }

  def nextChapterOrder: Int = orderedChapters.lastOption.fold(0)(_._2.order) + 1
}

object Study {

  type ID = String

  val idSize = 8

  def make(
    owner: User.ID,
    setup: Chapter.Setup) = {
    val chapterId = Chapter.makeId
    val chapter = Chapter.make(setup, Node.Root.default, 1)
    Study(
      _id = scala.util.Random.alphanumeric take idSize mkString,
      owner = owner,
      chapters = Map(chapterId -> chapter),
      ownerChapterId = chapterId,
      createdAt = DateTime.now)
  }
}
