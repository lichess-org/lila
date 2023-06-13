package lila.memo

import scala.util.matching.Regex
import scala.util.Try
import reactivemongo.api.bson.BSONHandler

import lila.db.dsl.*
import play.api.data.*, Forms.*
import lila.common.{ Ints, Iso, Strings, UserIds }

final class SettingStore[A: BSONHandler: SettingStore.StringReader: SettingStore.Formable] private (
    coll: Coll,
    val id: String,
    val default: A,
    val text: Option[String],
    persist: Boolean,
    init: SettingStore.Init[A],
    onSet: A => Funit
)(using Executor):

  import SettingStore.{ dbField, ConfigValue, DbValue }

  private var value: A = default

  def get(): A = value

  def set(v: A): Funit = {
    value = v
    persist so coll.update.one(dbId, $set(dbField -> v), upsert = true).void
  } >> onSet(v)

  def form: Form[?] = summon[SettingStore.Formable[A]] form value

  def setString(str: String): Funit = (summon[SettingStore.StringReader[A]] read str) so set

  private val dbId = $id(id)

  persist so coll.primitiveOne[A](dbId, dbField) map2 { (v: A) =>
    value = init(ConfigValue(default), DbValue(v))
  }

object SettingStore:

  case class ConfigValue[A](value: A)
  case class DbValue[A](value: A)

  type Init[A] = (ConfigValue[A], DbValue[A]) => A

  final class Builder(db: lila.db.Db, config: MemoConfig)(using Executor):
    val coll = db(config.configColl)
    def apply[A: BSONHandler: StringReader: Formable](
        id: String,
        default: A,
        text: Option[String] = None,
        persist: Boolean = true,
        init: Init[A] = (_: ConfigValue[A], db: DbValue[A]) => db.value,
        onSet: A => Funit = (_: A) => funit
    ) = SettingStore[A](coll, id, default, text, persist = persist, init = init, onSet = onSet)

  final class StringReader[A](val read: String => Option[A])

  object StringReader:
    given StringReader[Boolean] = StringReader[Boolean]({
      case "on" | "yes" | "true" | "1"  => true.some
      case "off" | "no" | "false" | "0" => false.some
      case _                            => none
    })
    given StringReader[Int]                     = StringReader[Int](_.toIntOption)
    given StringReader[Float]                   = StringReader[Float](_.toFloatOption)
    given StringReader[String]                  = StringReader[String](some)
    def fromIso[A](using iso: Iso.StringIso[A]) = StringReader[A](v => iso.from(v).some)

  object Strings:
    val stringsIso              = Iso.strings(",")
    given BSONHandler[Strings]  = lila.db.dsl.isoHandler(using stringsIso)
    given StringReader[Strings] = StringReader.fromIso(using stringsIso)
  object UserIds:
    val userIdsIso              = Iso.userIds(",")
    given BSONHandler[UserIds]  = lila.db.dsl.isoHandler(using userIdsIso)
    given StringReader[UserIds] = StringReader.fromIso(using userIdsIso)
  object Ints:
    val intsIso              = Iso.ints(",")
    given BSONHandler[Ints]  = lila.db.dsl.isoHandler(using intsIso)
    given StringReader[Ints] = StringReader.fromIso(using intsIso)
  object Regex:
    val regexIso              = Iso.string[Regex](_.r, _.toString)
    given BSONHandler[Regex]  = lila.db.dsl.isoHandler(using regexIso)
    given StringReader[Regex] = StringReader.fromIso(using regexIso)

  final class Formable[A](val form: A => Form[?])
  object Formable:
    given Formable[Regex] =
      Formable[Regex](v => Form(single("v" -> text.verifying(t => Try(t.r).isSuccess))) fill v.toString)
    given Formable[Boolean] = Formable[Boolean](v => Form(single("v" -> boolean)) fill v)
    given Formable[Int]     = Formable[Int](v => Form(single("v" -> number)) fill v)
    given Formable[Float]   = Formable[Float](v => Form(single("v" -> bigDecimal)) fill BigDecimal(v))
    given Formable[String]  = Formable[String](v => Form(single("v" -> text)) fill v)
    given Formable[Strings] = Formable[Strings](v => Form(single("v" -> text)) fill Strings.stringsIso.to(v))
    given Formable[UserIds] = Formable[UserIds](v => Form(single("v" -> text)) fill UserIds.userIdsIso.to(v))

  private val dbField = "setting"
