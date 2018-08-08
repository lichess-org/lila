package lidraughts.lobby

import scala.util.Random.nextBoolean

sealed abstract class Color(val name: String) {

  def resolve: draughts.Color

  def unary_! : Color

  def compatibleWith(c: Color) = !c == this
}

object Color {

  object White extends Color("white") {

    def resolve = draughts.White

    def unary_! = Black
  }

  object Black extends Color("black") {

    def resolve = draughts.Black

    def unary_! = White
  }

  object Random extends Color("random") {

    def resolve = nextBoolean.fold(White, Black).resolve

    def unary_! = this
  }

  def apply(name: String): Option[Color] = all find (_.name == name)

  def orDefault(name: String) = apply(name) | default

  val all = List(White, Black, Random)

  def random = all(scala.util.Random.nextInt(all.size))

  val names = all map (_.name)

  val choices = names zip names

  val default = Random
}
