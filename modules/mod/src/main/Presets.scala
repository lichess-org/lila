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

case class ModPresets(value: List[ModPreset]) {

  def named(name: String) = value.find(_.name == name)

  def findLike(text: String) = {
    val clean = text.filter(_.isLetter)
    value.find(_.text.filter(_.isLetter) == clean)
  }
}
case class ModPreset(name: String, text: String)

object ModPresets {

  val groups = List("PM", "appeal")

  private[mod] object setting {

    private def write(presets: ModPresets): String =
      presets.value.map { case ModPreset(name, text) =>
        s"$name\n\n$text"
      } mkString "\n\n----------\n\n"

    private def read(s: String): ModPresets =
      ModPresets {
        "\n-{3,}\\s*\n".r
          .split(s.linesIterator.map(_.trim).dropWhile(_.isEmpty) mkString "\n")
          .toList
          .map(_.linesIterator.toList)
          .filter(_.nonEmpty)
          .flatMap {
            case name :: text => ModPreset(name, text.dropWhile(_.isEmpty) mkString "\n").some
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
