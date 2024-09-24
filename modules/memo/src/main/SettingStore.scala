package lila.memo

import play.api.data.*
import reactivemongo.api.bson.BSONHandler
import scalalib.Iso

import scala.util.Try
import scala.util.matching.Regex

import lila.core.data.{ Ints, Strings, UserIds }
import lila.db.dsl.*

import Forms.*

final class SettingStore[A: BSONHandler: SettingStore.StringReader: SettingStore.Formable] private (
    coll: Coll,
    val id: String,
    val default: A,
    val text: Option[String],
    init: SettingStore.Init[A],
    onSet: A => Funit
)(using Executor):

  import SettingStore.{ dbField, ConfigValue, DbValue }

  private var value: A = default

  def get(): A = value

  def set(v: A): Funit = {
    value = v
    coll.update.one(dbId, $set(dbField -> v), upsert = true).void
  } >> onSet(v)

  def form: Form[?] = summon[SettingStore.Formable[A]].form(value)

  def setString(str: String): Funit = (summon[SettingStore.StringReader[A]].read(str)).so(set)

  private val dbId = $id(id)

  coll.primitiveOne[A](dbId, dbField).map2 { (v: A) =>
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
        init: Init[A] = (_: ConfigValue[A], db: DbValue[A]) => db.value,
        onSet: A => Funit = (_: A) => funit
    ) = SettingStore[A](coll, id, default, text, init = init, onSet = onSet)

  final class StringReader[A](val read: String => Option[A])

  object StringReader:
    given StringReader[Boolean] = StringReader[Boolean]:
      case "on" | "yes" | "true" | "1"  => true.some
      case "off" | "no" | "false" | "0" => false.some
      case _                            => none
    given StringReader[Int]                     = StringReader[Int](_.toIntOption)
    given StringReader[Float]                   = StringReader[Float](_.toFloatOption)
    given StringReader[String]                  = StringReader[String](some)
    def fromIso[A](using iso: Iso.StringIso[A]) = StringReader[A](v => iso.from(v).some)

  private type CredOption = Option[lila.core.config.Credentials]
  private type HostOption = Option[lila.core.config.HostPort]

  object Strings:
    val stringsIso              = lila.common.Iso.strings(",")
    given BSONHandler[Strings]  = lila.db.dsl.isoHandler(using stringsIso)
    given StringReader[Strings] = StringReader.fromIso(using stringsIso)
  object UserIds:
    val userIdsIso = Strings.stringsIso.map[lila.core.data.UserIds](
      strs => lila.core.data.UserIds(UserStr.from(strs.value).map(_.id)),
      uids => lila.core.data.Strings(UserId.raw(uids.value))
    )
    given BSONHandler[UserIds]  = lila.db.dsl.isoHandler(using userIdsIso)
    given StringReader[UserIds] = StringReader.fromIso(using userIdsIso)
  object Ints:
    val intsIso              = lila.common.Iso.ints(",")
    given BSONHandler[Ints]  = lila.db.dsl.isoHandler(using intsIso)
    given StringReader[Ints] = StringReader.fromIso(using intsIso)
  object Regex:
    val regexIso              = Iso.string[Regex](_.r, _.toString)
    given BSONHandler[Regex]  = lila.db.dsl.isoHandler(using regexIso)
    given StringReader[Regex] = StringReader.fromIso(using regexIso)
  object CredentialsOption:
    val credentialsIso             = Iso.string[CredOption](lila.core.config.Credentials.read, _.so(_.show))
    given BSONHandler[CredOption]  = lila.db.dsl.isoHandler(using credentialsIso)
    given StringReader[CredOption] = StringReader.fromIso(using credentialsIso)
  object HostPortOption:
    val hostPortIso                = Iso.string[HostOption](lila.core.config.HostPort.read, _.so(_.show))
    given BSONHandler[HostOption]  = lila.db.dsl.isoHandler(using hostPortIso)
    given StringReader[HostOption] = StringReader.fromIso(using hostPortIso)

  final class Formable[A](val form: A => Form[?])
  object Formable:
    given Formable[Regex] =
      Formable[Regex](v => Form(single("v" -> text.verifying(t => Try(t.r).isSuccess))).fill(v.toString))
    given Formable[Boolean] = Formable[Boolean](v => Form(single("v" -> boolean)).fill(v))
    given Formable[Int]     = Formable[Int](v => Form(single("v" -> number)).fill(v))
    given Formable[Float]   = Formable[Float](v => Form(single("v" -> bigDecimal)).fill(BigDecimal(v)))
    given Formable[String]  = Formable[String](v => Form(single("v" -> text)).fill(v))
    given Formable[Strings] = Formable[Strings](v => Form(single("v" -> text)).fill(Strings.stringsIso.to(v)))
    given Formable[UserIds] = Formable[UserIds](v => Form(single("v" -> text)).fill(UserIds.userIdsIso.to(v)))
    given Formable[CredOption] = stringPair(using CredentialsOption.credentialsIso)
    given Formable[HostOption] = stringPair(using HostPortOption.hostPortIso)
    private def stringPair[A](using iso: Iso.StringIso[A]): Formable[A] = Formable[A]: v =>
      Form(
        single("v" -> text.verifying(t => t.isEmpty || t.count(_ == ':') == 1))
      ).fill(iso.to(v))

  private val dbField = "setting"
