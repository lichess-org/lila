package lila.game

private[game] object MoveTime {

  def encode(mts: List[Int]): String = (mts map encode).mkString

  def encode(mt: Int): Char = encodeHash get mt getOrElse lastChar

  def decode(str: String): List[Int] = str.toList map { mt =>
    decodeHash get mt getOrElse lastInt
  }

  private val chars: List[Char] =
    "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toList

  private val decodeHash: Map[Char, Int] = chars.zipWithIndex.toMap

  private val encodeHash: Map[Int, Char] = decodeHash.map(x => x._2 -> x._1).toMap

  private val lastChar: Char = chars.last
  private val lastInt: Int = chars.size - 1
}
