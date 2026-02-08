package lila.opening

import chess.opening.{ Opening, OpeningDb, OpeningKey }
import play.api.data.*
import play.api.data.Forms.*
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi

case class OpeningWiki(
    markup: Option[Html],
    revisions: List[OpeningWiki.Revision],
    popularity: Long
):
  def hasMarkup = markup.exists(_.value.nonEmpty)
  def markupForMove(move: String): Option[Html] =
    markup.map(OpeningWiki.filterMarkupForMove(move))

final class OpeningWikiApi(coll: Coll, explorer: OpeningExplorer, cacheApi: CacheApi)(using Executor):

  import OpeningWiki.Revision

  given BSONDocumentHandler[Revision] = Macros.handler
  given BSONDocumentHandler[OpeningWiki] = Macros.handler

  def apply(op: Opening, withRevisions: Boolean): Fu[OpeningWiki] = for
    wiki <- cache.get(op.key)
    revisions <- withRevisions.so:
      coll.primitiveOne[List[Revision]]($id(op.key), "revisions")
  yield wiki.copy(revisions = (~revisions).take(25))

  def write(op: Opening, text: String, by: UserId): Funit =
    for _ <- coll.update
        .one(
          $id(op.key),
          $doc(
            "$push" -> $doc(
              "revisions" -> $doc(
                "$each" -> List(Revision(Markdown(text), by.id, nowInstant)),
                "$position" -> 0,
                "$slice" -> 30
              )
            )
          ),
          upsert = true
        )
    yield cache.put(op.key, compute(op.key))

  def popularOpeningsWithShortWiki: Fu[List[Opening]] =
    coll
      .aggregateList(100, _.sec): framework =>
        import framework.*
        Project($doc("popularity" -> true, "rev" -> $doc("$first" -> "$revisions"))) -> List(
          AddFields($doc("len" -> $doc("$strLenBytes" -> $doc("$ifNull" -> $arr("$rev.text", ""))))),
          Match($doc("len".$lt(300))),
          Sort(Descending("popularity")),
          Project($doc("_id" -> true))
        )
      .map: docs =>
        for
          doc <- docs
          id <- doc.getAsOpt[OpeningKey]("_id")
          op <- OpeningDb.shortestLines.get(id)
        yield op

  private object markdown:

    private val renderer = lila.common.MarkdownRender(
      autoLink = false,
      list = true,
      header = true,
      table = false,
      strikeThrough = false
    )

    private val moveNumberRegex = """(\d+)\.""".r
    def render(key: OpeningKey)(markdown: Markdown): Html = renderer(s"opening:$key") {
      markdown.map { moveNumberRegex.replaceAllIn(_, "$1{DOT}") }
    }.map(_.replace("{DOT}", "."))

  private val cache = cacheApi[OpeningKey, OpeningWiki](1024, "opening.wiki"):
    _.maximumSize(4096).buildAsyncFuture(compute)

  private def compute(key: OpeningKey): Fu[OpeningWiki] = for
    docOpt <- coll.aggregateOne(): F =>
      F.Match($id(key)) -> List(F.Project($doc("lastRev" -> $doc("$first" -> "$revisions"))))
    popularity <- updatePopularity(key)
    lastRev = docOpt.flatMap(_.getAsOpt[Revision]("lastRev"))
    text = lastRev.map(_.text)
  yield OpeningWiki(text.map(markdown.render(key)), Nil, popularity)

  private def updatePopularity(key: OpeningKey): Fu[Long] =
    OpeningDb.shortestLines.get(key).so { op =>
      explorer
        .simplePopularity(op)
        .flatMapz: popularity =>
          val update = $set("popularity" -> popularity, "popularityAt" -> nowInstant)
          coll.update.one($id(key), update, upsert = true).inject(popularity)
    }

object OpeningWiki:

  case class Revision(text: Markdown, by: UserId, at: Instant)

  val form = Form(single("text" -> nonEmptyText(minLength = 10, maxLength = 10_000)))

  private val MoveLiRegex = """(?i)^<li>(\w{2,5}\+?):(.+)</li>""".r
  private def filterMarkupForMove(move: String)(markup: Html) = markup.map:
    _.linesIterator
      .collect:
        case MoveLiRegex(m, content) =>
          if m.toLowerCase == move.toLowerCase then s"<p>${content.trim}</p>" else ""
        case html => html
      .mkString("\n")

  private val priorityByPopularityPercent = List(3, 0.5, 0.05, 0.005, 0)
  def priorityOf(explored: OpeningExplored) =
    priorityByPopularityPercent.indexWhere(_ <= ~explored.lastPopularityPercent)
