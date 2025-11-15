package lila.common

import scalalib.data.LazyFu

export lila.core.lilaism.Lilaism.{ *, given }

object extensions:
  export Chronometer.futureExtension.*
  // replaces Product.unapply in play forms
  def unapply[P <: Product](p: P)(using m: scala.deriving.Mirror.ProductOf[P]): Option[m.MirroredElemTypes] =
    Some(Tuple.fromProductTyped(p))

  import scalalib.model.LangTag
  import play.api.i18n.Lang
  extension (l: LangTag) def toLang: Option[Lang] = Lang.get(l.value)
  extension (l: Lang) def toTag: LangTag = LangTag(l.code)

export extensions.*

case class CliCommand(args: List[String], promise: Promise[LazyFu[String]])

object Cli:
  def handle(f: PartialFunction[List[String], Fu[String]]) =
    Bus.sub[CliCommand]:
      case c if f.isDefinedAt(c.args) => c.promise.success(LazyFu(() => f(c.args)))
