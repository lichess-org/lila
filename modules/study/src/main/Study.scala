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
    settings: Settings,
    from: Study.From,
    likes: Study.Likes,
    createdAt: DateTime,
    updatedAt: DateTime) {

  import Study._

  def id = _id

  def owner = members get ownerId

  def isOwner(id: User.ID) = ownerId == id

  def isMember(id: User.ID) = members contains id

  def canChat = isMember _

  def canContribute(id: User.ID) = isOwner(id) || members.get(id).exists(_.canContribute)

  def withChapter(c: Chapter.Like): Study =
    if (c.id == position.chapterId) this
    else copy(position = Position.Ref(chapterId = c.id, path = Path.root))

  def isPublic = visibility == Study.Visibility.Public

  def isNew = (nowSeconds - createdAt.getSeconds) < 4

  def isOld = (nowSeconds - updatedAt.getSeconds) > 10 * 60

  def cloneFor(user: User): Study = {
    val owner = StudyMember(
      id = user.id,
      role = StudyMember.Role.Write,
      addedAt = DateTime.now)
    copy(
      _id = Study.makeId,
      members = StudyMembers(Map(user.id -> owner)),
      ownerId = owner.id,
      visibility = Study.Visibility.Private,
      from = Study.From.Study(id),
      likes = Likes(0),
      createdAt = DateTime.now,
      updatedAt = DateTime.now)
  }
}

object Study {

  def toName(str: String) = str.trim take 100

  case class Views(value: Int) extends AnyVal

  sealed trait Visibility {
    lazy val key = toString.toLowerCase
  }
  object Visibility {
    case object Private extends Visibility
    case object Public extends Visibility
    val byKey = List(Private, Public).map { v => v.key -> v }.toMap
  }

  case class Likes(value: Int) extends AnyVal
  case class Liking(likes: Likes, me: Boolean)
  val emptyLiking = Liking(Likes(0), false)

  case class Rank(value: DateTime) extends AnyVal
  object Rank {
    def compute(likes: Likes, createdAt: DateTime) = Rank {
      createdAt plusHours likesToHours(likes)
    }
    private def likesToHours(likes: Likes): Int =
      if (likes.value < 1) 0
      else (5 * math.log(likes.value) + 1).toInt.min(likes.value) * 24
  }

  sealed trait From
  object From {
    case object Scratch extends From
    case class Game(id: String) extends From
    case class Study(id: String) extends From
  }

  case class Data(
      name: String,
      visibility: String,
      computer: String,
      explorer: String) {
    import Settings._
    def vis = Visibility.byKey get visibility getOrElse Visibility.Public
    def settings = for {
      comp <- UserSelection.byKey get computer
      expl <- UserSelection.byKey get explorer
    } yield Settings(comp, expl)
  }

  case class WithChapter(study: Study, chapter: Chapter)

  case class WithChapters(study: Study, chapters: Seq[String])

  case class WithActualChapters(study: Study, chapters: Seq[Chapter])

  case class WithChaptersAndLiked(study: Study, chapters: Seq[String], liked: Boolean)

  type ID = String

  val idSize = 8

  def makeId = scala.util.Random.alphanumeric take idSize mkString

  def make(user: User, from: From) = {
    val owner = StudyMember(
      id = user.id,
      role = StudyMember.Role.Write,
      addedAt = DateTime.now)
    Study(
      _id = makeId,
      name = s"${user.username}'s Study",
      members = StudyMembers(Map(user.id -> owner)),
      position = Position.Ref("", Path.root),
      ownerId = user.id,
      visibility = Visibility.Public,
      settings = Settings.init,
      from = from,
      likes = Likes(1),
      createdAt = DateTime.now,
      updatedAt = DateTime.now)
  }
}
