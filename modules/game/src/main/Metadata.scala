package lila.game

import shogi.format.kif.KifParser
import shogi.format.csa.CsaParser
import shogi.format.ParsedNotation

import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import lila.db.ByteArray

private[game] case class Metadata(
    source: Option[Source],
    notationImport: Option[NotationImport],
    tournamentId: Option[String],
    simulId: Option[String],
    postGameStudy: Option[String],
    analysed: Boolean
) {

  def isEmpty = this == Metadata.empty
}

private[game] object Metadata {

  val empty = Metadata(None, None, None, None, None, false)
}

case class NotationImport(
    user: Option[String],
    date: Option[String],
    kif: Option[String],
    csa: Option[String],
    // hashed Kif for DB unicity
    h: Option[ByteArray]
) {
  def isCsa = csa.isDefined
  def isKif = kif.isDefined

  def notation: String = ~(kif.orElse(csa))

  def parseNotation: Option[ParsedNotation] =
    if (isCsa)
      CsaParser.full(notation).toOption
    else
      KifParser.full(notation).toOption
}

object NotationImport {

  def hash(notation: String) =
    ByteArray {
      MessageDigest getInstance "MD5" digest {
        notation.linesIterator
          .map(_.replace(" ", ""))
          .filter(_.nonEmpty)
          .to(List)
          .mkString("\n")
          .getBytes(UTF_8)
      } take 12
    }

  def make(
      user: Option[String],
      date: Option[String],
      kif: Option[String],
      csa: Option[String]
  ) =
    NotationImport(
      user = user,
      date = date,
      kif = kif,
      csa = csa,
      h = hash(~(kif.orElse(csa))).some
    )

  import reactivemongo.api.bson.Macros
  import ByteArray.ByteArrayBSONHandler
  implicit val notationImportBSONHandler = Macros.handler[NotationImport]
}
