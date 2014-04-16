package lila.analyse

import com.github.nscala_time.time.Imports._
import org.joda.time.DateTime
import play.api.libs.json.Json
import play.modules.reactivemongo.json.ImplicitBSONHandlers.JsObjectWriter

import lila.db.api._
import lila.db.Implicits._
import tube.analysisTube

object AnalysisRepo {

  type ID = String

  def done(id: ID, a: Analysis) = $update(
    $select(id),
    $set(Json.obj(
      "done" -> true,
      "data" -> Info.encodeList(a.infos)
    ))
  )

  def progress(id: ID, userId: ID) = $update(
    $select(id),
    $set(Json.obj(
      "uid" -> userId,
      "done" -> false,
      "date" -> $date(DateTime.now)
    )) ++ $unset("old", "data"),
    upsert = true)

  def byId(id: ID): Fu[Option[Analysis]] = $find byId id

  def doneById(id: ID): Fu[Option[Analysis]] =
    $find.one($select(id) ++ Json.obj("done" -> true))

  def doneByIds(ids: Seq[ID]): Fu[Seq[Option[Analysis]]] = $find byOrderedIds ids map { as =>
    ids.map { id =>
      as find { a => a.id == id && a.done }
    }
  }

  def doneByIdNotOld(id: ID): Fu[Option[Analysis]] =
    $find.one($select(id) ++ Json.obj("done" -> true, "old" -> $exists(false)))

  def isDone(id: ID): Fu[Boolean] =
    $count.exists($select(id) ++ Json.obj("done" -> true))

  def userInProgress(uid: ID): Fu[Option[String]] = $primitive.one(
    Json.obj(
      "uid" -> uid,
      "done" -> false,
      "date" -> $gt($date(DateTime.now - 30.minutes))),
    "_id")(_.asOpt[String])

  def recent(nb: Int): Fu[List[Analysis]] =
    $find($query(Json.obj("done" -> true)) sort $sort.desc("date"), nb)

  def skipping(skip: Int, nb: Int): Fu[List[Analysis]] =
    $find($query(Json.obj("done" -> true)) skip skip, nb)

  def count = $count($select.all)

  def remove(id: String) = $remove byId id
}
