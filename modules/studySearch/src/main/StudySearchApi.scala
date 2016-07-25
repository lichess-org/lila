package lila.studySearch

import lila.search._
import lila.study.{ Study, Chapter, StudyRepo, ChapterRepo }

import play.api.libs.iteratee._
import play.api.libs.json._

final class StudySearchApi(
    client: ESClient,
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo) extends SearchReadApi[Study, Query] {

  def search(query: Query, from: From, size: Size) =
    client.search(query, from, size) flatMap { res =>
      studyRepo byOrderedIds res.ids
    }

  def count(query: Query) = client.count(query) map (_.count)

  def store(study: Study) = getChapters(study) flatMap { s =>
    client.store(Id(s.study.id), toDoc(s))
  }

  private def toDoc(s: Study.WithActualChapters) = Json.obj(
    Fields.name -> s.study.name,
    Fields.owner -> s.study.ownerId,
    Fields.members -> s.study.members.ids,
    Fields.chapters -> JsArray(s.chapters.map(chapterToDoc)),
    // Fields.createdAt -> study.createdAt)
    // Fields.updatedAt -> study.updatedAt,
    // Fields.rank -> study.rank,
    Fields.public -> s.study.isPublic,
    Fields.likes -> s.study.likes.value)

  private def chapterToDoc(c: Chapter) = Json.obj(
    Fields.chapter.name -> c.name)

  private def getChapters(s: Study): Fu[Study.WithActualChapters] =
    chapterRepo.orderedByStudy(s.id) map { Study.WithActualChapters(s, _) }

  def reset = client match {
    case c: ESClientHttp => c.putMapping >> {
      lila.log("studySearch").info(s"Index to ${c.index.name}")
      import lila.db.dsl._
      studyRepo.cursor($empty).enumerate() |>>>
        Iteratee.foldM[Study, Unit](()) {
          case (_, study) => store(study)
        }
    }
    case _ => funit
  }
}
