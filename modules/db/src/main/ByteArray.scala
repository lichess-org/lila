package lila.db

import scala.util.{ Try, Success, Failure }

import play.api.data.validation.ValidationError
import play.api.libs.json._
import reactivemongo.bson._
import reactivemongo.bson.utils.Converters

import lila.common.PimpedJson._

case class ByteArray(value: Array[Byte]) {

  def isEmpty = value.isEmpty

  def toHexStr = Converters hex2Str value

  def showBytes: String = value map { b =>
    "%08d" format { b & 0xff }.toBinaryString.toInt
  } mkString ","

  override def toString = toHexStr
}

object ByteArray {

  val empty = ByteArray(Array())

  def fromHexStr(hexStr: String): Try[ByteArray] =
    Try(ByteArray(Converters str2Hex hexStr))

  implicit object ByteArrayBSONHandler extends BSONHandler[BSONBinary, ByteArray] {

    def read(bin: BSONBinary) = ByteArray(bin.value.readArray(bin.value.readable))

    def write(ba: ByteArray) = BSONBinary(ba.value, subtype)
  }

  implicit object JsByteArrayFormat extends OFormat[ByteArray] {

    def reads(json: JsValue) = (for {
      hexStr ← json str "$binary"
      bytes ← fromHexStr(hexStr).toOption
    } yield bytes) match {
      case None     => JsError(s"error reading ByteArray from $json")
      case Some(ba) => JsSuccess(ba)
    }

    def writes(byteArray: ByteArray) = Json.obj(
      "$binary" -> byteArray.toHexStr,
      "$type" -> binarySubType)
  }

  def parseByte(s: String): Byte = {
    var i = s.length - 1
    var sum = 0
    var mult = 1
    while (i >= 0) {
      s.charAt(i) match {
        case '1' => sum += mult
        case '0' =>
        case x   => sys error s"invalid binary literal: $x in $s"
      }
      mult *= 2
      i -= 1
    }
    sum.toByte
  }

  def parseBytes(s: List[String]) = ByteArray(s map parseByte toArray)

  def subtype = Subtype.GenericBinarySubtype

  private val binarySubType = Converters hex2Str Array(subtype.value)
}
