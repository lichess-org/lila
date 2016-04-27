package lila.study

import org.joda.time.DateTime

import lila.user.User

case class Study(
    _id: Study.ID,
    name: String,
    members: StudyMembers,
    position: Position.Ref,
    ownerId: User.ID,
    visibility: Study.Visibility,
    createdAt: DateTime) {

  import Study._

  def id = _id

  def owner = members get ownerId

  def isOwner(id: User.ID) = ownerId == id

  def canContribute(id: User.ID) = isOwner(id) || members.get(id).exists(_.canContribute)

  def withChapter(c: Chapter.Like) =
    if (c.id == position.chapterId) this
    else copy(position = Position.Ref(chapterId = c.id, path = Path.root))
}

object Study {

  sealed trait Visibility {
    lazy val key = toString.toLowerCase
  }
  object Visibility {
    case object Private extends Visibility
    case object Public extends Visibility
    val byKey = List(Private, Public).map { v => v.key -> v }.toMap
  }

  case class Data(name: String, visibility: String) {
    def realVisibility = Visibility.byKey get visibility
  }

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
      name = s"${user.name}'s Study",
      members = StudyMembers(Map(user.id -> owner)),
      position = Position.Ref("", Path.root),
      ownerId = user.id,
      visibility = Visibility.Public,
      createdAt = DateTime.now)
  }
}
