package lila.studySearch

import lila.search._
import lila.study.{Study,StudyRepo,ChapterRepo}

import play.api.libs.json._
import play.api.libs.iteratee._

final class StudySearchApi(
  client: ESClient,
studyRepo: StudyRepo,
chapterRepo: ChapterRepo) extends SearchReadApi[Study, Query] {

  def search(query: Query, from: From, size: Size) =
    client.search(query, from, size) flatMap { res =>
      studyRepo byOrderedIds res.ids
    }

  def count(query: Query) = client.count(query) map (_.count)

  def store(study: Study) = client.store(Id(study.id), toDoc(study))

  private def toDoc(study: Study) = Json.obj(
    Fields.name -> study.name)

  def reset = client match {
    case c: ESClientHttp => c.putMapping >> {
      lila.log("studySearch").info(s"Index to ${c.index.name}")
      import lila.db.dsl._
      studyRepo.cursor($doc("enabled" -> true))
        .enumerateBulks(Int.MaxValue) |>>>
        Iteratee.foldM[Iterator[Study], Unit](()) {
          case (_, studies) =>
            c.storeBulk(studies.toList map (t => Id(t.id) -> toDoc(t)))
        }
    }
    case _ => funit
  }
}
