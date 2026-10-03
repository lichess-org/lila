package lila.mod

import play.api.data.Form
import play.api.data.Forms.*
import reactivemongo.api.bson.BSONHandler
import scalalib.Iso

import lila.core.perm.{ Granter, Permission }
import lila.core.misc.{ AppealTopic, AppealPresetTag }
import lila.memo.SettingStore
import lila.memo.SettingStore.{ Formable, StringReader }

final class ModPresetsApi(settingStore: lila.memo.SettingStore.Builder):

  import ModPresets.setting.given

  def getPmPresets(using Me): PmPresets =
    ModPresets(pmPresets.get().value.filter(_.tags.exists(Granter(_))))

  def permissionsByName(name: String): Set[Permission] =
    pmPresets.get().value.find(_.name == name).so(_.tags)

  lazy val pmPresets = settingStore[ModPresets[Permission]](
    "modPmPresets",
    default = ModPresets(Nil),
    text = "Moderator PM presets".some
  )

  lazy val appealPresets = settingStore[ModPresets[AppealPresetTag]](
    "modAppealPresets",
    default = ModPresets(Nil),
    text = "Moderator appeal presets".some
  )

  private def filterAppealPresets(topic: AppealTopic): AppealPresets =
    val all = appealPresets.get()
    if topic == AppealTopic.legacy then all
    else
      ModPresets:
        all.value
          .filter: p =>
            p.tags.contains(topic) || p.tags.contains("any")
          .sortBy(_.tags.contains("any"))

  def asPairsFor(topic: AppealTopic): List[PairOf[String]] =
    filterAppealPresets(topic).value.map(p => p.name -> p.text)

  def get(group: ModPresets.Group): SettingStore[ModPresets[group.Tag]] = group.get(this)

  def setKidModePreset: Option[PmPreset] =
    pmPresets.get().value.find(_.name == "Account set to kid mode")

type PmPreset = ModPreset[Permission]
type PmPresets = ModPresets[Permission]
type AppealPreset = ModPreset[AppealPresetTag]
type AppealPresets = ModPresets[AppealPresetTag]

case class ModPresets[T](value: List[ModPreset[T]]):
  def named(name: String) = value.find(_.name == name)
  def byTags: Map[T, List[ModPreset[T]]] =
    value.flatMap(v => v.tags.map(_ -> v)).groupBy(_._1).view.mapValues(_._2F).toMap

case class ModPreset[T](name: String, text: String, tags: Set[T]):
  def isNameClose = name.contains(ModPresets.nameClosePresetName)
  override def toString = name

object ModPresets:

  sealed trait Group:
    type Tag
    def key: String
    def get(api: ModPresetsApi): SettingStore[ModPresets[Tag]]
  object Group:
    case object PM extends Group:
      type Tag = Permission
      val key = "PM"
      def get(api: ModPresetsApi) = api.pmPresets
    case object appeal extends Group:
      type Tag = AppealPresetTag
      val key = "appeal"
      def get(api: ModPresetsApi) = api.appealPresets
    val values = List(PM, appeal)
    val byKey = values.mapBy(_.key)

  val nameClosePresetName = "Account closure for name in 48h"

  private[mod] object setting:

    private def write[T](presets: ModPresets[T])(using tagStr: TagString[T]): String =
      presets.value
        .map { case ModPreset(name, text, tags) =>
          s"${tags.map(tagStr.write).mkString(", ")}\n\n$name\n\n$text"
        }
        .mkString("\n\n----------\n\n")

    private def read[T](s: String)(using tagStr: TagString[T]): ModPresets[T] =
      ModPresets:
        "\n-{3,}\\s*\n".r
          .split(s.linesIterator.map(_.trim).dropWhile(_.isEmpty).mkString("\n"))
          .toList
          .map(_.linesIterator.toList)
          .filter(_.nonEmpty)
          .flatMap:
            case tagsLine :: rest =>
              val cleanRest = rest.dropWhile(_.isEmpty)
              for
                name <- cleanRest.headOption
                text = cleanRest.tail.dropWhile(_.isEmpty).mkString("\n")
                tags = tagsLine.split(",").map(_.trim).toList.flatMap(tagStr.read)
              yield ModPreset[T](name, text, tags.toSet)
            case _ => none

    given [T: TagString]: Iso.StringIso[ModPresets[T]] = Iso.string(read, write)
    given [T: TagString]: BSONHandler[ModPresets[T]] = lila.db.dsl.isoHandler
    given [T: TagString]: StringReader[ModPresets[T]] = StringReader.fromIso
    given [T: TagString]: Formable[ModPresets[T]] =
      Formable(presets => Form(single("v" -> text)).fill(write(presets)))

private trait TagString[T]:
  def read(s: String): Option[T]
  def write(t: T): String

private given TagString[Permission] with
  def read(s: String) = Permission.allByKeyLower.get(s.toLowerCase)
  def write(p: Permission) = p.key.toLowerCase

private given TagString[AppealPresetTag] with
  def read(s: String) = AppealTopic.byKey
    .get(s.toLowerCase)
    .orElse:
      if s == "any" then "any".some
      else if s == "none" then "none".some
      else none
  def write(a: AppealPresetTag) = a.toString
