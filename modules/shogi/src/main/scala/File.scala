package shogi

case class File private (val index: Int) extends AnyVal with Ordered[File] {
  @inline def -(that: File): Int           = index - that.index
  @inline override def compare(that: File) = this - that

  def offset(delta: Int): Option[File] =
    File(index + delta)

  @inline def char: Char = (49 + index).toChar
  override def toString  = char.toString
}

object File {
  def apply(index: Int): Option[File] =
    if (0 <= index && index < Pos.MaxFiles) Some(new File(index))
    else None

  @inline def of(pos: Pos): File = new File(pos.index % Pos.MaxFiles)

  def fromChar(ch: Char): Option[File] = apply(ch.toInt - 48)

  val First   = new File(0)
  val Second  = new File(1)
  val Third   = new File(2)
  val Forth   = new File(3)
  val Fifth   = new File(4)
  val Sixth   = new File(5)
  val Seventh = new File(6)
  val Eighth  = new File(7)
  val Ninth   = new File(8)

  val all = List(First, Second, Third, Forth, Fifth, Sixth, Seventh, Eighth, Ninth)
}
