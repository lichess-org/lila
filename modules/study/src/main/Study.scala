package lila.study

import org.joda.time.DateTime

import lila.user.User

case class Study(
    _id: Study.Id,
    name: Study.Name,
    members: StudyMembers,
    position: Position.Ref,
    ownerId: User.ID,
    visibility: Study.Visibility,
    settings: Settings,
    from: Study.From,
    likes: Study.Likes,
    createdAt: DateTime,
    updatedAt: DateTime
) {

  import Study._

  def id = _id

  def owner = members get ownerId

  def isOwner(id: User.ID) = ownerId == id

  def isMember(id: User.ID) = members contains id

  def canChat(id: User.ID) = Settings.UserSelection.allows(settings.chat, this, id.some)

  def canContribute(id: User.ID) = isOwner(id) || members.get(id).exists(_.canContribute)

  def isCurrent(c: Chapter.Like) = c.id == position.chapterId

  def withChapter(c: Chapter.Like): Study = if (isCurrent(c)) this else rewindTo(c)

  def rewindTo(c: Chapter.Like): Study =
    copy(position = Position.Ref(chapterId = c.id, path = Path.root))

  def isPublic = visibility == Study.Visibility.Public

  def isNew = (nowSeconds - createdAt.getSeconds) < 4

  def isOld = (nowSeconds - updatedAt.getSeconds) > 20 * 60

  def cloneFor(user: User): Study = {
    val owner = StudyMember(
      id = user.id,
      role = StudyMember.Role.Write,
      addedAt = DateTime.now
    )
    copy(
      _id = Study.makeId,
      members = StudyMembers(Map(user.id -> owner)),
      ownerId = owner.id,
      visibility = Study.Visibility.Private,
      from = Study.From.Study(id),
      likes = Likes(1),
      createdAt = DateTime.now,
      updatedAt = DateTime.now
    )
  }

  def nbMembers = members.members.size

  def withoutMembers = copy(members = StudyMembers.empty)

  def light = LightStudy(isPublic, members.contributorIds)
}

object Study {

  case class Id(value: String) extends AnyVal with StringValue
  implicit val idIso = lila.common.Iso.string[Id](Id.apply, _.value)

  case class Name(value: String) extends AnyVal with StringValue
  implicit val nameIso = lila.common.Iso.string[Name](Name.apply, _.value)

  case class IdName(_id: Id, name: Name) {

    def id = _id
  }

  def toName(str: String) = Name(str.trim take 100)

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
    case class Study(id: Id) extends From
  }

  case class Data(
      name: String,
      visibility: String,
      computer: String,
      explorer: String,
      cloneable: String,
      chat: String
  ) {
    import Settings._
    def vis = Visibility.byKey get visibility getOrElse Visibility.Public
    def settings = for {
      comp <- UserSelection.byKey get computer
      expl <- UserSelection.byKey get explorer
      clon <- UserSelection.byKey get cloneable
      chat <- UserSelection.byKey get chat
    } yield Settings(comp, expl, clon, chat)
  }

  case class WithChapter(study: Study, chapter: Chapter)

  case class WithChapters(study: Study, chapters: Seq[Chapter.Name])

  case class WithActualChapters(study: Study, chapters: Seq[Chapter])

  case class WithChaptersAndLiked(study: Study, chapters: Seq[Chapter.Name], liked: Boolean)

  case class LightStudy(isPublic: Boolean, contributors: Set[User.ID])

  val idSize = 8

  def makeId = Id(scala.util.Random.alphanumeric take idSize mkString)

  def make(user: User, from: From) = {
    val owner = StudyMember(
      id = user.id,
      role = StudyMember.Role.Write,
      addedAt = DateTime.now
    )
    Study(
      _id = makeId,
      name = Name(s"${user.username}'s Study"),
      members = StudyMembers(Map(user.id -> owner)),
      position = Position.Ref(Chapter.Id(""), Path.root),
      ownerId = user.id,
      visibility = Visibility.Public,
      settings = Settings.init,
      from = from,
      likes = Likes(1),
      createdAt = DateTime.now,
      updatedAt = DateTime.now
    )
  }
}
