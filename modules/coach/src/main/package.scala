package lila

package object coach extends PackageObject with WithPlay {

  import ornicar.scalalib.Zero
  implicit def NumbersZero: Zero[Numbers] = Zero.instance(Numbers.empty)
}
