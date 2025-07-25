package lila.ui

import ScalatagsTemplate.*

opaque type Nonce = String
object Nonce extends OpaqueString[Nonce]:
  def random: Nonce = Nonce(scalalib.SecureRandom.nextString(24))

type Optionce = Option[Nonce]
type WithNonce[A] = Optionce => A
given cats.Monoid[WithNonce[Frag]] with
  def empty = _ => emptyFrag
  def combine(a: WithNonce[Frag], b: WithNonce[Frag]) = n => frag(a(n), b(n))
