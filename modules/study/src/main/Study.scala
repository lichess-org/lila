package lila.study

import chess.format.UciPath
import reactivemongo.api.bson.Macros.Annotations.Key
import scalalib.ThreadLocalRandom

import lila.core.data.OpaqueInstant
import lila.core.study as hub
import lila.core.study.Visibility

case class Study(
    @Key("_id") id: StudyId,
    name: StudyName,
    members: StudyMembers,
    position: Position.Ref,
    ownerId: UserId,
    visibility: Visibility,
    settings: Settings,
    from: Study.From,
    likes: Study.Likes,
    description: Option[String] = None,
    topics: Option[StudyTopics] = None,
    flair: Option[Flair] = None,
    createdAt: Instant,
    updatedAt: Instant
) extends hub.Study:

  import Study.*

  val slug = scalalib.StringOps.slug(name.value)

  def owner = members.get(ownerId)

  def isOwner[U: UserIdOf](u: U) = ownerId.is(u)

  def isMember[U: UserIdOf](u: U) = members.contains(u.id)

  def canChat(id: UserId) = Settings.UserSelection.allows(settings.chat, this, id.some)

  def canContribute[U: UserIdOf](u: U) =
    isOwner(u) || members.get(u.id).exists(_.canContribute) || u.is(UserId.lichess)

  def canView(id: Option[UserId]) = !isPrivate || id.exists(members.contains)

  def isCurrent(c: Chapter.Like) = c.id == position.chapterId

  def withChapter(c: Chapter.Like): Study = if isCurrent(c) then this else rewindTo(c.id)

  def rewindTo(chapterId: StudyChapterId): Study =
    copy(position = Position.Ref(chapterId = chapterId, path = UciPath.root))

  def isPublic = visibility == Visibility.public
  def isUnlisted = visibility == Visibility.unlisted
  def isPrivate = visibility == Visibility.`private`

  def isNew = (nowSeconds - createdAt.toSeconds) < 4

  def isOld = (nowSeconds - updatedAt.toSeconds) > 20 * 60

  def isRelay = from match
    case _: From.Relay => true
    case _ => false

  def notable = likes.value > 10 || {
    likes.value > 5 && createdAt.isBefore(nowInstant.minusDays(7))
  }

  def cloneFor(user: User): Study =
    val owner = StudyMember(id = user.id, role = StudyMember.Role.Write)
    copy(
      id = Study.makeId,
      members = StudyMembers(Map(user.id -> owner)),
      ownerId = owner.id,
      visibility = Visibility.`private`,
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

  val maxChapters = Max(64)

  val previewNbMembers = 4
  val previewNbChapters = 4

  def toName(str: String) = StudyName(lila.common.String.fullCleanUp(str).take(100))

  opaque type Likes = Int
  object Likes extends OpaqueInt[Likes]

  case class Liking(likes: Likes, me: Boolean)
  val emptyLiking = Liking(Likes(0), me = false)

  opaque type Rank = Instant
  object Rank extends OpaqueInstant[Rank]:
    def compute(likes: Likes, createdAt: Instant) =
      Rank(createdAt.plusHours(likesToHours(likes)))
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
      flair: Option[String],
      visibility: Visibility,
      computer: Settings.UserSelection,
      explorer: Settings.UserSelection,
      cloneable: Settings.UserSelection,
      shareable: Settings.UserSelection,
      chat: Settings.UserSelection,
      sticky: String,
      description: String
  ):
    def settings =
      Settings(computer, explorer, cloneable, shareable, chat, sticky == "true", description == "true")

  case class WithChapter(study: Study, chapter: Chapter)

  case class WithChapters(study: Study, chapters: Seq[StudyChapterName])

  case class WithActualChapters(study: Study, chapters: Seq[Chapter])

  case class WithChaptersAndLiked(study: Study, chapters: Seq[StudyChapterName], liked: Boolean)

  case class WithLiked(study: Study, liked: Boolean)

  case class LightStudy(isPublic: Boolean, contributors: Set[UserId])

  def makeId = StudyId(ThreadLocalRandom.nextString(8))

  def make(
      user: User,
      from: From,
      id: Option[StudyId] = None,
      name: Option[StudyName] = None,
      settings: Option[Settings] = None
  ) =
    val owner = StudyMember(id = user.id, role = StudyMember.Role.Write)
    Study(
      id = id | makeId,
      name = name | StudyName(s"${user.username}'s Study"),
      members = StudyMembers(Map(user.id -> owner)),
      position = Position.Ref(StudyChapterId(""), UciPath.root),
      ownerId = user.id,
      visibility = Visibility.public,
      settings = settings | Settings.init,
      from = from,
      likes = Likes(1),
      createdAt = nowInstant,
      updatedAt = nowInstant
    )
