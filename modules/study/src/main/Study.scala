package lila.study

import ornicar.scalalib.ThreadLocalRandom
import chess.format.UciPath

import lila.user.User

case class Study(
    _id: StudyId,
    name: StudyName,
    members: StudyMembers,
    position: Position.Ref,
    ownerId: UserId,
    visibility: Study.Visibility,
    settings: Settings,
    from: Study.From,
    likes: Study.Likes,
    description: Option[String] = None,
    topics: Option[StudyTopics] = None,
    createdAt: Instant,
    updatedAt: Instant
):

  import Study.*

  inline def id = _id

  def owner = members get ownerId

  def isOwner[U: UserIdOf](u: U) = ownerId is u

  def isMember[U: UserIdOf](u: U) = members contains u.id

  def canChat(id: UserId) = Settings.UserSelection.allows(settings.chat, this, id.some)

  def canContribute[U: UserIdOf](u: U) =
    isOwner(u) || members.get(u.id).exists(_.canContribute) || u.is(User.lichessId)

  def canView(id: Option[UserId]) = !isPrivate || id.exists(members.contains)

  def isCurrent(c: Chapter.Like) = c.id == position.chapterId

  def withChapter(c: Chapter.Like): Study = if isCurrent(c) then this else rewindTo(c)

  def rewindTo(c: Chapter.Like): Study =
    copy(position = Position.Ref(chapterId = c.id, path = UciPath.root))

  def isPublic   = visibility == Study.Visibility.Public
  def isUnlisted = visibility == Study.Visibility.Unlisted
  def isPrivate  = visibility == Study.Visibility.Private

  def isNew = (nowSeconds - createdAt.toSeconds) < 4

  def isOld = (nowSeconds - updatedAt.toSeconds) > 20 * 60

  def cloneFor(user: User): Study =
    val owner = StudyMember(id = user.id, role = StudyMember.Role.Write)
    copy(
      _id = Study.makeId,
      members = StudyMembers(Map(user.id -> owner)),
      ownerId = owner.id,
      visibility = Study.Visibility.Private,
      from = Study.From.Study(id),
      likes = Likes(1),
      createdAt = nowInstant,
      updatedAt = nowInstant
    )

  def nbMembers = members.members.size

  def withoutMembers = copy(members = StudyMembers.empty)

  def light = LightStudy(isPublic, members.contributorIds)

  def topicsOrEmpty = topics | StudyTopics.empty

  def addTopics(ts: StudyTopics) =
    copy(
      topics = topics.fold(ts)(_ ++ ts).some
    )

object Study:

  val maxChapters = 64

  val previewNbMembers  = 4
  val previewNbChapters = 4

  case class IdName(_id: StudyId, name: StudyName):
    inline def id = _id

  def toName(str: String) = StudyName(lila.common.String.fullCleanUp(str) take 100)

  enum Visibility:
    case Private, Unlisted, Public
    val key = Visibility.this.toString.toLowerCase
  object Visibility:
    val byKey = values.mapBy(_.key)

  opaque type Likes = Int
  object Likes extends OpaqueInt[Likes]

  case class Liking(likes: Likes, me: Boolean)
  val emptyLiking = Liking(Likes(0), me = false)

  opaque type Rank = Instant
  object Rank extends OpaqueInstant[Rank]:
    def compute(likes: Likes, createdAt: Instant) =
      Rank(createdAt plusHours likesToHours(likes))
    private def likesToHours(likes: Likes): Int =
      if likes < 1 then 0
      else (5 * math.log(likes) + 1).toInt.min(likes) * 24

  enum From:
    case Scratch
    case Game(id: GameId)
    case Study(id: StudyId)
    case Relay(clonedFrom: Option[StudyId])

  case class Data(
      name: String,
      visibility: String,
      computer: Settings.UserSelection,
      explorer: Settings.UserSelection,
      cloneable: Settings.UserSelection,
      shareable: Settings.UserSelection,
      chat: Settings.UserSelection,
      sticky: String,
      description: String
  ):
    def vis = Visibility.byKey.getOrElse(visibility, Visibility.Public)
    def settings =
      Settings(computer, explorer, cloneable, shareable, chat, sticky == "true", description == "true")

  case class WithChapter(study: Study, chapter: Chapter)

  case class WithChapters(study: Study, chapters: Seq[StudyChapterName])

  case class WithActualChapters(study: Study, chapters: Seq[Chapter])

  case class WithChaptersAndLiked(study: Study, chapters: Seq[StudyChapterName], liked: Boolean)

  case class WithLiked(study: Study, liked: Boolean)

  case class LightStudy(isPublic: Boolean, contributors: Set[UserId])

  def makeId = StudyId(ThreadLocalRandom nextString 8)

  def make(
      user: User,
      from: From,
      id: Option[StudyId] = None,
      name: Option[StudyName] = None,
      settings: Option[Settings] = None
  ) =
    val owner = StudyMember(id = user.id, role = StudyMember.Role.Write)
    Study(
      _id = id | makeId,
      name = name | StudyName(s"${user.username}'s Study"),
      members = StudyMembers(Map(user.id -> owner)),
      position = Position.Ref(StudyChapterId(""), UciPath.root),
      ownerId = user.id,
      visibility = Visibility.Public,
      settings = settings | Settings.init,
      from = from,
      likes = Likes(1),
      createdAt = nowInstant,
      updatedAt = nowInstant
    )
