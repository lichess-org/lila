package lila.studySearch

import akka.actor.ActorRef
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.concurrent.duration._

import chess.format.pgn.Tag
import lila.hub.LateMultiThrottler
import lila.search._
import lila.study.{ Study, Chapter, StudyRepo, ChapterRepo, RootOrNode }
import lila.tree.Node.Comments

final class StudySearchApi(
    client: ESClient,
    indexThrottler: ActorRef,
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo
) extends SearchReadApi[Study, Query] {

  def search(query: Query, from: From, size: Size) = {
    client.search(query, from, size) flatMap { res =>
      studyRepo byOrderedIds res.ids.map(Study.Id.apply)
    }
  }.mon(_.study.search.query.time) >>- lila.mon.study.search.query.count()

  def count(query: Query) = client.count(query) map (_.count)

  def store(study: Study) = fuccess {
    indexThrottler ! LateMultiThrottler.work(
      id = study.id.value,
      run = studyRepo byId study.id flatMap { _ ?? doStore },
      delay = 30.seconds.some
    )
  }

  private def doStore(study: Study) = {
    getChapters(study) flatMap { s =>
      client.store(Id(s.study.id.value), toDoc(s))
    }
  }.mon(_.study.search.index.time) >>- lila.mon.study.search.index.count()

  private def toDoc(s: Study.WithActualChapters) = Json.obj(
    Fields.name -> s.study.name.value,
    Fields.owner -> s.study.ownerId,
    Fields.members -> s.study.members.ids,
    Fields.chapterNames -> s.chapters.collect {
      case c if !Chapter.isDefaultName(c.name) => c.name.value
    }.mkString(" "),
    Fields.chapterTexts -> noMultiSpace(s.chapters.map(chapterText).mkString(" ")),
    // Fields.createdAt -> study.createdAt)
    // Fields.updatedAt -> study.updatedAt,
    Fields.likes -> s.study.likes.value,
    Fields.public -> s.study.isPublic
  )

  private val relevantPgnTags: Set[chess.format.pgn.TagType] = Set(
    Tag.Variant, Tag.Event, Tag.Round,
    Tag.White, Tag.Black,
    Tag.ECO, Tag.Opening, Tag.Annotator
  )

  private def chapterText(c: Chapter): String = {
    nodeText(c.root) :: c.tags.value.collect {
      case Tag(name, value) if relevantPgnTags.contains(name) => value
    } :: extraText(c)
  }.mkString(" ").trim

  private def extraText(c: Chapter): List[String] = List(
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

  private val multiSpaceRegex = """\s{2,}""".r
  private def noMultiSpace(text: String) = multiSpaceRegex.replaceAllIn(text, " ")

  import reactivemongo.play.iteratees.cursorProducer
  def reset(system: akka.actor.ActorSystem) = client match {
    case c: ESClientHttp => c.putMapping >> {
      logger.info(s"Index to ${c.index.name}")
      import lila.db.dsl._
      studyRepo.cursor($empty).enumerator() |>>>
        Iteratee.foldM[Study, Int](0) {
          case (nb, study) =>
            lila.common.Future.retry(doStore(study), 10 seconds, 10)(system) inject {
              if (nb % 100 == 0) logger.info(s"Indexed $nb studies")
              nb + 1
            }
        }
    } >> client.refresh
    case _ => funit
  }
}
