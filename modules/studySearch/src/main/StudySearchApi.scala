package lila.studySearch

import akka.actor.ActorRef
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.concurrent.duration._

import chess.format.pgn.Tag
import lila.hub.MultiThrottler
import lila.search._
import lila.socket.tree.Node.{ Comments, Comment }
import lila.study.{ Study, Chapter, StudyRepo, ChapterRepo, RootOrNode }

final class StudySearchApi(
    client: ESClient,
    indexThrottler: ActorRef,
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo) extends SearchReadApi[Study, Query] {

  def search(query: Query, from: From, size: Size) = {
    client.search(query, from, size) flatMap { res =>
      studyRepo byOrderedIds res.ids
    }
  }.mon(_.study.search.query.time) >>- lila.mon.study.search.query.count()

  def count(query: Query) = client.count(query) map (_.count)

  def store(study: Study) = fuccess {
    indexThrottler ! MultiThrottler.work(
      id = study.id,
      run = studyRepo byId study.id flatMap { _ ?? doStore },
      delay = 30.seconds.some)
  }

  private def doStore(study: Study) = {
    getChapters(study) flatMap { s =>
      client.store(Id(s.study.id), toDoc(s))
    }
  }.mon(_.study.search.index.time) >>- lila.mon.study.search.index.count()

  private def toDoc(s: Study.WithActualChapters) = Json.obj(
    Fields.name -> s.study.name,
    Fields.owner -> s.study.ownerId,
    Fields.members -> s.study.members.ids,
    Fields.chapterNames -> s.chapters.collect {
      case c if !Chapter.isDefaultName(c.name) => c.name
    }.mkString(" "),
    Fields.chapterTexts -> noMultiSpace(s.chapters.map(chapterText).mkString(" ")),
    // Fields.createdAt -> study.createdAt)
    // Fields.updatedAt -> study.updatedAt,
    Fields.likes -> s.study.likes.value,
    Fields.public -> s.study.isPublic)

  private val relevantPgnTags: Set[chess.format.pgn.TagType] = Set(
    Tag.Variant, Tag.Event, Tag.Round,
    Tag.White, Tag.Black,
    Tag.ECO, Tag.Opening, Tag.Annotator)

  private def chapterText(c: Chapter): String = {
    nodeText(c.root) :: c.setup.fromPgn.?? { pgn =>
      pgn.tags collect {
        case Tag(name, value) if relevantPgnTags.contains(name) => value
      }
    }
  }.flatten mkString " "

  private def nodeText(n: RootOrNode): String =
    commentsText(n.comments) + " " + n.children.nodes.map(nodeText).mkString(" ")

  private def commentsText(cs: Comments): String =
    cs.value.map(_.text.value) mkString " "

  private def getChapters(s: Study): Fu[Study.WithActualChapters] =
    chapterRepo.orderedByStudy(s.id) map { Study.WithActualChapters(s, _) }

  private val multiSpaceRegex = """\s{2,}""".r
  private def noMultiSpace(text: String) = multiSpaceRegex.replaceAllIn(text, " ")

  def reset = client match {
    case c: ESClientHttp => c.putMapping >> {
      lila.log("studySearch").info(s"Index to ${c.index.name}")

      import lila.db.dsl._
      import reactivemongo.play.iteratees.cursorProducer

      studyRepo.cursor($empty).enumerator() |>>>
        Iteratee.foldM[Study, Unit](()) {
          case (_, study) => doStore(study)
        }
    }
    case _ => funit
  }
}
