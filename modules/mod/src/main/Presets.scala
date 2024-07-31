package lila.mod

import play.api.data.Form
import play.api.data.Forms.*
import reactivemongo.api.bson.BSONHandler
import scalalib.Iso

import lila.core.perm.{ Granter, Permission }
import lila.memo.SettingStore.{ Formable, StringReader }

final class ModPresetsApi(settingStore: lila.memo.SettingStore.Builder):

  import ModPresets.setting.given

  def get(group: String) = group match
    case "PM"     => pmPresets.some
    case "appeal" => appealPresets.some
    case _        => none

  def getPmPresets(using Me): ModPresets =
    ModPresets(pmPresets.get().value.filter(_.permissions.exists(Granter(_))))

  def getPmPresetsOpt(using mod: Option[Me]): ModPresets =
    mod.map(getPmPresets(using _)).getOrElse(ModPresets(Nil))

  def permissionsByName(name: String): Set[Permission] =
    pmPresets.get().value.find(_.name == name).so(_.permissions)

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

case class ModPresets(value: List[ModPreset]):
  def named(name: String) = value.find(_.name == name)
  def findLike(text: String) =
    val clean = text.filter(_.isLetter)
    value.find(_.text.filter(_.isLetter) == clean)

case class ModPreset(name: String, text: String, permissions: Set[Permission]):
  def isNameClose = name.contains(ModPresets.nameClosePresetName)

object ModPresets:

  val groups              = List("PM", "appeal")
  val nameClosePresetName = "Account closure for name in 48h"

  private[mod] object setting:

    private def write(presets: ModPresets): String =
      presets.value
        .map { case ModPreset(name, text, permissions) =>
          s"${permissions.map(_.key).mkString(", ")}\n\n$name\n\n$text"
        }
        .mkString("\n\n----------\n\n")

    private def read(s: String): ModPresets =
      ModPresets:
        "\n-{3,}\\s*\n".r
          .split(s.linesIterator.map(_.trim).dropWhile(_.isEmpty).mkString("\n"))
          .toList
          .map(_.linesIterator.toList)
          .filter(_.nonEmpty)
          .flatMap {
            case perms :: rest =>
              val cleanRest = rest.dropWhile(_.isEmpty)
              for
                name <- cleanRest.headOption
                text = cleanRest.tail
              yield ModPreset(
                name,
                text.dropWhile(_.isEmpty).mkString("\n"),
                toPermisssions(perms)
              )
            case _ => none
          }

    private def toPermisssions(s: String): Set[Permission] =
      Permission.ofDbKeys(s.split(",").map(key => s"ROLE_${key.trim.toUpperCase}").toList) match
        case set if set.nonEmpty => set
        case _                   => Set(Permission.Admin)

    given Iso.StringIso[ModPresets] = Iso.string(read, write)
    given BSONHandler[ModPresets]   = lila.db.dsl.isoHandler
    given StringReader[ModPresets]  = StringReader.fromIso
    given Formable[ModPresets]      = Formable(presets => Form(single("v" -> text)).fill(write(presets)))
