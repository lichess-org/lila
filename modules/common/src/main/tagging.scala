package lila.common

/**
 * Tag instances with arbitrary types. The tags are usually empty `trait`s. Tags have no runtime overhead and are only
 * used at compile-time for additional type safety.
 *
 * For example:
 *
 * {{{
 *   class Berry()
 *
 *   trait Black
 *   trait Blue
 *
 *   val berry = new Berry()
 *   val blackBerry: Berry @@ Black = berry.taggedWith[Black]
 *   val blueBerry: Berry @@ Blue = berry.taggedWith[Blue]
 *
 *   // compile error: val anotherBlackBerry: Berry @@ Black = blueBerry
 * }}}
 *
 * Original idea by Miles Sabin, see: https://gist.github.com/milessabin/89c9b47a91017973a35f
 */
package object tagging {
  trait Tag[+U] extends Any { type Tag <: U }
  type @@[+T, +U] = T with Tag[U]
  type Tagged[+T, +U] = T with Tag[U]
  implicit class Tagger[T](val t: T) extends AnyVal {
    def taggedWith[U]: T @@ U = t.asInstanceOf[T @@ U]
  }
  implicit class AndTagger[T, U](val t: T @@ U) extends AnyVal {
    def andTaggedWith[V]: T @@ (U with V) = t.asInstanceOf[T @@ (U with V)]
  }
}
