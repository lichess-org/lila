package lila.security

import lila.core.security.FingerHash

opaque type FingerPrint = String
object FingerPrint extends OpaqueString[FingerPrint]:
  extension (a: FingerPrint) def hash: Option[FingerHash] = FingerHash.from(a)

object FingerHash:

  val length = 12

  def from(print: FingerPrint): Option[FingerHash] =
    try
      import java.util.Base64
      import org.apache.commons.codec.binary.Hex
      lila.core.security.FingerHash {
        Base64.getEncoder
          .encodeToString(Hex.decodeHex(normalize(print).toArray))
          .take(length)
      }.some
    catch case _: Exception => none

  private def normalize(fp: FingerPrint): String =
    val str = fp.value.replace("-", "")
    if str.length % 2 != 0 then s"${str}0" else str
