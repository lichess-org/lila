package lila.study

import play.api.libs.json.*

import lila.db.dsl.{ *, given }
import lila.db.{ AsyncColl, JSON }

final class StudyBookmarks(coll: AsyncColl)(using Executor):

  def load(userId: UserId): Fu[JsArray] =
    coll(_.byId[Bdoc](userId)).map:
      _.fold(Json.arr())(doc => StudyBookmarks.read((JSON.jval(doc) \ "entries").as[JsArray]).get)

  def save(userId: UserId, entries: JsArray): Funit =
    coll(_.update.one(bid(userId), bset("entries" -> JSON.bval(entries)), upsert = true)).void

  def delete(userId: UserId): Funit = coll(_.delete.one(bid(userId))).void

object StudyBookmarks:

  def studyIds(entries: JsArray): List[StudyId] =
    entries.value.toList
      .flatMap: entry =>
        (entry \ "children").asOpt[JsArray] match
          case Some(children) => studyIds(children)
          case None => (entry \ "studyId").asOpt[String].toList.map(StudyId.apply)
      .distinct

  def read(json: JsValue): JsResult[JsArray] =
    Reads.list(using Reads[JsObject](readEntry)).reads(json).map(JsArray.apply)

  private def readEntry(json: JsValue): JsResult[JsObject] =
    (json \ "name")
      .validate[String]
      .flatMap: name =>
        (json \ "children").toOption match
          case Some(children) =>
            read(children).map(entries => Json.obj("name" -> name, "children" -> entries))
          case None =>
            for
              studyId <- (json \ "studyId").validate[String]
              studyName <- (json \ "studyName").validate[String]
            yield Json.obj("name" -> name, "studyId" -> studyId, "studyName" -> studyName)
