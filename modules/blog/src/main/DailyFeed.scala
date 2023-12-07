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

  private val renderer = lila.common.MarkdownRender(
    autoLink = true,
    list = true,
    table = true,
    strikeThrough = true,
    header = true
  )

final class DailyFeed(coll: Coll, cacheApi: CacheApi)(using Executor):

  import DailyFeed.Update

  private given BSONHandler[LocalDate] = quickHandler[LocalDate](
    { case BSONString(s) => LocalDate.parse(s) },
    d => BSONString(d.toString)
  )
  private given BSONDocumentHandler[Update] = Macros.handler

  private val cache = cacheApi[Max, List[Update]](512, "dailyFeed.updates"):
    _.expireAfterWrite(3 seconds).buildAsyncFuture: nb =>
      coll.find($empty).sort($sort.desc("_id")).cursor[Update]().list(nb.value)
  private def clearCache() = cache.underlying.synchronous.invalidateAll()

  export cache.{ get as recent }

  def get(day: LocalDate): Fu[Option[Update]] = coll.one[Update]($id(day))

  def set(update: Update, from: Option[Update]): Funit = for
    _ <- from.filter(_.day != update.day).so(up => coll.delete.one($id(up.day)).void)
    _ <- coll.update.one($id(update.day), update, upsert = true).void andDo clearCache()
  yield ()

  def delete(id: LocalDate): Funit =
    coll.delete.one($id(id)).void andDo clearCache()

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
