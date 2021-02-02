package lila.studySearch

import akka.actor._
import akka.stream.scaladsl._
import chess.format.pgn.Tag
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json._
import scala.concurrent.duration._

import lila.hub.LateMultiThrottler
import lila.search._
import lila.study.{ Chapter, ChapterRepo, RootOrNode, Study, StudyRepo }
import lila.tree.Node.Comments

final class StudySearchApi(
    client: ESClient,
    indexThrottler: ActorRef,
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem,
    mat: akka.stream.Materializer
) extends SearchReadApi[Study, Query] {

  def search(query: Query, from: From, size: Size) =
    client.search(query, from, size) flatMap { res =>
      studyRepo byOrderedIds res.ids.map(Study.Id.apply)
    }

  def count(query: Query) = client.count(query) dmap (_.count)

  def store(study: Study) =
    fuccess {
      indexThrottler ! LateMultiThrottler.work(
        id = study.id.value,
        run = studyRepo byId study.id flatMap { _ ?? doStore },
        delay = 30.seconds.some
      )
    }

  private def doStore(study: Study) =
    getChapters(study)
      .flatMap { s =>
        client.store(Id(s.study.id.value), toDoc(s))
      }
      .prefixFailure(study.id.value)

  private def toDoc(s: Study.WithActualChapters) =
    Json.obj(
      Fields.name    -> s.study.name.value,
      Fields.owner   -> s.study.ownerId,
      Fields.members -> s.study.members.ids,
      Fields.chapterNames ->
        s.chapters
          .collect { case c if !Chapter.isDefaultName(c.name) => c.name.value }
          .mkString(" "),
      Fields.chapterTexts -> noMultiSpace {
        (s.study.description.toList :+ s.chapters.flatMap(chapterText)).mkString(" ")
      },
      Fields.topics -> s.study.topicsOrEmpty.value.map(_.value),
      // Fields.createdAt -> study.createdAt)
      // Fields.updatedAt -> study.updatedAt,
      Fields.likes  -> s.study.likes.value,
      Fields.public -> s.study.isPublic
    )

  private val relevantPgnTags: Set[chess.format.pgn.TagType] = Set(
    Tag.Variant,
    Tag.Event,
    Tag.Round,
    Tag.White,
    Tag.Black,
    Tag.ECO,
    Tag.Opening,
    Tag.Annotator
  )

  private def chapterText(c: Chapter): List[String] = {
    nodeText(c.root) :: c.tags.value.collect {
      case Tag(name, value) if relevantPgnTags.contains(name) => value
    } ::: extraText(c)
  }

  private def extraText(c: Chapter): List[String] =
    List(
      c.isPractice option "practice",
      c.isConceal option "conceal puzzle",
      c.isGamebook option "lesson",
      c.description
    ).flatten

  private def nodeText(n: RootOrNode): String =
    commentsText(n.comments) + " " + n.children.nodes.map(nodeText).mkString(" ")

  private def commentsText(cs: Comments): String =
    cs.value.map(_.text.value) mkString " "

  private def getChapters(s: Study): Fu[Study.WithActualChapters] =
    chapterRepo.orderedByStudy(s.id) map { Study.WithActualChapters(s, _) }

  private val multiSpaceRegex            = """\s{2,}""".r
  private def noMultiSpace(text: String) = multiSpaceRegex.replaceAllIn(text, " ")

  def reset(sinceStr: String) =
    client match {
      case c: ESClientHttp =>
        {
          val sinceOption: Either[Unit, Option[DateTime]] =
            if (sinceStr == "reset") Left(()) else Right(parseDate(sinceStr))
          val since = sinceOption match {
            case Right(None) => sys error "Missing since date argument"
            case Right(Some(date)) =>
              logger.info(s"Resume since $date")
              date
            case _ =>
              logger.info("Reset study index")
              c.putMapping.await(10.seconds, "studyMapping")
              parseDate("2011-01-01").get
          }
          logger.info(s"Index to ${c.index.name} since $since")
          val retryLogger = logger.branch("index")
          import lila.db.dsl._
          Source
            .futureSource {
              studyRepo
                .sortedCursor(
                  $doc("createdAt" $gte since),
                  sort = $sort asc "createdAt"
                )
                .map(_.documentSource())
            }
            .via(lila.common.LilaStream.logRate[Study]("study index")(logger))
            .mapAsyncUnordered(8) { study =>
              lila.common.Future.retry(() => doStore(study), 5 seconds, 10, retryLogger.some)
            }
            .toMat(Sink.ignore)(Keep.right)
            .run()
        } >> client.refresh
      case _ => funit
    }

  private def parseDate(str: String): Option[DateTime] = {
    val datePattern   = "yyyy-MM-dd"
    val dateFormatter = DateTimeFormat forPattern datePattern
    scala.util.Try(dateFormatter parseDateTime str).toOption
  }
}
