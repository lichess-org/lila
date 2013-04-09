package lila

package object notification extends PackageObject with WithPlay {

  object actorApi {

    case class RenderNotification(id: String, from: Option[String], body: String)
  }
}
