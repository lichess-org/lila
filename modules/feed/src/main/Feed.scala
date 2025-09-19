package lila.feed

import play.api.data.Form
import reactivemongo.api.bson.*
import reactivemongo.api.bson.Macros.Annotations.Key
import java.time.format.{ DateTimeFormatter, FormatStyle }
import scalalib.paginator.Paginator

import lila.core.lilaism.Lilaism.*
export lila.common.extensions.unapply
import lila.db.dsl.{ *, given }
import lila.db.paginator.Adapter
import lila.memo.CacheApi
import lila.memo.CacheApi.buildAsyncTimeout
import lila.core.user.FlairApi

object Feed:

  type ID = String

  case class Update(
      @Key("_id") id: ID,
      content: Markdown,
      public: Boolean,
      at: Instant,
      flair: Option[Flair]
  ):
    lazy val rendered: Html = renderer(s"dailyFeed:${id}")(content)
    lazy val dateStr = dateFormatter.print(at)
    lazy val title = "Daily update - " + dateStr
    def published = public && at.isBeforeNow
    def future = at.isAfterNow

  private val renderer = lila.common.MarkdownRender(autoLink = false, strikeThrough = true)
  private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
  given BSONDocumentHandler[Update] = Macros.handler

  type GetLastUpdates = () => List[Update]

  import scalalib.ThreadLocalRandom
  def makeId = ThreadLocalRandom.nextString(6)

final class FeedApi(coll: Coll, cacheApi: CacheApi, flairApi: FlairApi)(using Executor, Scheduler):

  import Feed.*

  private val max = Max(50)

  private object cache:
    private var mutableLastUpdates: List[Update] = Nil
    val store = cacheApi.unit[List[Update]]:
      _.refreshAfterWrite(1.minute).buildAsyncTimeout(): _ =>
        coll
          .find($empty)
          .sort($sort.desc("at"))
          .cursor[Update]()
          .list(max.value)
          .addEffect: ups =>
            mutableLastUpdates = ups.filter(_.published).take(7)
    def clear() =
      store.underlying.synchronous.invalidateAll()
      store.get({}) // populate lastUpdate
    def lastUpdate: Feed.GetLastUpdates = () => mutableLastUpdates
    store.get({}) // populate lastUpdate

  export cache.lastUpdate

  def recentPublished = cache.store.get({}).map(_.filter(_.published))

  def get(id: ID): Fu[Option[Update]] = coll.byId[Update](id)

  def set(update: Update): Funit =
    for _ <- coll.update.one($id(update.id), update, upsert = true) yield cache.clear()

  def delete(id: ID): Funit =
    for _ <- coll.delete.one($id(id)) yield cache.clear()

  case class UpdateData(content: Markdown, public: Boolean, at: Instant, flair: Option[Flair]):
    def toUpdate(id: Option[ID]) = Update(id | makeId, content, public, at, flair)

  def form(from: Option[Update]): Form[UpdateData] =
    import play.api.data.*
    import play.api.data.Forms.*
    import lila.common.Form.*
    val form = Form:
      mapping(
        "content" -> nonEmptyText(maxLength = 20_000).into[Markdown],
        "public" -> boolean,
        "at" -> ISOInstantOrTimestamp.mapping,
        "flair" -> flairApi.formField(anyFlair = true, asAdmin = true)
      )(UpdateData.apply)(unapply)
    from.fold(form)(u => form.fill(UpdateData(u.content, u.public, u.at, u.flair)))

final class FeedPaginatorBuilder(coll: Coll)(using Executor):
  import Feed.*

  def recent(includeAll: Boolean, page: Int): Fu[Paginator[Update]] =
    Paginator(
      adapter = Adapter[Update](
        collection = coll,
        selector =
          if includeAll then $empty
          else $doc("public" -> true, "at".$lt(nowInstant)),
        projection = none,
        sort = $sort.desc("at")
      ),
      page,
      MaxPerPage(25)
    )
