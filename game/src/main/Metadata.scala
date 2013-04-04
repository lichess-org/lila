package lila.game

private[game] case class Metadata(
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

private[game] case class RawMetadata(
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

private[game] object RawMetadata {

  import lila.db.Tube
  import Tube.Helpers._
  import play.api.libs.json._

  private implicit def importTube = PgnImport.tube

  private def defaults = Json.obj("t" -> none[PgnImport])

  lazy val tube = Tube(
    reader = (__.json update merge(defaults)) andThen Json.reads[RawMetadata],
    writer = Json.writes[RawMetadata])
}

private[game] case class PgnImport(
  user: Option[String], 
  date: Option[String], 
  pgn: String)

private[game] object PgnImport {

  import lila.db.Tube
  import play.api.libs.json._

  lazy val tube = Tube(Json.reads[PgnImport], Json.writes[PgnImport])
}
