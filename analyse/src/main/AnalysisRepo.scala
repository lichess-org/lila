package lila.analyse

import lila.db.api._
import lila.db.Implicits._
import tube.analysisTube

import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._

import org.joda.time.DateTime
import org.scala_tools.time.Imports._

private[analyse] object AnalysisRepo {

  type ID = String

  def done(id: ID, a: Analysis) = $update($select(id),
    $set("done" -> true) ++ $set("encoded" -> a.encodeInfos) ++ $unset("fail")
  )

  def fail(id: ID, err: Failures) = $update.field(id, "fail", err.shows)

  def progress(id: ID, userId: ID) = $update($select(id),
    $set("uid" -> userId) ++ $set("done" -> false) ++ $set("date" -> DateTime.now),
    upsert = true)

  def doneById(id: ID): Fu[Option[Analysis]] =
    $find.one($select(id) ++ Json.obj("done" -> true))

  def isDone(id: ID): Fu[Boolean] =
    $count.exists($select(id) ++ Json.obj("done" -> true))

  def userInProgress(uid: ID): Fu[Boolean] = $count.exists(Json.obj(
    "fail" -> $exists(false),
    "uid" -> uid,
    "done" -> false,
    "date" -> $gt(DateTime.now - 15.minutes)
  ))

  def count = $count($select.all)
}
