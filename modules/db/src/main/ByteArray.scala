package lila.db

import scala.util.{ Try, Success, Failure }

import reactivemongo.bson._
import reactivemongo.bson.utils.Converters

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

  implicit val ByteArrayBSONHandler = new BSONHandler[BSONBinary, ByteArray] {
    def read(bin: BSONBinary) = ByteArray(bin.byteArray)
    def write(ba: ByteArray) = BSONBinary(ba.value, subtype)
  }

  def parseBytes(s: List[String]) = ByteArray(s map parseByte toArray)

  private def parseByte(s: String): Byte = {
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

  def subtype = Subtype.GenericBinarySubtype

  private val binarySubType = Converters hex2Str Array(subtype.value)
}
