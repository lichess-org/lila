package lila.core.lilaism

object Lilaism
    extends cats.syntax.OptionSyntax
    with cats.syntax.ListSyntax
    with LilaTypes
    with LilaModel
    with LilaUserId
    with LilaLibraryExtensions:

  export scalalib.newtypes.{ given, * }
  export scalalib.zeros.given
  export scalalib.extensions.{ given, * }
  export scalalib.json.extensions.*
  export scalalib.json.Json.given_Zero_JsObject
  export scalalib.time.*

  export cats.syntax.all.*
  export cats.{ Eq, Show }
  export cats.data.NonEmptyList

  def some[A](a: A): Option[A] = Some(a)

  trait StringValue extends Any:
    def value: String
    override def toString = value
  given cats.Show[StringValue] = cats.Show.show(_.value)

  // move somewhere else when we have more Eqs
  given cats.Eq[play.api.i18n.Lang] = cats.Eq.fromUniversalEquals

  import play.api.Mode
  extension (mode: Mode)
    def isDev   = mode == Mode.Dev
    def isProd  = mode == Mode.Prod
    def notProd = mode != Mode.Prod
