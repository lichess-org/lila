package lila.game

import org.joda.time.DateTime

private[game] case class Metadata(
    source: Option[Source],
    pgnImport: Option[PgnImport],
    tournamentId: Option[String],
    tvAt: Option[DateTime]) {

  def encode = RawMetadata(
    so = source map (_.id),
    pgni = pgnImport,
    tid = tournamentId,
    tv = tvAt)

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
      pgnImport = r.getO[PgnImport](pgnImport),
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

private[game] case class RawMetadata(
    so: Option[Int],
    pgni: Option[PgnImport],
    tid: Option[String],
    tv: Option[DateTime]) {

  def decode = Metadata(
    source = so flatMap Source.apply,
    pgnImport = pgni,
    tournamentId = tid,
    tvAt = tv)
}

private[game] object RawMetadata {

  import lila.db.Tube
  import Tube.Helpers._
  import play.api.libs.json._

  private implicit def importTube = PgnImport.tube

  private def defaults = Json.obj(
    "pgni" -> none[PgnImport],
    "tid" -> none[String],
    "tv" -> none[DateTime])

  private[game] lazy val tube = Tube(
    (__.json update (merge(defaults) andThen readDateOpt('tv))) andThen Json.reads[RawMetadata],
    Json.writes[RawMetadata] andThen (__.json update writeDateOpt('tv))
  )
}

case class PgnImport(
  user: Option[String],
  date: Option[String],
  pgn: String)

object PgnImport {

  import reactivemongo.bson.Macros
  implicit val pgnImportBSONHandler = Macros.handler[PgnImport]

  import lila.db.Tube
  import play.api.libs.json._

  private[game] lazy val tube = Tube(Json.reads[PgnImport], Json.writes[PgnImport])
}
