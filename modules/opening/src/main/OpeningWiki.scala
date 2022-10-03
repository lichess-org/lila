package lila.opening

import chess.opening.FullOpening
import com.softwaremill.tagging._
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import reactivemongo.api.bson.Macros
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.Markdown
import lila.db.dsl._
import lila.user.User

case class OpeningWiki(markup: Option[String], revisions: List[OpeningWiki.Revision])

final class OpeningWikiApi(coll: Coll @@ WikiColl)(implicit ec: ExecutionContext) {

  import OpeningWiki._

  def apply(op: FullOpening, withRevisions: Boolean): Fu[OpeningWiki] = for {
    lastRev <- coll
      .aggregateOne() { framework =>
        import framework._
        Match($id(op.key)) ->
          List(Project($doc("lastRev" -> $doc("$first" -> "$revisions"))))
      }
      .map { _.flatMap(_.getAsOpt[Revision]("lastRev")(revisionHandler)) }
    revisions <- withRevisions ?? {
      coll.primitiveOne[List[Revision]]($id(op.key), "revisions")
    }
  } yield OpeningWiki(lastRev.map(_.text) map render(op.key), ~revisions)

  def write(op: FullOpening, text: String, by: User): Funit =
    coll.update
      .one(
        $id(op.key),
        $doc(
          "$push" -> $doc(
            "revisions" -> $doc(
              "$each"     -> List(Revision(Markdown(text), by.id, DateTime.now)),
              "$position" -> 0,
              "$slice"    -> 20
            )
          )
        ),
        upsert = true
      )
      .void
}

object OpeningWiki {

  case class Revision(text: Markdown, by: User.ID, at: DateTime)

  val form = Form(single("text" -> nonEmptyText(minLength = 10, maxLength = 10_000)))

  private val renderer =
    new lila.common.MarkdownRender(
      autoLink = true,
      list = true,
      header = true,
      table = false,
      strikeThrough = false
    )

  private val cache = lila.memo.CacheApi.scaffeineNoScheduler
    .maximumSize(1024)
    .build[Markdown, String]()

  def render(key: String)(markdown: Markdown): String = cache.get(markdown, renderer(s"opening:$key"))

  implicit val revisionHandler = Macros.handler[Revision]
  implicit val wikiHandler     = Macros.handler[OpeningWiki]
}
