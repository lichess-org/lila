package lidraughts.game

import java.security.MessageDigest
import lidraughts.db.ByteArray
import org.joda.time.DateTime

private[game] case class Metadata(
    source: Option[Source],
    pdnImport: Option[PdnImport],
    tournamentId: Option[String],
    simulId: Option[String],
    tvAt: Option[DateTime],
    analysed: Boolean
) {

  def pdnDate = pdnImport flatMap (_.date)

  def pdnUser = pdnImport flatMap (_.user)

  def isEmpty = this == Metadata.empty
}

private[game] object Metadata {

  val empty = Metadata(None, None, None, None, None, false)
}

case class PdnImport(
    user: Option[String],
    date: Option[String],
    pdn: String,
    // hashed PDN for DB unicity
    h: Option[ByteArray]
)

object PdnImport {

  def hash(pdn: String) = ByteArray {
    MessageDigest getInstance "MD5" digest
      pdn.lines.map(_.replace(" ", "")).filter(_.nonEmpty).mkString("\n").getBytes("UTF-8") take 12
  }

  def make(
    user: Option[String],
    date: Option[String],
    pdn: String
  ) = PdnImport(
    user = user,
    date = date,
    pdn = pdn,
    h = hash(pdn).some
  )

  import reactivemongo.bson.Macros
  import ByteArray.ByteArrayBSONHandler
  implicit val pdnImportBSONHandler = Macros.handler[PdnImport]
}
