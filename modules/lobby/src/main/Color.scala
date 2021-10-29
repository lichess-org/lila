package lila.lobby

import lila.common.ThreadLocalRandom.nextBoolean

sealed abstract class Color(val name: String) {

  def resolve: shogi.Color

  def unary_! : Color

  def compatibleWith(c: Color) = !c == this
}

object Color {

  object Sente extends Color("sente") {

    def resolve = shogi.Sente

    def unary_! = Gote
  }

  object Gote extends Color("gote") {

    def resolve = shogi.Gote

    def unary_! = Sente
  }

  object Random extends Color("random") {

    def resolve = shogi.Color.fromSente(nextBoolean())

    def unary_! = this
  }

  def apply(name: String): Option[Color] = all find (_.name == name)

  def orDefault(name: String) = apply(name) | default

  def orDefault(name: Option[String]) = name.flatMap(apply) | default

  val all = List(Sente, Gote, Random)

  val names = all map (_.name)

  val choices = names zip names

  val default = Random
}
