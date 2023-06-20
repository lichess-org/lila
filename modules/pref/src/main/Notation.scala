package lila.pref

sealed class Notation private[pref] (val index: Int) {

  override def toString = index.toString

}

object Notations {

  val western       = new Notation(0) // 11
  val kawasaki      = new Notation(1)
  val japanese      = new Notation(2)
  val westernEngine = new Notation(3) // 1a
  val kif           = new Notation(4)
  val usi           = new Notation(5)

  val all = List(
    western,
    westernEngine,
    kawasaki,
    japanese,
    kif,
    usi
  )
  val allToString = all.map(_.toString)

  lazy val allByIndex = all map { n =>
    n.index -> n
  } toMap

}
