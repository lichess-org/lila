package lila.study

import org.joda.time.DateTime

import lila.user.User

case class Study(
    _id: Study.ID,
    name: String,
    members: StudyMembers,
    position: Position.Ref,
    ownerId: User.ID,
    settings: StudySettings,
    createdAt: DateTime) {

  import Study._

  def id = _id

  def owner = members get ownerId

  def isOwner(id: User.ID) = ownerId == id

  def canContribute(id: User.ID) = isOwner(id) || members.get(id).exists(_.canContribute)

  def withChapter(c: Chapter) =
    if (c.id == position.chapterId) this
    else copy(position = Position.Ref(chapterId = c.id, path = c.root.mainLineLastNodePath))

  def isPublic = settings.visibility == StudySettings.Visibility.Public
}

object Study {

  def toName(str: String) = str.trim take 100

  case class Data(
      name: String,
      visibility: String,
      computer: String,
      explorer: String) {
    import StudySettings._
    def settings = for {
      vis <- Visibility.byKey get visibility
      comp <- UserSelection.byKey get computer
      expl <- UserSelection.byKey get explorer
    } yield StudySettings(vis, comp, expl)
  }

  case class WithChapter(study: Study, chapter: Chapter)

  case class WithChapters(study: Study, chapters: Seq[String])

  type ID = String

  val idSize = 8

  def make(user: User) = {
    val owner = StudyMember(
      id = user.id,
      role = StudyMember.Role.Write,
      addedAt = DateTime.now)
    Study(
      _id = scala.util.Random.alphanumeric take idSize mkString,
      name = s"${user.username}'s Study",
      members = StudyMembers(Map(user.id -> owner)),
      position = Position.Ref("", Path.root),
      ownerId = user.id,
      settings = StudySettings.init,
      createdAt = DateTime.now)
  }
}
