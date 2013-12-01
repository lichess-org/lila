package lila.db

import scala.util.{ Try, Success, Failure }

import play.api.data.validation.ValidationError
import play.api.libs.json._
import reactivemongo.bson._
import reactivemongo.bson.utils.Converters

import lila.common.PimpedJson._

case class ByteArray(value: Array[Byte]) {

  def toHexStr = Converters hex2Str value

  override def toString = toHexStr
}

object ByteArray {

  def fromHexStr(hexStr: String): Try[ByteArray] =
    Try(ByteArray(Converters str2Hex hexStr))

  implicit object JsByteArrayFormat extends Format[ByteArray] {

    def reads(json: JsValue) = json match {
      case JsObject(str) ⇒ fromHexStr(str) match {
        case Success(ba) ⇒ JsSuccess(ba)
        case Failure(e)  ⇒ JsError(s"error deserializing hex ${e.getMessage}")
      }
      case _ ⇒ JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsstring"))))
    }

    def writes(byteArray: ByteArray) = Json.obj(
      "$binary" -> Converters.hex2Str(binary.value.slice(remaining).readArray(remaining)),
      "$type" -> Converters.hex2Str(Array(binary.subtype.value.toByte)))
  }
}
