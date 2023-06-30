package lila.game

import shogi.format.forsyth.Sfen
import shogi.format.usi.Usi

// for bots and shoginet
object FairyConversion {
  object Kyoto {

    def makeFairySfen(sfen: Sfen): Sfen =
      Sfen(
        List(
          sfen.boardString.fold("") { _.flatMap(c => kyotoBoardMap.getOrElse(c, c.toString)) },
          sfen.color.map(_.letter.toString) | "b",
          sfen.handsString.fold("-") { _.map(c => kyotoHandsMap.getOrElse(c, c)) }
        ).mkString(" ")
      )

    def readFairyUsi(usiStr: String): Option[Usi] =
      Usi(fairyToUsi(usiStr))

    def readFairyUsiList(moves: String): Option[List[Usi]] =
      Usi.readList(moves.split(' ').toList.map(fairyToUsi))

    def makeFairyUsiList(usiList: Seq[Usi], initialSfen: Option[Sfen]): List[String] =
      shogi.Replay
        .usiWithRoleWhilePossible(
          usiList,
          initialSfen,
          shogi.variant.Kyotoshogi
        )
        .map(uwr => usiWithRoleToFairy(uwr))

    def usiWithRoleToFairy(usiWithRole: Usi.WithRole): String = {
      val usi        = usiWithRole.usi
      val usiStr     = usi.usi
      val roleLetter = usiWithRole.role.name.head.toUpper
      usi match {
        case move: Usi.Move =>
          if (move.promotion && kyotoBoardMap.contains(roleLetter))
            usiStr.replace("+", "-")
          else usiStr
        case _: Usi.Drop =>
          kyotoBoardMap.get(roleLetter).fold(usiStr) { c =>
            s"$c${usiStr.drop(1)}"
          }
      }
    }

    def fairyToUsi(str: String): String =
      if (str.startsWith("+")) dropRoles.get(str.take(2)).fold(str)(rc => s"$rc${str.drop(2)}")
      else if (str.endsWith("-")) str.replace('-', '+')
      else str

    private val kyotoBoardMap: Map[Char, String] = Map(
      'g' -> "+n",
      'G' -> "+N",
      't' -> "+l",
      'T' -> "+L",
      'b' -> "+s",
      'B' -> "+S",
      'r' -> "+p",
      'R' -> "+P"
    )
    private val kyotoHandsMap: Map[Char, Char] = Map(
      'g' -> 'n',
      'G' -> 'N',
      't' -> 'l',
      'T' -> 'L'
    )
    private val dropRoles: Map[String, Char] = kyotoBoardMap.map { case (k, v) => (v, k) } toMap

  }
}
