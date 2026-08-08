package lila.pref

import play.api.libs.json.*

object PrefJsonChange:

  sealed trait Change:
    def name: String
    def get(pref: Pref): Option[JsValue]
    def set(pref: Pref, value: JsValue): JsResult[Pref]

  private final case class Field[A: Reads: Writes](
      name: String,
      read: Pref => Option[A],
      write: (Pref, Option[A]) => Pref
  ) extends Change:
    def get(pref: Pref): Option[JsValue] = read(pref).map(Json.toJson(_))
    def set(pref: Pref, value: JsValue): JsResult[Pref] = value.pp match
      case JsNull => JsSuccess(write(pref, none))
      case json => json.validate[A].map(v => write(pref, v.some))

  private def optional[A: Reads: Writes](name: String)(read: Pref => Option[A])(
      write: (Pref, Option[A]) => Pref
  ): Change =
    Field(name, read, write)

  val changes: Map[String, Change] = List(
    optional[JsValue]("lobbyShortcuts")(_.lobbyShortcuts): (pref, shortcuts) =>
      pref.copy(lobbyShortcuts = shortcuts)
  ).map: change =>
    change.name -> change
  .toMap
