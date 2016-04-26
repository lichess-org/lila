package lila.study

import org.joda.time.DateTime

import lila.user.User

case class Study(
    _id: Study.ID,
    members: StudyMembers,
    position: Position.Ref,
    ownerId: User.ID,
    createdAt: DateTime) {

  import Study._

  def id = _id

  def name = "Unnamed study"

  def owner = members get ownerId

  def isOwner(id: User.ID) = ownerId == id

  def canContribute(id: User.ID) = isOwner(id) || members.get(id).exists(_.canContribute)

  def withChapter(c: Chapter.Like) =
    if (c.id == position.chapterId) this
    else copy(position = Position.Ref(chapterId = c.id, path = Path.root))
}

object Study {

  case class WithChapter(study: Study, chapter: Chapter)

  type ID = String

  val idSize = 8

  def make(user: lila.common.LightUser) = {
    val owner = StudyMember(
      user = user,
      role = StudyMember.Role.Write,
      addedAt = DateTime.now)
    Study(
      _id = scala.util.Random.alphanumeric take idSize mkString,
      members = StudyMembers(Map(user.id -> owner)),
      position = Position.Ref("", Path.root),
      ownerId = user.id,
      createdAt = DateTime.now)
  }
}
