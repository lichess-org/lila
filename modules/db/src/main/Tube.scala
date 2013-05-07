package lila.db

import Types.Coll

import play.api.libs.json._
import play.api.libs.functional.syntax._
import Reads.constraints._
import reactivemongo.bson._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._

trait InColl[A] { implicit def coll: Types.Coll }

case class Tube[Doc](
  reader: Reads[Doc],
  writer: Writes[Doc],
  flags: Seq[Tube.Flag.type ⇒ Tube.Flag] = Seq.empty)
    extends Reads[Doc]
    with Writes[Doc]
    with BSONDocumentReader[Option[Doc]] {

  implicit def reads(js: JsValue): JsResult[Doc] = reader reads js
  implicit def writes(doc: Doc): JsValue = writer writes doc

  def read(bson: BSONDocument): Option[Doc] = {
    val js = JsObjectReader read bson
    fromMongo(js) match {
      case JsSuccess(v, _) ⇒ Some(v)
      case e ⇒ {
        logwarn("[tube] Cannot read %s\n%s".format(js, e))
        None
      }
    }
  }

  def read(js: JsObject): JsResult[Doc] = reads(js)

  def write(doc: Doc): JsResult[JsObject] = writes(doc) match {
    case obj: JsObject ⇒ JsSuccess(obj)
    case something ⇒ {
      logwarn("[tube] Cannot write %s\ngot %s".format(doc, something))
      JsError()
    }
  }

  def toMongo(doc: Doc): JsResult[JsObject] = flag(_.NoId)(
    write(doc),
    write(doc) flatMap Tube.toMongoId
  )

  def fromMongo(js: JsObject): JsResult[Doc] = flag(_.NoId)(
    read(js),
    Tube.depath(Tube fromMongoId js) flatMap read
  )

  def inColl(c: Coll): TubeInColl[Doc] =
    new Tube[Doc](reader, writer, flags) with InColl[Doc] {
      def coll = c
    }

  private lazy val flagSet = flags.map(_(Tube.Flag)).toSet

  private def flag[A](f: Tube.Flag.type ⇒ Tube.Flag)(x: ⇒ A, y: ⇒ A) =
    flagSet contains f(Tube.Flag) fold (x, y)
}

object Tube {

  val json = Tube[JsObject](
    reader = __.read[JsObject],
    writer = __.write[JsObject])

  def toMongoId(js: JsValue): JsResult[JsObject] =
    js transform Helpers.rename('id, '_id)

  def fromMongoId(js: JsValue): JsResult[JsObject] =
    js transform Helpers.rename('_id, 'id)

  sealed trait Flag
  object Flag {
    case object NoId extends Flag
  }

  object Helpers {

    // Adds Writes[A].andThen combinator, symmetric to Reads[A].andThen
    // Defaults to original value on fail
    implicit final class LilaTubePimpedWrites[A](writes: Writes[A]) {
      def andThen(transformer: Reads[JsObject]): Writes[A] =
        writes.transform(Writes[JsValue] { origin ⇒
          origin transform transformer match {
            case err: JsError ⇒ {
              logwarn("[tube] Cannot transform %s\n%s".format(origin, err))
              origin
            }
            case JsSuccess(js, _) ⇒ js
          }
        })
    }

    def rename(from: Symbol, to: Symbol) = __.json update (
      (__ \ to).json copyFrom (__ \ from).json.pick
    ) andThen (__ \ from).json.prune

    def readDate(field: Symbol) =
      (__ \ field).json.update(of[JsObject] map (_ \ "$date"))

    def writeDate(field: Symbol) = (__ \ field).json.update(of[JsNumber] map {
      millis ⇒ Json.obj("$date" -> millis)
    })

    def merge(obj: JsObject) = __.read[JsObject] map (obj ++)
  }

  private def depath[A](r: JsResult[A]): JsResult[A] = r.flatMap(JsSuccess(_))
}
