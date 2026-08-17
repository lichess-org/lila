package lila.ublog

import reactivemongo.api.bson.Macros.Annotations.Key

import scalalib.ThreadLocalRandom.shuffle
import scalalib.model.Language
import lila.core.id.ImageId
import lila.core.i18n.{ defaultLanguage, toLanguage }
import lila.core.ublog.Quality

case class UblogPost(
    @Key("_id") id: UblogPostId,
    blog: UblogBlog.Id,
    title: String,
    intro: String,
    markdown: Markdown,
    language: Language,
    image: Option[UblogImage],
    topics: List[UblogTopic],
    live: Boolean,
    discuss: Option[Boolean],
    sticky: Option[Boolean],
    ads: Option[Boolean], // boo!
    created: UblogPost.Recorded,
    updated: Option[UblogPost.Recorded],
    lived: Option[UblogPost.Recorded],
    featured: Option[UblogPost.Featured],
    likes: UblogPost.Likes,
    views: UblogPost.Views,
    quality: Quality = Quality.spam, // effective quality used for filtering and ordering
    listedAt: Option[Instant] = none, // this is the sort key because lived.at is unfair to untrusteds
    similar: Option[List[UblogSimilar]] = none,
    automod: Option[UblogAutomod.Assessment] = none,
    approval: UblogPost.Approval = UblogPost.Approval.unverified
) extends UblogPost.BasePost
    with lila.core.ublog.UblogPost:

  def isBy[U: UserIdOf](u: U) = created.by.is(u)
  def isUserBlog[U: UserIdOf](u: U) = blog == UblogBlog.Id.User(u.id)

  def indexable = live && (
    topics.exists(UblogTopic.chessExists) || featured.isDefined || isBy(UserId.lichess)
  )
  def visibleByCrawlers = indexable && quality != Quality.spam
  def allText = s"$title $intro $markdown"
  def isEmpty = title.isEmpty && intro.isEmpty && markdown.value.isEmpty && image.isEmpty

  def allows = UblogBlog.Allows(created.by)
  def canView(using Option[Me]) = live || allows.draft

  def moderate(d: UblogForm.ModPostData): UblogPost =
    def hasTags = d.evergreen.isDefined
    val base = automod.getOrElse(UblogAutomod.Assessment(quality = Quality.spam))
    val assessment = base.copy(
      evergreen = d.evergreen.orElse(base.evergreen),
      flagged = if hasTags then d.flagged else base.flagged,
      commercial = if hasTags then d.commercial else base.commercial
    )
    copy(
      automod = assessment.some,
      quality = d.quality | quality,
      approval = d.quality.fold(approval)(_ => UblogPost.Approval.verified)
    )

  def isPendingQuality =
    approval == UblogPost.Approval.unverified &&
      automod.exists(_.quality == Quality.good) &&
      quality == Quality.weak

  private[ublog] def computeEffectiveQuality(trustedAuthor: Boolean): UblogPost =
    if approval == UblogPost.Approval.verified then this
    else
      automod
        .map(_.quality)
        .match
          case None =>
            copy(quality = if trustedAuthor then Quality.good else Quality.spam) // shouldn't happen
          case Some(Quality.good) if trustedAuthor =>
            copy(quality = Quality.good, approval = UblogPost.Approval.trusted)
          case Some(Quality.good) =>
            copy(quality = Quality.weak, approval = UblogPost.Approval.unverified)
          case Some(notGood) => copy(quality = notGood, approval = UblogPost.Approval.unverified)

  private[ublog] def refreshListedAt(previous: Quality): UblogPost =
    if listedAt.isEmpty && quality.ordinal > previous.ordinal then copy(listedAt = nowInstant.some) else this

case class UblogImage(id: ImageId, alt: Option[String] = None, credit: Option[String] = None)

case class UblogSimilar(id: UblogPostId, count: Int)

object UblogPost:

  export lila.core.ublog.UblogPost.*

  def empty(user: User) =
    UblogPost(
      id = randomId,
      blog = UblogBlog.Id.User(user.id),
      title = "",
      intro = "",
      markdown = Markdown(""),
      language = user.realLang.map(toLanguage) | defaultLanguage,
      image = none,
      topics = Nil,
      live = false,
      discuss = false.some,
      sticky = false.some,
      ads = false.some,
      created = Recorded(user.id, nowInstant),
      updated = none,
      lived = none,
      featured = none,
      likes = Likes(1),
      views = Views(0)
    )

  enum Approval:
    case unverified, trusted, verified
    def key = toString
  object Approval:
    def apply(key: String) = values.find(_.key == key)

  def slug(title: String) =
    val s = scalalib.StringOps.slug(title)
    if s.isEmpty then "-" else s

  opaque type Likes = Int
  object Likes extends RelaxedOpaqueInt[Likes]
  opaque type Views = Int
  object Views extends RelaxedOpaqueInt[Views]

  trait BasePost extends lila.core.ublog.UblogPost:
    val blog: UblogBlog.Id
    val title: String
    val intro: String
    val image: Option[UblogImage]
    val created: Recorded
    val updated: Option[Recorded]
    val lived: Option[Recorded]
    val listedAt: Option[Instant]
    val featured: Option[Featured]
    val sticky: Option[Boolean]
    def slug = UblogPost.slug(title)
    def isLichess = created.by.is(UserId.lichess)

  case class PreviewPost(
      @Key("_id") id: UblogPostId,
      blog: UblogBlog.Id,
      title: String,
      intro: String,
      image: Option[UblogImage],
      created: Recorded,
      updated: Option[Recorded],
      lived: Option[Recorded],
      listedAt: Option[Instant],
      featured: Option[Featured],
      sticky: Option[Boolean],
      topics: List[UblogTopic]
  ) extends BasePost

  case class Featured(by: UserId, at: Option[Instant], until: Option[Instant] = none)

  case class CarouselPosts(
      pinned: List[PreviewPost],
      queue: List[PreviewPost]
  ):
    def shuffled: List[PreviewPost] =
      (pinned ++ shuffle(queue)).toList

    def has(id: UblogPostId): Boolean =
      pinned.exists(_.id == id) || queue.exists(_.id == id)

  case class BlogPreview(nbPosts: Int, latests: List[PreviewPost])

  def randomId = UblogPostId(scalalib.ThreadLocalRandom.nextString(8))

  object thumbnail:
    enum Size(val width: Int):
      def height = width * 10 / 16
      def dimensions = lila.memo.Dimensions(width, height)
      case Large extends Size(880)
      case Small extends Size(400)
    type SizeSelector = thumbnail.type => Size

    def apply(picfitUrl: lila.memo.PicfitUrl, image: ImageId, size: SizeSelector): Url =
      picfitUrl.thumbnail(image)(size(thumbnail).dimensions)
