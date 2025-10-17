package lila.core

import scalalib.newtypes.{ OpaqueString, TotalWrapper }

import lila.core.userId.UserId
import lila.core.lilaism.Lilaism.StringValue

// has to be an object, not a package,
// so opaque types don't leak out
object data:

  case class Strings(value: List[String]) extends AnyVal
  case class UserIds(value: List[UserId]) extends AnyVal
  case class Ints(value: List[Int]) extends AnyVal
  case class Text(value: String) extends AnyVal with StringValue

  trait OpaqueInstant[A](using A =:= Instant) extends TotalWrapper[A, Instant]

  opaque type RichText = String
  object RichText extends OpaqueString[RichText]

  opaque type Markdown = String
  object Markdown extends OpaqueString[Markdown]

  opaque type Html = String
  // not an OpaqueString, because we don't want the default Render[Html]
  object Html extends TotalWrapper[Html, String]:
    def apply(frag: scalatags.Text.Frag): Html = frag.render
    extension (a: Html) def frag = scalatags.Text.all.raw(a.value)

  opaque type JsonStr = String
  object JsonStr extends OpaqueString[JsonStr]

  // JSON string that is safe to include in HTML
  opaque type SafeJsonStr = String
  object SafeJsonStr extends OpaqueString[SafeJsonStr]

  opaque type Template = String
  object Template extends OpaqueString[Template]

  opaque type ErrorMsg = String
  object ErrorMsg extends OpaqueString[ErrorMsg]

  final class CircularDep[A](val resolve: () => A)
  final class LazyDep[A](val resolve: () => A)
