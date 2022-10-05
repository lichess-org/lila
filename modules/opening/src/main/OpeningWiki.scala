package lila.opening

import chess.opening.{ FullOpening, FullOpeningDB }
import com.softwaremill.tagging._
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import reactivemongo.api.bson.Macros
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.Markdown
import lila.db.dsl._
import lila.memo.CacheApi
import lila.user.User

case class OpeningWiki(
    markup: Option[String],
    firstParagraph: Option[String],
    revisions: List[OpeningWiki.Revision]
)

final class OpeningWikiApi(coll: Coll @@ WikiColl, explorer: OpeningExplorer, cacheApi: CacheApi)(implicit
    ec: ExecutionContext
) {

  import OpeningWiki.Revision

  implicit val revisionHandler = Macros.handler[Revision]
  implicit val wikiHandler     = Macros.handler[OpeningWiki]

  def apply(op: FullOpening, withRevisions: Boolean): Fu[OpeningWiki] = for {
    wiki <- cache.get(op.key)
    revisions <- withRevisions ?? {
      coll.primitiveOne[List[Revision]]($id(op.key), "revisions")
    }
  } yield wiki.copy(revisions = (~revisions) take 25)

  def firstParagraph(op: FullOpening): Fu[Option[String]] =
    apply(op, false).dmap(_.firstParagraph)

  def write(op: FullOpening, text: String, by: User): Funit =
    coll.update
      .one(
        $id(op.key),
        $doc(
          "$push" -> $doc(
            "revisions" -> $doc(
              "$each"     -> List(Revision(Markdown(text), by.id, DateTime.now)),
              "$position" -> 0,
              "$slice"    -> 30
            )
          )
        ),
        upsert = true
      )
      .void >>- cache.put(op.key, compute(op.key))

  def popularOpeningsWithShortWiki: Fu[List[FullOpening]] =
    coll
      .aggregateList(100, ReadPreference.secondaryPreferred) { framework =>
        import framework._
        Project($doc("popularity" -> true, "rev" -> $doc("$first" -> "$revisions"))) -> List(
          AddFields($doc("len" -> $doc("$strLenBytes" -> $doc("$ifNull" -> $arr("$rev.text", ""))))),
          Match($doc("len" $lt 300)),
          Sort(Descending("popularity")),
          Project($doc("_id" -> true))
        )
      }
      .map { docs =>
        for {
          doc <- docs
          id  <- doc string "_id"
          op  <- FullOpeningDB.shortestLines get id
        } yield op
      }

  private val renderer =
    new lila.common.MarkdownRender(
      autoLink = false,
      list = true,
      header = true,
      table = false,
      strikeThrough = false
    )

  private val cache = cacheApi[FullOpening.Key, OpeningWiki](1024, "opening.wiki") {
    _.maximumSize(4096).buildAsyncFuture(compute)
  }

  private def compute(key: FullOpening.Key): Fu[OpeningWiki] = for {
    docOpt <- coll
      .aggregateOne() { framework =>
        import framework._
        Match($id(key)) ->
          List(Project($doc("lastRev" -> $doc("$first" -> "$revisions"))))
      }
    _ <- updatePopularity(key)
    lastRev  = docOpt.flatMap(_.getAsOpt[Revision]("lastRev"))
    markdown = lastRev.map(_.text)
  } yield OpeningWiki(
    markdown map renderer(s"opening:${key}"),
    markdown.flatMap(_.value.split("\n").headOption).map(Markdown) map renderer(s"opening:${key}"),
    Nil
  )

  private def updatePopularity(key: FullOpening.Key): Funit =
    FullOpeningDB.shortestLines.get(key) ?? { op =>
      explorer.simplePopularity(op) flatMap {
        _ ?? { popularity =>
          coll.update
            .one(
              $id(key),
              $set(
                "popularity"   -> popularity,
                "popularityAt" -> DateTime.now
              ),
              upsert = true
            )
            .void
        }
      }
    }
}

object OpeningWiki {

  case class Revision(text: Markdown, by: User.ID, at: DateTime)

  val form = Form(single("text" -> nonEmptyText(minLength = 10, maxLength = 10_000)))
}
