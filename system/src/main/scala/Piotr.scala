package lila.system

import lila.chess.{ Pos, Role }
import Pos._

object Piotr {

  val decodePos: Map[Char, Pos] = Map('a' -> A1, 'b' -> B1, 'c' -> C1, 'd' -> D1, 'e' -> E1, 'f' -> F1, 'g' -> G1, 'h' -> H1, 'i' -> A2, 'j' -> B2, 'k' -> C2, 'l' -> D2, 'm' -> E2, 'n' -> F2, 'o' -> G2, 'p' -> H2, 'q' -> A3, 'r' -> B3, 's' -> C3, 't' -> D3, 'u' -> E3, 'v' -> F3, 'w' -> G3, 'x' -> H3, 'y' -> A4, 'z' -> B4, 'A' -> C4, 'B' -> D4, 'C' -> E4, 'D' -> F4, 'E' -> G4, 'F' -> H4, 'G' -> A5, 'H' -> B5, 'I' -> C5, 'J' -> D5, 'K' -> E5, 'L' -> F5, 'M' -> G5, 'N' -> H5, 'O' -> A6, 'P' -> B6, 'Q' -> C6, 'R' -> D6, 'S' -> E6, 'T' -> F6, 'U' -> G6, 'V' -> H6, 'W' -> A7, 'X' -> B7, 'Y' -> C7, 'Z' -> D7, '0' -> E7, '1' -> F7, '2' -> G7, '3' -> H7, '4' -> A8, '5' -> B8, '6' -> C8, '7' -> D8, '8' -> E8, '9' -> F8, '!' -> G8, '?' -> H8)

  val encodePos: Map[Pos, Char] = decodePos map { p ⇒ (p._2, p._1) } toMap

  val decodeRole: Map[Char, Role] = Role.all map { r => (r.forsyth, r) } toMap
}
