package lila.game

case class Metadata(
    source: Source,
    pgnImport: Option[PgnImport] = None,
    tournamentId: Option[String] = None) {

  def encode = RawMetadata(
    so = source.id,
    pgni = pgnImport,
    tid = tournamentId)

  def pgnDate = pgnImport flatMap (_.date)

  def pgnUser = pgnImport flatMap (_.user)
}

case class RawMetadata(
    so: Int,
    pgni: Option[PgnImport],
    tid: Option[String]) {

  def decode = Source(so) map { source ⇒
    Metadata(
      source = source,
      pgnImport = pgni,
      tournamentId = tid)
  }
}

object RawMetadatas {

  import lila.db.Tube
  import Tube.Helpers._
  import play.api.libs.json._

  private implicit def importTube = PgnImports.tube

  private val defaults = Json.obj("t" -> none[PgnImport])

  val tube = Tube(
    reader = (__.json update merge(defaults)) andThen Json.reads[RawMetadata],
    writer = Json.writes[RawMetadata])
}

case class PgnImport(user: Option[String], date: Option[String], pgn: String)

object PgnImports {

  import lila.db.Tube
  import play.api.libs.json._

  val tube = Tube(Json.reads[PgnImport], Json.writes[PgnImport])
}
