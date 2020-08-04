package lila.mod

import play.api.data.Form
import play.api.data.Forms._
import scala.concurrent.ExecutionContext

import lila.memo.SettingStore.{ Formable, StringReader }
import reactivemongo.api.bson.BSONHandler

final class ModPresetsApi(
    settingStore: lila.memo.SettingStore.Builder
)(implicit ec: ExecutionContext) {

  import ModPresets.setting._

  val groups = List("PM", "appeal")

  def get(group: String) =
    group match {
      case "PM"     => pmPresets.some
      case "appeal" => appealPresets.some
      case _        => none
    }

  lazy val pmPresets = settingStore[ModPresets](
    "modPmPresets",
    default = ModPresets(Nil),
    text = "Moderator PM presets".some
  )

  lazy val appealPresets = settingStore[ModPresets](
    "modAppealPresets",
    default = ModPresets(Nil),
    text = "Moderator appeal presets".some
  )
}

case class ModPresets(value: List[ModPreset])
case class ModPreset(name: String, text: String)

private object ModPresets {

  object setting {

    private def write(presets: ModPresets): String =
      presets.value.map {
        case ModPreset(name, text) => s"$name\n$text"
      } mkString "\n----------\n"

    private def read(s: String): ModPresets =
      ModPresets {
        "\n-{3,}\n"
          .split(s)
          .toList
          .map(_.linesIterator.map(_.trim).filter(_.nonEmpty).toList)
          .filter(_.nonEmpty)
          .flatMap {
            case name :: text => ModPreset(name, text mkString "\n").some
            case _            => none
          }
      }

    private val presetsIso = lila.common.Iso[String, ModPresets](read, write)

    implicit val presetsBsonHandler: BSONHandler[ModPresets]   = lila.db.dsl.isoHandler(presetsIso)
    implicit val presetsStringReader: StringReader[ModPresets] = StringReader.fromIso(presetsIso)
    implicit val presetsFormable: Formable[ModPresets] =
      new Formable[ModPresets](presets => Form(single("v" -> text)) fill write(presets))
  }
}
