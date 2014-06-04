package lila.game

import org.joda.time.DateTime

private[game] case class Metadata(
    source: Option[Source],
    pgnImport: Option[PgnImport],
    tournamentId: Option[String],
    poolId: Option[String],
    tvAt: Option[DateTime]) {

  def pgnDate = pgnImport flatMap (_.date)

  def pgnUser = pgnImport flatMap (_.user)

  def isEmpty = this == Metadata.empty
}

private[game] object Metadata {

  val empty = Metadata(None, None, None, None, None)
}

case class PgnImport(
  user: Option[String],
  date: Option[String],
  pgn: String)

object PgnImport {

  import reactivemongo.bson.Macros
  implicit val pgnImportBSONHandler = Macros.handler[PgnImport]
}
