package lila.memo

import lila.db.dsl._
import play.api.data._, Forms._

final class SettingStore[A: BSONValueHandler: SettingStore.StringReader] private (
    coll: Coll,
    val id: String,
    val default: A,
    val text: Option[String],
    persist: Boolean,
    init: SettingStore.Init[A]
) {

  import SettingStore.{ ConfigValue, DbValue, dbField }

  private var value: A = default

  def get(): A = value

  def set(v: A): Funit = {
    value = v
    persist ?? coll.update(dbId, $set(dbField -> v), upsert = true).void
  }

  def form: Option[Form[_]] = SettingStore formOf this

  def setString(str: String): Funit = (implicitly[SettingStore.StringReader[A]] read str) ?? set

  override def toString = s"SettingStore(id: $id, default: $default, value: $value, persist: $persist)"

  private val dbId = $id(id)

  persist ?? coll.primitiveOne[A](dbId, dbField) map2 { (v: A) =>
    value = init(ConfigValue(default), DbValue(v))
  }
}

object SettingStore {

  case class ConfigValue[A](value: A)
  case class DbValue[A](value: A)

  type Init[A] = (ConfigValue[A], DbValue[A]) => A

  final class Builder(coll: Coll) {
    def apply[A: BSONValueHandler: StringReader](
      id: String,
      default: A,
      text: Option[String] = None,
      persist: Boolean = true,
      init: Init[A] = (config: ConfigValue[A], db: DbValue[A]) => db.value
    ) = new SettingStore[A](coll, id, default, text, persist = persist, init = init)
  }

  final class StringReader[A](val read: String => Option[A])

  object StringReader {
    implicit val booleanReader = new StringReader[Boolean](v =>
      if (Set("on", "yes", "true", "1")(v)) true.some
      else if (Set("off", "no", "false", "0")(v)) false.some
      else none)
    implicit val intReader = new StringReader[Int](parseIntOption)
    implicit val stringReader = new StringReader[String](some)
    def fromIso[A](iso: lila.common.Iso[String, A]) = new StringReader[A](v => iso.from(v).some)
  }

  def formOf(s: SettingStore[_]): Option[Form[_]] = s.value match {
    case v: Boolean => Form(single("v" -> boolean)) fill v some
    case v: Int => Form(single("v" -> number)) fill v some
    case v: String => Form(single("v" -> text)) fill v some
    case _ => none
  }

  private val dbField = "setting"
}
