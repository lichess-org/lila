package lila.lobby

import scalalib.ThreadLocalRandom.nextBoolean

enum TriColor(val name: String, val resolve: () => Color):
  case White  extends TriColor("white", () => Color.white)
  case Black  extends TriColor("black", () => Color.black)
  case Random extends TriColor("random", () => Color.fromWhite(nextBoolean()))

  def negate: TriColor = this match
    case White  => Black
    case Black  => White
    case Random => Random
  def compatibleWith(o: TriColor) = o == negate

object TriColor:

  def apply(name: String): Option[TriColor] = all.find(_.name == name)

  def orDefault(name: String): TriColor = apply(name) | default

  def orDefault(name: Option[String]): TriColor = name.flatMap(apply) | default

  val all = values.toList

  val names = all.map(_.name)

  val choices = names.zip(names)

  val default = Random
