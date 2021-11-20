package lila.mod

import play.api.data.Form
import play.api.data.Forms._
import scala.concurrent.ExecutionContext

import lila.memo.SettingStore.{ Formable, StringReader }
import lila.user.User
import lila.security.{ Granter, Permission }
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

  def getPmPresets(mod: User): ModPresets =
    ModPresets(pmPresets.get().value.filter(_.permissions.exists(Granter(_)(mod))))

  def getPmPresets(mod: Option[User]): ModPresets =
    mod.map(getPmPresets).getOrElse(ModPresets(Nil))

  private lazy val pmPresets = settingStore[ModPresets](
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
case class ModPreset(name: String, text: String, permissions: Set[Permission]) {

  def isNameClose = name contains ModPresets.nameClosePresetName
}

object ModPresets {

  val groups              = List("PM", "appeal")
  val nameClosePresetName = "Account closure for name in 48h"

  private[mod] object setting {

    private def write(presets: ModPresets): String =
      presets.value.map { case ModPreset(name, text, permissions) =>
        s"${permissions.map(_.key) mkString ", "}\n\n$name\n\n$text"
      } mkString "\n\n----------\n\n"

    private def read(s: String): ModPresets =
      ModPresets {
        "\n-{3,}\\s*\n".r
          .split(s.linesIterator.map(_.trim).dropWhile(_.isEmpty) mkString "\n")
          .toList
          .map(_.linesIterator.toList)
          .filter(_.nonEmpty)
          .flatMap {
            case perms :: rest => {
              val cleanRest = rest.dropWhile(_.isEmpty)
              for {
                name <- cleanRest.headOption
                text = cleanRest.tail
              } yield ModPreset(
                name,
                text.dropWhile(_.isEmpty) mkString "\n",
                toPermisssions(perms)
              )
            }
            case _ => none
          }
      }

    private def toPermisssions(s: String): Set[Permission] =
      Permission(s.split(",").map(key => s"ROLE_${key.trim.toUpperCase}").toList) match {
        case set if set.nonEmpty => set
        case _                   => Set(Permission.Admin)
      }

    private val presetsIso = lila.common.Iso[String, ModPresets](read, write)

    implicit val presetsBsonHandler: BSONHandler[ModPresets]   = lila.db.dsl.isoHandler(presetsIso)
    implicit val presetsStringReader: StringReader[ModPresets] = StringReader.fromIso(presetsIso)
    implicit val presetsFormable: Formable[ModPresets] =
      new Formable[ModPresets](presets => Form(single("v" -> text)) fill write(presets))
  }
}
