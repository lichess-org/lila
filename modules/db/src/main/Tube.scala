package lila.db

import scala.util.{ Try, Success, Failure }

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.bson._
import Reads.constraints._
import Types.Coll

import lila.common.LilaException

trait InColl[A] { implicit def coll: Types.Coll }

trait Tube[Doc] extends BSONDocumentReader[Option[Doc]]

case class BsTube[Doc](handler: BSONHandler[BSONDocument, Doc]) extends Tube[Doc] {

  def read(bson: BSONDocument): Option[Doc] = handler readTry bson match {
    case Success(doc) => Some(doc)
    case Failure(err) =>
      logerr(s"[tube] Cannot read ${lila.db.BSON.debug(bson)}\n$err\n${err.printStackTrace}")
      None
  }

  def write(doc: Doc): BSONDocument = handler write doc

  def inColl(c: Coll): BsTubeInColl[Doc] =
    new BsTube[Doc](handler) with InColl[Doc] { def coll = c }
}

case class JsTube[Doc](
  reader: Reads[Doc],
  writer: Writes[Doc],
  flags: Seq[JsTube.Flag.type => JsTube.Flag] = Seq.empty)
    extends Tube[Doc]
    with Reads[Doc]
    with Writes[Doc] {

  implicit def reads(js: JsValue): JsResult[Doc] = reader reads js
  implicit def writes(doc: Doc): JsValue = writer writes doc

  def read(bson: BSONDocument): Option[Doc] = {
    val js = JsObjectReader read bson
    fromMongo(js) match {
      case JsSuccess(v, _) => Some(v)
      case e =>
        logerr("[tube] Cannot read %s\n%s".format(js, e))
        None
    }
  }

  def read(js: JsObject): JsResult[Doc] = reads(js)

  def write(doc: Doc): JsResult[JsObject] = writes(doc) match {
    case obj: JsObject => JsSuccess(obj)
    case something =>
      logerr("[tube] Cannot write %s\ngot %s".format(doc, something))
      JsError()
  }

  def toMongo(doc: Doc): JsResult[JsObject] = flag(_.NoId)(
    write(doc),
    write(doc) flatMap JsTube.toMongoId
  )

  def fromMongo(js: JsObject): JsResult[Doc] = flag(_.NoId)(
    read(js),
    JsTube.depath(JsTube fromMongoId js) flatMap read
  )

  def inColl(c: Coll): JsTubeInColl[Doc] =
    new JsTube[Doc](reader, writer, flags) with InColl[Doc] { def coll = c }

  private lazy val flagSet = flags.map(_(JsTube.Flag)).toSet

  private def flag[A](f: JsTube.Flag.type => JsTube.Flag)(x: => A, y: => A) =
    flagSet contains f(JsTube.Flag) fold (x, y)
}

object JsTube {

  val json = JsTube[JsObject](
    __.read[JsObject],
    __.write[JsObject],
    Seq(_.NoId) // no need to rename the ID field as we are not mapping
  )

  private val toMongoIdOp = Helpers.rename('id, '_id)
  def toMongoId(js: JsValue): JsResult[JsObject] = js transform toMongoIdOp

  private val fromMongoIdOp = Helpers.rename('_id, 'id)
  def fromMongoId(js: JsValue): JsResult[JsObject] = js transform fromMongoIdOp

  sealed trait Flag
  object Flag {
    case object NoId extends Flag
  }

  object Helpers {

    // Adds Writes[A].andThen combinator, symmetric to Reads[A].andThen
    // Explodes on failure
    implicit final class LilaTubePimpedWrites[A](writes: Writes[A]) {
      def andThen(transformer: Reads[JsObject]): Writes[A] =
        writes.transform(Writes[JsValue] { origin =>
          origin transform transformer match {
            case err: JsError     => throw LilaException("[tube] Cannot transform %s\n%s".format(origin, err))
            case JsSuccess(js, _) => js
          }
        })
    }

    def rename(from: Symbol, to: Symbol) = __.json update (
      (__ \ to).json copyFrom (__ \ from).json.pick
    ) andThen (__ \ from).json.prune

    def readDate(field: Symbol) =
      (__ \ field).json.update(of[JsObject] map (_ \ "$date"))

    def readDateOpt(field: Symbol) = readDate(field) orElse json.reader

    def writeDate(field: Symbol) = (__ \ field).json.update(of[JsNumber] map {
      millis => Json.obj("$date" -> millis)
    })

    def writeDateOpt(field: Symbol) = (__ \ field).json.update(of[JsNumber] map {
      millis => Json.obj("$date" -> millis)
    }) orElse json.reader

    def merge(obj: JsObject) = __.read[JsObject] map (obj ++)
  }

  private def depath[A](r: JsResult[A]): JsResult[A] = r.flatMap(JsSuccess(_))
}
