package lila.blog

import reactivemongo.api.bson.*
import reactivemongo.api.bson.Macros.Annotations.Key
import java.time.format.{ DateTimeFormatter, FormatStyle }
import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import lila.common.config.{ Max, BaseUrl }
import play.api.data.Form
import lila.user.Me

object DailyFeed:

  type ID = String

  case class Update(
      @Key("_id") id: ID,
      content: Markdown,
      public: Boolean,
      at: Instant,
      flair: Option[Flair]
  ):
    lazy val rendered: Html = renderer(s"dailyFeed:${id}")(content)
    lazy val dateStr        = dateFormatter print at
    lazy val title          = "Daily update - " + dateStr
    def published           = public && at.isBeforeNow
    def future              = at.isAfterNow

  private val renderer      = lila.common.MarkdownRender(autoLink = false, strikeThrough = true)
  private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

  type GetLastUpdates = () => List[Update]

  import ornicar.scalalib.ThreadLocalRandom
  def makeId = ThreadLocalRandom nextString 6

final class DailyFeed(coll: Coll, cacheApi: CacheApi, baseUrl: BaseUrl)(using Executor):

  import DailyFeed.*

  private val max = Max(50)

  private given BSONDocumentHandler[Update] = Macros.handler

  private val atomRenderer =
    lila.common.MarkdownRender(autoLink = false, strikeThrough = true, baseUrl = baseUrl.some)

  private object cache:
    private var mutableLastUpdates: List[Update] = Nil
    val store = cacheApi.unit[List[Update]]:
      _.refreshAfterWrite(1 minute).buildAsyncFuture: _ =>
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
    def lastUpdate: DailyFeed.GetLastUpdates = () => mutableLastUpdates
    store.get({}) // populate lastUpdate

  export cache.lastUpdate

  def recent: Fu[List[Update]] = cache.store.get({})

  def recentPublished = recent.map(_.filter(_.published))

  def get(id: ID): Fu[Option[Update]] = coll.byId[Update](id)

  def set(update: Update): Funit =
    coll.update.one($id(update.id), update, upsert = true).void andDo cache.clear()

  def delete(id: ID): Funit =
    coll.delete.one($id(id)).void andDo cache.clear()

  case class UpdateData(content: Markdown, public: Boolean, at: Instant, flair: Option[Flair]):
    def toUpdate(id: Option[ID]) = Update(id | makeId, content, public, at, flair)

  def form(from: Option[Update])(using Me): Form[UpdateData] =
    import play.api.data.*
    import play.api.data.Forms.*
    import lila.common.Form.*
    val form = Form:
      mapping(
        "content" -> nonEmptyText(maxLength = 20_000).into[Markdown],
        "public"  -> boolean,
        "at"      -> ISOInstantOrTimestamp.mapping,
        lila.user.FlairApi.formPair(anyFlair = true)
      )(UpdateData.apply)(unapply)
    from.fold(form)(u => form.fill(UpdateData(u.content, u.public, u.at, u.flair)))

final class DailyFeedPaginatorBuilder(
    coll: Coll
)(using Executor):
  import DailyFeed.*

  def recent(includeAll: Boolean, page: Int): Fu[Paginator[Update]] =
    Paginator(
      adapter = Adapter[Update](
        collection = coll,
        selector =
          if includeAll then $empty
          else $doc("public" -> true, "at" $lt nowInstant),
        projection = none,
        sort = $sort.desc("at")
      ),
      page,
      MaxPerPage(25)
    )
  def renderAtom(up: Update): Html = atomRenderer(s"dailyFeed:atom:${up.id}")(up.content)
