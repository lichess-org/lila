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

  import lila.db.Tube
  import play.api.libs.json._

  private[game] lazy val tube = Tube(Json.reads[PgnImport], Json.writes[PgnImport])
}
