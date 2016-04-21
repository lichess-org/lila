package lila.study

import chess.format.{ Uci, FEN }
import chess.Pos
import chess.variant.Variant
import org.joda.time.DateTime
import scalaz.NonEmptyList

import lila.user.User

case class Study(
    _id: Study.ID,
    chapters: ChapterMap,
    members: StudyMembers,
    ownerId: User.ID,
    createdAt: DateTime) {

  import Study._

  def id = _id

  def orderedChapters: List[(Chapter.ID, Chapter)] =
    chapters.toList.sortBy(_._2.order)

  def firstChapterId = orderedChapters.headOption.map(_._1)
  def firstChapter = firstChapterId flatMap chapters.get

  def location(chapterId: Chapter.ID): Option[Location] =
    chapters get chapterId map { Location(this, chapterId, _) }

  def nextChapterOrder: Int = orderedChapters.lastOption.fold(0)(_._2.order) + 1

  def owner = members get ownerId

  def isOwner(id: User.ID) = ownerId == id

  def canWrite(id: User.ID) = isOwner(id) || members.get(id).exists(_.canWrite)
}

object Study {

  type ID = String

  val idSize = 8

  def make(
    user: lila.common.LightUser,
    setup: Chapter.Setup) = {
    val chapterId = Chapter.makeId
    val chapter = Chapter.make(setup, Node.Root.default, 1)
    val owner = StudyMember(
      user,
      Position.Ref(chapterId, Path.root),
      StudyMember.Role.Write,
      DateTime.now)
    Study(
      _id = scala.util.Random.alphanumeric take idSize mkString,
      chapters = Map(chapterId -> chapter),
      members = StudyMembers(Map(user.id -> owner)),
      ownerId = user.id,
      createdAt = DateTime.now)
  }
}
