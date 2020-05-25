package draughts

import Board.BoardSize

object Piotr {

  val all = List(
    'a', 'b', 'c', 'd', 'e',
    'f', 'g', 'h', 'i', 'j',
    'k', 'l', 'm', 'n', 'o',
    'p', 'q', 'r', 's', 't',
    'u', 'v', 'w', 'x', 'y',
    'z', 'A', 'B', 'C', 'D',
    'E', 'F', 'G', 'H', 'I',
    'J', 'K', 'L', 'M', 'N',
    'O', 'P', 'Q', 'R', 'S',
    'T', 'U', 'V', 'W', 'X'
  )

  def byField(field: Int) = all(field - 1)

  def keyToPiotr(key: String) = BoardSize.max.posAt(key) map (_.piotr)
  def doubleKeyToPiotr(key: String) = for {
    a ← keyToPiotr(key take 2)
    b ← keyToPiotr(key drop 2)
  } yield s"$a$b"
  def doublePiotrToKey(piotrs: String) = for {
    a ← BoardSize.max.piotr(piotrs.head)
    b ← BoardSize.max.piotr(piotrs(1))
  } yield s"${a.key}${b.key}"
}
