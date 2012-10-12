package lila
package mongodb

object PgnBinary {

  def encode(pgn: String): Array[Byte] = {
    val ints = pgnToInts(pgn)
    val bytes = ints grouped 4 map { 
      case Nil => 0
      case a :: Nil => a
      case a :: b :: Nil => a + 32 

  def decode(bytes: Array[Byte]): String = ""

  private def pgnToInts(str: String): List[Int] = 
    stringToInt get (str take 1) map (i => (str drop 1, i)) orElse {
      if (str startsWith "O-O-O") stringToInt("O-O-O") map (i => (str drop 5), i)
      else if (str startsWith "O-O") stringToInt("O-O") map (i => (str drop 3), i)
      else None
    } fold ((int, rest) => int :: consumeString(rest), Nil)

  private val symbols: List[String] = " " :: {
    ('a' to 'h').map(_.toString) ++
      (1 to 8).map(_.toString) ++
      "RNBQKPx=+#".toList.map(_.toString) ++
      Seq("O-O", "O-O-O")
  }.toList

  private val stringToInt: Map[String, Int] = symbols.zipWithIndex.toMap
  private val intToString: Map[Int, String] = (stringToByte map {
    case (a, b) ⇒ (b, a)
  }).toMap

  // utility methods for printing a byte array
  def showArray(b: Array[Byte]) = b.map(showByte).mkString(" ")
  def showByte(b: Byte) = pad(((b + 256) % 256).toHexString.toUpperCase)
  private def pad(s: String) = if (s.length == 1) "0" + s else s
}
