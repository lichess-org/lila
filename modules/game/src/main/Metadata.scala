package lila.game

import org.joda.time.DateTime

private[game] case class Metadata(
    source: Option[Source],
    pgnImport: Option[PgnImport],
    ficsRelay: Option[FicsRelay],
    tournamentId: Option[String],
    tvAt: Option[DateTime],
    analysed: Boolean) {

  def pgnDate = pgnImport flatMap (_.date)

  def pgnUser = pgnImport flatMap (_.user)

  def isEmpty = this == Metadata.empty
}

private[game] object Metadata {

  val empty = Metadata(None, None, None, None, None, false)
}

case class PgnImport(
  user: Option[String],
  date: Option[String],
  pgn: String)

object PgnImport {

  import reactivemongo.bson.Macros
  implicit val pgnImportBSONHandler = Macros.handler[PgnImport]
}

case class FicsRelay(
  white: String,
  black: String)

object FicsRelay {

  import reactivemongo.bson.Macros
  implicit val ficsRelayBSONHandler = Macros.handler[FicsRelay]
}
