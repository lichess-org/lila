package draughts

package object format {

  case class FEN(value: String) extends AnyVal {
    override def toString = value
  }
}