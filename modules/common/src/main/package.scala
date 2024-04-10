package lila.common

export lila.core.lilaism.Lilaism.{ *, given }

object extensions:
  export Chronometer.futureExtension.*
  // replaces Product.unapply in play forms
  def unapply[P <: Product](p: P)(using m: scala.deriving.Mirror.ProductOf[P]): Option[m.MirroredElemTypes] =
    Some(Tuple.fromProductTyped(p))

export extensions.*
