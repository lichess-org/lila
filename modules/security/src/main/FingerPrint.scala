package lila.security

import lila.common.Iso

case class FingerPrint(value: String) extends AnyVal {
  def hash: Option[FingerHash] = FingerHash(this)
}

case class FingerHash(value: String) extends AnyVal with StringValue

object FingerHash {

  val length = 8

  def apply(print: FingerPrint): Option[FingerHash] =
    try {
      import java.util.Base64
      import org.apache.commons.codec.binary.Hex
      FingerHash {
        Base64.getEncoder encodeToString {
          Hex decodeHex normalize(print).toArray
        } take length
      } some
    } catch {
      case _: Exception => none
    }

  private def normalize(fp: FingerPrint): String = {
    val str = fp.value.replace("-", "")
    if (str.length % 2 != 0) s"${str}0" else str
  }

  implicit val fingerHashIso     = Iso.string[FingerHash](FingerHash.apply, _.value)
  implicit val fingerHashHandler = lila.db.BSON.isoHandler[FingerHash, String](fingerHashIso)
}
