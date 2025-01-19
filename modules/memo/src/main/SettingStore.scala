package lila.memo

import scala.util.Try
import scala.util.matching.Regex

import play.api.data.Forms._
import play.api.data._

import reactivemongo.api.bson.BSONHandler

import lila.common
import lila.db.dsl._

final class SettingStore[A: BSONHandler: SettingStore.StringReader: SettingStore.Formable] private (
    coll: Coll,
    val id: String,
    val default: A,
    val text: Option[String],
    persist: Boolean,
    init: SettingStore.Init[A],
)(implicit ec: scala.concurrent.ExecutionContext) {

  import SettingStore.ConfigValue
  import SettingStore.DbValue
  import SettingStore.dbField

  private var value: A = default

  def get(): A = value

  def set(v: A): Funit = {
    value = v
    persist ?? coll.update.one(dbId, $set(dbField -> v), upsert = true).void
  }

  def form: Form[_] = implicitly[SettingStore.Formable[A]] form value

  def setString(str: String): Funit = (implicitly[SettingStore.StringReader[A]] read str) ?? set

  private val dbId = $id(id)

  persist ?? coll.primitiveOne[A](dbId, dbField) map2 { (v: A) =>
    value = init(ConfigValue(default), DbValue(v))
  }
}

object SettingStore {

  case class ConfigValue[A](value: A)
  case class DbValue[A](value: A)

  type Init[A] = (ConfigValue[A], DbValue[A]) => A

  final class Builder(db: lila.db.Db, config: MemoConfig)(implicit
      ec: scala.concurrent.ExecutionContext,
  ) {
    val coll = db(config.configColl)
    def apply[A: BSONHandler: StringReader: Formable](
        id: String,
        default: A,
        text: Option[String] = None,
        persist: Boolean = true,
        init: Init[A] = (_: ConfigValue[A], db: DbValue[A]) => db.value,
    ) = new SettingStore[A](coll, id, default, text, persist = persist, init = init)
  }

  final class StringReader[A](val read: String => Option[A])

  object StringReader {
    implicit val booleanReader: StringReader[Boolean] = new StringReader[Boolean](v =>
      v match {
        case "on" | "yes" | "true" | "1"  => true.some
        case "off" | "no" | "false" | "0" => false.some
        case _                            => none
      },
    )
    implicit val intReader: StringReader[Int]       = new StringReader[Int](_.toIntOption)
    implicit val stringReader: StringReader[String] = new StringReader[String](some)
    def fromIso[A](iso: lila.common.Iso[String, A]) = new StringReader[A](v => iso.from(v).some)
  }

  object Strings {
    val stringsIso = lila.common.Iso.strings(",")
    implicit val stringsBsonHandler: BSONHandler[common.Strings] =
      lila.db.dsl.isoHandler(stringsIso)
    implicit val stringsReader: StringReader[common.Strings] = StringReader.fromIso(stringsIso)
  }
  object Regex {
    val regexIso = lila.common.Iso.string[Regex](_.r, _.toString)
    implicit val regexBsonHandler: BSONHandler[Regex] = lila.db.dsl.isoHandler(regexIso)
    implicit val regexReader: StringReader[Regex]     = StringReader.fromIso(regexIso)
  }

  final class Formable[A](val form: A => Form[_])
  object Formable {
    implicit val regexFormable: Formable[Regex] = new Formable[Regex](v =>
      Form(
        single(
          "v" -> text.verifying(t => Try(t.r).isSuccess),
        ),
      ) fill v.toString,
    )
    implicit val booleanFormable: Formable[Boolean] =
      new Formable[Boolean](v => Form(single("v" -> boolean)) fill v)
    implicit val intFormable: Formable[Int] =
      new Formable[Int](v => Form(single("v" -> number)) fill v)
    implicit val stringFormable: Formable[String] =
      new Formable[String](v => Form(single("v" -> text)) fill v)
    implicit val stringsFormable: Formable[common.Strings] = new Formable[lila.common.Strings](v =>
      Form(single("v" -> text)) fill Strings.stringsIso.to(v),
    )
  }

  private val dbField = "setting"
}
