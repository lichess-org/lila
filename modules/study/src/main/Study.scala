package lila.study

import chess.format.{ Uci, FEN }
import chess.Pos
import chess.variant.Variant
import org.joda.time.DateTime
import scalaz.NonEmptyList

import lila.user.User

case class Study(
    _id: Study.ID,
    chapters: Map[Chapter.ID, Chapter],
    members: Map[User.ID, StudyMember],
    ownerId: User.ID,
    createdAt: DateTime) {

  import Study._

  def id = _id

  def orderedChapters: List[(Chapter.ID, Chapter)] =
    chapters.toList.sortBy(_._2.order)

  def firstChapter = orderedChapters.headOption.map(_._2)

  def location(chapterId: Chapter.ID): Option[Location] =
    chapters get chapterId map { Location(this, chapterId, _) }

  def nextChapterOrder: Int = orderedChapters.lastOption.fold(0)(_._2.order) + 1

  def addMember(id: User.ID, member: StudyMember) =
    copy(members = members + (id -> member))

  def owner = members get ownerId
}

object Study {

  type ID = String

  val idSize = 8

  def make(
    ownerId: User.ID,
    setup: Chapter.Setup) = {
    val chapterId = Chapter.makeId
    val chapter = Chapter.make(setup, Node.Root.default, 1)
    Study(
      _id = scala.util.Random.alphanumeric take idSize mkString,
      chapters = Map(chapterId -> chapter),
      members = Map(ownerId -> StudyMember(true, chapterId, Path.root)),
      ownerId = ownerId,
      createdAt = DateTime.now)
  }
}
