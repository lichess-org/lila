package lila.security

import lila.common.Iso

case class FingerPrint(value: String) extends AnyVal

case class FingerHash(value: String) extends AnyVal

object FingerHash {

  val length = 8

  def apply(print: FingerPrint): Option[FingerHash] = try {
    import java.util.Base64
    import org.apache.commons.codec.binary.Hex
    FingerHash {
      Base64.getEncoder encodeToString {
        Hex decodeHex print.value.toArray
      } take length
    } some
  } catch {
    case _: Exception => none
  }

  implicit val fingerHashIso = Iso.string[FingerHash](FingerHash.apply, _.value)
}
