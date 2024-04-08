package lila.core
package perf

import cats.derived.*
import lila.core.i18n.Translate
import lila.core.i18n.I18nKey
import chess.Speed

opaque type PerfKey = String
object PerfKey extends OpaqueString[PerfKey]

opaque type PerfId = Int
object PerfId extends OpaqueInt[PerfId]

enum PerfType(
    val id: PerfId,
    val key: PerfKey,
    private val name: String,  // ??
    private val title: String, // ??
    val icon: Icon,
    val nameKey: I18nKey,
    val descKey: I18nKey
) derives Eq:

  def trans(using translate: Translate): String = nameKey.txt()
  def desc(using translate: Translate): String  = descKey.txt()

  case UltraBullet
      extends PerfType(
        PerfId(0),
        key = PerfKey("ultraBullet"),
        name = Speed.UltraBullet.name,
        title = Speed.UltraBullet.title,
        icon = Icon.UltraBullet,
        nameKey = I18nKey(Speed.UltraBullet.name),
        descKey = I18nKey.site.ultraBulletDesc
      )

  case Bullet
      extends PerfType(
        PerfId(1),
        key = PerfKey("bullet"),
        name = Speed.Bullet.name,
        title = Speed.Bullet.title,
        icon = Icon.Bullet,
        nameKey = I18nKey.site.bullet,
        descKey = I18nKey.site.bulletDesc
      )

  case Blitz
      extends PerfType(
        PerfId(2),
        key = PerfKey("blitz"),
        name = Speed.Blitz.name,
        title = Speed.Blitz.title,
        icon = Icon.FlameBlitz,
        nameKey = I18nKey.site.blitz,
        descKey = I18nKey.site.blitzDesc
      )

  case Rapid
      extends PerfType(
        PerfId(6),
        key = PerfKey("rapid"),
        name = Speed.Rapid.name,
        title = Speed.Rapid.title,
        icon = Icon.Rabbit,
        nameKey = I18nKey.site.rapid,
        descKey = I18nKey.site.rapidDesc
      )

  case Classical
      extends PerfType(
        PerfId(3),
        key = PerfKey("classical"),
        name = Speed.Classical.name,
        title = Speed.Classical.title,
        icon = Icon.Turtle,
        nameKey = I18nKey.site.classical,
        descKey = I18nKey.site.classicalDesc
      )

  case Correspondence
      extends PerfType(
        PerfId(4),
        key = PerfKey("correspondence"),
        name = "Correspondence",
        title = Speed.Correspondence.title,
        icon = Icon.PaperAirplane,
        nameKey = I18nKey.site.correspondence,
        descKey = I18nKey.site.correspondenceDesc
      )

  case Standard
      extends PerfType(
        PerfId(5),
        key = PerfKey("standard"),
        name = chess.variant.Standard.name,
        title = "Standard rules of chess",
        icon = Icon.Crown,
        nameKey = I18nKey(chess.variant.Standard.name),
        descKey = I18nKey("Standard rules of chess")
      )

  case Chess960
      extends PerfType(
        PerfId(11),
        key = PerfKey("chess960"),
        name = chess.variant.Chess960.name,
        title = "Chess960 variant",
        icon = Icon.DieSix,
        nameKey = I18nKey(chess.variant.Chess960.name),
        descKey = I18nKey("Chess960 variant")
      )

  case KingOfTheHill
      extends PerfType(
        PerfId(12),
        key = PerfKey("kingOfTheHill"),
        name = chess.variant.KingOfTheHill.name,
        title = "King of the Hill variant",
        icon = Icon.FlagKingHill,
        nameKey = I18nKey(chess.variant.KingOfTheHill.name),
        descKey = I18nKey("King of the Hill variant")
      )

  case Antichess
      extends PerfType(
        PerfId(13),
        key = PerfKey("antichess"),
        name = chess.variant.Antichess.name,
        title = "Antichess variant",
        icon = Icon.Antichess,
        nameKey = I18nKey(chess.variant.Antichess.name),
        descKey = I18nKey("Antichess variant")
      )

  case Atomic
      extends PerfType(
        PerfId(14),
        key = PerfKey("atomic"),
        name = chess.variant.Atomic.name,
        title = "Atomic variant",
        icon = Icon.Atom,
        nameKey = I18nKey(chess.variant.Atomic.name),
        descKey = I18nKey("Atomic variant")
      )

  case ThreeCheck
      extends PerfType(
        PerfId(15),
        key = PerfKey("threeCheck"),
        name = chess.variant.ThreeCheck.name,
        title = "Three-check variant",
        icon = Icon.ThreeCheckStack,
        nameKey = I18nKey(chess.variant.ThreeCheck.name),
        descKey = I18nKey("Three-check variant")
      )

  case Horde
      extends PerfType(
        PerfId(16),
        key = PerfKey("horde"),
        name = chess.variant.Horde.name,
        title = "Horde variant",
        icon = Icon.Keypad,
        nameKey = I18nKey(chess.variant.Horde.name),
        descKey = I18nKey("Horde variant")
      )

  case RacingKings
      extends PerfType(
        PerfId(17),
        key = PerfKey("racingKings"),
        name = chess.variant.RacingKings.name,
        title = "Racing kings variant",
        icon = Icon.FlagRacingKings,
        nameKey = I18nKey(chess.variant.RacingKings.name),
        descKey = I18nKey("Racing kings variant")
      )

  case Crazyhouse
      extends PerfType(
        PerfId(18),
        key = PerfKey("crazyhouse"),
        name = chess.variant.Crazyhouse.name,
        title = "Crazyhouse variant",
        icon = Icon.Crazyhouse,
        nameKey = I18nKey(chess.variant.Crazyhouse.name),
        descKey = I18nKey("Crazyhouse variant")
      )

  case Puzzle
      extends PerfType(
        PerfId(20),
        key = PerfKey("puzzle"),
        name = "Training",
        title = "Chess tactics trainer",
        icon = Icon.ArcheryTarget,
        nameKey = I18nKey.site.puzzles,
        descKey = I18nKey.site.puzzleDesc
      )

object PerfType:
  given Conversion[PerfType, PerfKey] = _.key
  given Conversion[PerfType, PerfId]  = _.id
  val all: List[PerfType]             = values.toList
  val byKey                           = all.mapBy(_.key)
  val byId                            = all.mapBy(_.id)

  def apply(key: PerfKey): Option[PerfType] = byKey.get(key)
  def apply(id: PerfId): Option[PerfType]   = byId.get(id)
