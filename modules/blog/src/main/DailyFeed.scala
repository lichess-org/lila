package lila.blog

import java.time.{ LocalDate, Instant, ZoneId }
import reactivemongo.api.bson.*
import reactivemongo.api.bson.Macros.Annotations.Key
import lila.common.config.Max
import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import lila.user.Me

object DailyFeed:

  case class Update(@Key("_id") day: LocalDate, content: Markdown, public: Boolean, rev: Long):

    lazy val rendered: Html = renderer(s"dailyFeed:${day}")(content)

    lazy val instant: Instant = day.atStartOfDay.instant

    lazy val dayString: String = day.toString

    lazy val title = "Update - " + dayString

    lazy val isFresh = instant isAfter nowInstant.minusDays(1)

    lazy val isVisible = public && (instant isBefore nowInstant)

  object Update:

    def apply(day: LocalDate, content: Markdown, public: Boolean): Update =
      new Update(day, content, public, nowInstant.getEpochSecond)

    def formUnapply(update: Update): Option[(LocalDate, Markdown, Boolean)] =
      (update.day, update.content, update.public).some

  private val renderer = lila.common.MarkdownRender(
    autoLink = false,
    list = true,
    table = true,
    strikeThrough = true,
    header = true
  )

  type GetLastUpdate = () => Option[Update]

final class DailyFeed(coll: Coll, cacheApi: CacheApi)(using Executor):

  import DailyFeed.Update

  private val max = Max(50)

  private given BSONHandler[LocalDate] = quickHandler[LocalDate](
    { case BSONString(s) => LocalDate.parse(s) },
    d => BSONString(d.toString)
  )
  private given BSONDocumentHandler[Update] = Macros.handler

  private object cache:
    private var mutableLastUpdate: Option[Update] = None
    val store = cacheApi.unit[List[Update]]:
      _.expireAfterWrite(1 minute).buildAsyncFuture: _ =>
        coll
          .find($empty)
          .sort($sort.desc("_id"))
          .cursor[Update]()
          .list(max.value)
          .addEffect: ups =>
            mutableLastUpdate = ups.filter(_.isVisible).headOption
    def clear()                             = store.underlying.synchronous.invalidateAll()
    def lastUpdate: DailyFeed.GetLastUpdate = () => mutableLastUpdate
    def lastRev: Long                       = mutableLastUpdate.fold(0L)(_.rev)

    store.get({}) // populate lastUpdate

  export cache.{ lastUpdate, lastRev }

  def recent: Fu[List[Update]] = cache.store.get({})

  def get(day: LocalDate): Fu[Option[Update]] = coll.one[Update]($id(day))

  def set(update: Update, from: Option[Update]): Funit = for
    _ <- from.filter(_.day != update.day).so(up => coll.delete.one($id(up.day)).void)
    _ <- coll.update.one($id(update.day), update, upsert = true).void andDo cache.clear()
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
      )((Update.apply))(Update.formUnapply)
    from.fold(form): up =>
      if up.rev != lastRev && up.isVisible then
        cache.clear() // we need lastRev to be correct
        lila.common.Bus.publish(lila.hub.actorApi.feed.UpdateFeedRev(up.rev), "lobbySocket")
      form.fill(up)

  private def existsBlocking(day: LocalDate): Boolean =
    coll.exists($id(day)).await(1.second, "dailyFeed.existsBlocking")
