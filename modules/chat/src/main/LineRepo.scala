package lila.chat

import org.joda.time.DateTime
import play.api.libs.json._
import play.modules.reactivemongo.json.BSONFormats.toJSON
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.api._
import reactivemongo.bson._

import lila.common.PimpedJson._
import lila.db.api._
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.Implicits._
import tube.lineTube

object LineRepo {

  import Line.{ BSONFields ⇒ L }

  def find(chanKeys: Set[String], troll: Boolean, blocks: Set[String], limit: Int): Fu[List[Line]] =
    $find($query(
      Json.obj(L.troll -> troll.fold($in(List(true, false)), JsBoolean(false))) ++
        Json.obj(L.userId -> $nin(blocks)) ++
        Json.obj(L.chan -> $in(chanKeys))
    ) sort $sort.desc(L.date), limit)

  def insert(line: Line) = $insert bson line

  def leastTalked(userId: String, chanKeys: Seq[String]): Fu[Option[String]] = {
    import reactivemongo.core.commands._
    val command = Aggregate(lineTube.coll.name, Seq(
      Match(JsObjectWriter write Json.obj(
        L.userId -> userId,
        L.chan -> $in(chanKeys),
        L.date -> $gt($date(DateTime.now.minusMinutes(30)))
      )),
      GroupField(L.chan)("nb" -> SumValue(1)),
      Sort(Seq(Ascending("nb"))),
      Limit(1)
    ))
    lineTube.coll.db.command(command) map { stream ⇒
      stream.headOption flatMap { obj ⇒
        toJSON(obj).asOpt[JsObject] flatMap { _ str "_id" }
      }
    }
  }

  def leastActive(chanKeys: Seq[String]): Fu[Option[String]] = {
    import reactivemongo.core.commands._
    val command = Aggregate(lineTube.coll.name, Seq(
      Match(JsObjectWriter write Json.obj(
        L.chan -> $in(chanKeys),
        L.date -> $gt($date(DateTime.now.minusMinutes(15)))
      )),
      GroupField(L.chan)("nb" -> SumValue(1)),
      Sort(Seq(Ascending("nb"))),
      Limit(1)
    ))
    lineTube.coll.db.command(command) map { stream ⇒
      stream.headOption flatMap { obj ⇒
        toJSON(obj).asOpt[JsObject] flatMap { _ str "_id" }
      }
    }
  }
}
