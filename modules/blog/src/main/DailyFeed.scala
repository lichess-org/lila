package lila.blog

import java.time.LocalDate
import reactivemongo.api.bson.*
import reactivemongo.api.bson.Macros.Annotations.Key
import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import lila.common.config.Max

object DailyFeed:

  case class Update(@Key("_id") day: LocalDate, content: Markdown, public: Boolean):

    lazy val rendered: Html = renderer(s"dailyFeed:${day}")(content)

    lazy val instant: Instant = day.atStartOfDay.instant

    lazy val dayString: String = day.toString

    lazy val title = "Daily update - " + dayString

    lazy val isFresh = instant isAfter nowInstant.minusDays(1)

  private val renderer = lila.common.MarkdownRender(
    autoLink = false,
    list = true,
    table = true,
    strikeThrough = true,
    header = true
  )

  type GetLastUpdates = () => List[Update]

final class DailyFeed(coll: Coll, cacheApi: CacheApi)(using Executor):

  import DailyFeed.Update

  private val max = Max(50)

  private given BSONHandler[LocalDate] = quickHandler[LocalDate](
    { case BSONString(s) => LocalDate.parse(s) },
    d => BSONString(d.toString)
  )
  private given BSONDocumentHandler[Update] = Macros.handler

  private object cache:
    private var mutableLastUpdates: List[Update] = Nil
    val store = cacheApi.unit[List[Update]]:
      _.expireAfterWrite(1 minute).buildAsyncFuture: _ =>
        coll
          .find($empty)
          .sort($sort.desc("_id"))
          .cursor[Update]()
          .list(max.value)
          .addEffect: ups =>
            mutableLastUpdates = ups.take(3)
    def clear()                              = store.underlying.synchronous.invalidateAll()
    def lastUpdate: DailyFeed.GetLastUpdates = () => mutableLastUpdates
    store.get({}) // populate lastUpdate

  export cache.lastUpdate

  def recent: Fu[List[Update]] = cache.store.get({})

  def get(day: LocalDate): Fu[Option[Update]] = coll.one[Update]($id(day))

  def set(update: Update, from: Option[Update]): Funit = for
    _ <- from.filter(_.day != update.day).so(up => coll.delete.one($id(up.day)).void)
    _ <- coll.update.one($id(update.day), update, upsert = true).void
    _ = cache.clear()
  yield ()

  def delete(id: LocalDate): Funit =
    coll.delete.one($id(id)).void andDo cache.clear()

  def form(from: Option[Update]) =
    import play.api.data.*
    import play.api.data.Forms.*
    import lila.common.Form.*
    val form = Form:
      mapping(
        "day" -> ISODate.mapping
          .verifying(
            "There is already an update for this day",
            day => from.exists(_.day == day) || !existsBlocking(day)
          ),
        "content" -> nonEmptyText(maxLength = 20_000).into[Markdown],
        "public"  -> boolean
      )(Update.apply)(unapply)
    from.fold(form)(form.fill)

  private def existsBlocking(day: LocalDate): Boolean =
    coll.exists($id(day)).await(1.second, "dailyFeed.existsBlocking")
