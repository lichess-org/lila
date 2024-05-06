package lila.db

import reactivemongo.api.bson.*

import scala.util.Try

case class ByteArray(value: Array[Byte]):

  def isEmpty = value.lengthIs == 0

  def toHexStr = ByteArray.hex.hex2Str(value)

  def showBytes: String =
    value
      .map: b =>
        "%08d".format({ b & 0xff }.toBinaryString.toInt)
      .mkString(",")

  override def toString = toHexStr

object ByteArray:

  val empty = ByteArray(Array())

  def fromHexStr(hexStr: String): Try[ByteArray] =
    Try(ByteArray(hex.str2Hex(hexStr)))

  given arrayByteHandler: BSONHandler[Array[Byte]] = dsl.quickHandler[Array[Byte]](
    { case v: BSONBinary => v.byteArray },
    v => BSONBinary(v, subtype)
  )

  given byteArrayHandler: BSONHandler[ByteArray] = dsl.quickHandler[ByteArray](
    { case v: BSONBinary => ByteArray(v.byteArray) },
    v => BSONBinary(v.value, subtype)
  )

  given Conversion[Array[Byte], ByteArray] = ByteArray(_)

  def parseBytes(s: List[String]) = ByteArray(s.map(parseByte).toArray)

  private def parseByte(s: String): Byte =
    var i    = s.length - 1
    var sum  = 0
    var mult = 1
    while i >= 0 do
      s.charAt(i) match
        case '1' => sum += mult
        case '0' =>
        case x   => sys.error(s"invalid binary literal: $x in $s")
      mult *= 2
      i -= 1
    sum.toByte

  // from https://github.com/ReactiveMongo/ReactiveMongo-BSON/blob/master/api/src/main/scala/Digest.scala
  private object hex:

    private val HEX_CHARS: Array[Char] = "0123456789abcdef".toCharArray

    /** Turns a hexadecimal String into an array of Byte. */
    def str2Hex(str: String): Array[Byte] =
      val sz    = str.length / 2
      val bytes = new Array[Byte](sz)

      var i = 0
      while i < sz do
        val t = 2 * i
        bytes(i) = Integer.parseInt(str.substring(t, t + 2), 16).toByte
        i += 1

      bytes

    /** Turns an array of Byte into a String representation in hexadecimal. */
    def hex2Str(bytes: Array[Byte]): String =
      val len = bytes.length
      val hex = new Array[Char](2 * len)

      var i = 0
      while i < len do
        val b = bytes(i)

        val t = 2 * i // index in output buffer

        hex(t) = HEX_CHARS((b & 0xf0) >>> 4)
        hex(t + 1) = HEX_CHARS(b & 0x0f)

        i = i + 1

      new String(hex)

  def subtype = Subtype.GenericBinarySubtype
