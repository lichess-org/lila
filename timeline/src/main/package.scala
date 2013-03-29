package lila

package object timeline extends PackageObject with WithPlay {

  import lila.db.Tube
  import play.api.libs.json._

  private[timeline] lazy val entryTube = Tube(
    Json.reads[Entry], 
    Json.writes[Entry]
  ) inColl Env.current.entryColl
}
