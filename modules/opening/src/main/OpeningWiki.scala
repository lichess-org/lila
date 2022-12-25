package lila.opening

import chess.opening.{ Opening, OpeningDb, OpeningKey }
import org.joda.time.DateTime
import play.api.data.*
import play.api.data.Forms.*
import reactivemongo.api.bson.*
import reactivemongo.api.ReadPreference
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext

import lila.common.Markdown
import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import lila.user.User

case class OpeningWiki(
    markup: Option[String],
    revisions: List[OpeningWiki.Revision]
):
  def markupForMove(move: String): Option[String] =
    markup map OpeningWiki.filterMarkupForMove(move)

final class OpeningWikiApi(coll: Coll, explorer: OpeningExplorer, cacheApi: CacheApi)(using ExecutionContext):

  import OpeningWiki.Revision

  given BSONDocumentHandler[Revision]    = Macros.handler
  given BSONDocumentHandler[OpeningWiki] = Macros.handler

  def apply(op: Opening, withRevisions: Boolean): Fu[OpeningWiki] = for
    wiki <- cache get op.key
    revisions <- withRevisions ?? {
      coll.primitiveOne[List[Revision]]($id(op.key), "revisions")
    }
  yield wiki.copy(revisions = (~revisions) take 25)

  def write(op: Opening, text: String, by: User): Funit =
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

  def popularOpeningsWithShortWiki: Fu[List[Opening]] =
    coll
      .aggregateList(100, ReadPreference.secondaryPreferred) { framework =>
        import framework.*
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
          id  <- doc.getAsOpt[OpeningKey]("_id")
          op  <- OpeningDb.shortestLines get id
        } yield op
      }

  private object markdown:

    private val renderer = new lila.common.MarkdownRender(
      autoLink = false,
      list = true,
      header = true,
      table = false,
      strikeThrough = false
    )

    private val moveNumberRegex = """(\d+)\.""".r
    def render(key: OpeningKey)(markdown: Markdown) = renderer(s"opening:$key") {
      markdown.map { moveNumberRegex.replaceAllIn(_, "$1{DOT}") }
    }.replace("{DOT}", ".")

  private val cache = cacheApi[OpeningKey, OpeningWiki](1024, "opening.wiki") {
    _.maximumSize(4096).buildAsyncFuture(compute)
  }

  private def compute(key: OpeningKey): Fu[OpeningWiki] = for {
    docOpt <- coll
      .aggregateOne() { framework =>
        import framework.*
        Match($id(key)) ->
          List(Project($doc("lastRev" -> $doc("$first" -> "$revisions"))))
      }
    _ <- updatePopularity(key)
    lastRev = docOpt.flatMap(_.getAsOpt[Revision]("lastRev"))
    text    = lastRev.map(_.text)
  } yield OpeningWiki(
    text map markdown.render(key),
    Nil
  )

  private def updatePopularity(key: OpeningKey): Funit =
    OpeningDb.shortestLines.get(key) ?? { op =>
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

object OpeningWiki:

  case class Revision(text: Markdown, by: UserId, at: DateTime)

  val form = Form(single("text" -> nonEmptyText(minLength = 10, maxLength = 10_000)))

  private val MoveLiRegex = """(?i)^<li>(\w{2,5}\+?):(.+)</li>""".r
  private def filterMarkupForMove(move: String)(markup: String) =
    markup.linesIterator collect {
      case MoveLiRegex(m, content) => if (m.toLowerCase == move.toLowerCase) s"<p>${content.trim}</p>" else ""
      case html                    => html
    } mkString "\n"
