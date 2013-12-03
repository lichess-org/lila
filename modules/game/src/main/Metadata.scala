package lila.game

import org.joda.time.DateTime

private[game] case class Metadata(
    source: Option[Source],
    pgnImport: Option[PgnImport],
    tournamentId: Option[String],
    tvAt: Option[DateTime]) {

  def pgnDate = pgnImport flatMap (_.date)

  def pgnUser = pgnImport flatMap (_.user)
}

object Metadata {

  import reactivemongo.bson._
  import lila.db.BSON
  import PgnImport.pgnImportBSONHandler

  implicit val metadataBSONHandler = new BSON[Metadata] {

    val source = "so"
    val pgnImport = "pgni"
    val tournamentId = "tid"
    val tvAt = "tv"

    def reads(r: BSON.Reader) = Metadata(
      source = r intO source flatMap Source.apply,
      pgnImport = r.getO[PgnImport](pgnImport)(PgnImport.pgnImportBSONHandler),
      tournamentId = r strO tournamentId,
      tvAt = r dateO tvAt)

    def writes(w: BSON.Writer, o: Metadata) = BSONDocument(
      source -> o.source.map(_.id),
      pgnImport -> o.pgnImport,
      tournamentId -> o.tournamentId,
      tvAt -> o.tvAt.map(w.date)
    )
  }
}

case class PgnImport(
  user: Option[String],
  date: Option[String],
  pgn: String)

object PgnImport {

  import reactivemongo.bson.Macros
  implicit val pgnImportBSONHandler = Macros.handler[PgnImport]
}
