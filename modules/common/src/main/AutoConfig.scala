package io.methvin.play.autoconfig.*

import scala.deriving.*
import scala.compiletime.{ erasedValue, summonInline }
import play.api.{ Configuration, ConfigLoader }
import com.typesafe.config.Config

object AutoConfig:

  inline def summonAll[T <: Tuple]: List[ConfigLoader[_]] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (t *: ts)  => summonInline[ConfigLoader[t]] :: summonAll[ts]

  def iterator[T](p: T) = p.asInstanceOf[Product].productIterator

  inline given derived[T](using m: Mirror.Of[T]): ConfigLoader[T] =
    inline m match
      case p: Mirror.ProductOf[T] =>
        val elemInstances: List[ConfigLoader[?]] = summonAll[p.MirroredElemTypes]
        new ConfigLoader[T]:
          def load(config: Config, path: String = ""): T = p.fromTuple {
            elemInstances
              .foldLeft[Tuple](EmptyTuple) { (tuple, elem) => elem *: tuple }
              .asInstanceOf[p.MirroredElemTypes]
          }

  case class Yay(foo: Int, bar: String)
  val loader = summon[ConfigLoader[Yay]]
  // AutoConfig.loader[Yay](using ConfigLoader[Yay])
