package lila.lobby

import lila.common.ThreadLocalRandom.nextBoolean

sealed abstract class Color(val name: String) {

  def resolve: chess.Color

  def unary_! : Color

  def compatibleWith(c: Color) = !c == this
}

object Color {

  object White extends Color("white") {

    def resolve = chess.White

    def unary_! = Black
  }

  object Black extends Color("black") {

    def resolve = chess.Black

    def unary_! = White
  }

  object Random extends Color("random") {

    def resolve = chess.Color(nextBoolean())

    def unary_! = this
  }

  def apply(name: String): Option[Color] = all find (_.name == name)

  def orDefault(name: String) = apply(name) | default

  def orDefault(name: Option[String]) = name.flatMap(apply) | default

  val all = List(White, Black, Random)

  val names = all map (_.name)

  val choices = names zip names

  val default = Random
}
