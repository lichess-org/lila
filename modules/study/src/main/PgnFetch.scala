package lila.study

import chess.format.pgn.PgnStr
import play.api.libs.ws.StandaloneWSClient

final private class PgnFetch(ws: StandaloneWSClient):

  private val pgnContentType = "application/x-chess-pgn"

  // http://www.chessgames.com/perl/chessgame?gid=1427487
  // http://www.chessgames.com/perl/nph-chesspgn?text=1&gid=1427487
  // http://www.chessgames.com/pgn/boleslavsky_ufimtsev_1944.pgn?gid=1427487
  private val ChessbaseRegex = """chessgames\.com/.*[\?&]gid=(\d+)""".r.unanchored

  def fromUrl(url: String): Fu[Option[PgnStr]] =
    url match
      case ChessbaseRegex(id) => id.toIntOption.so(downloadChessbase)
      case _                  => fuccess(none)

  private def downloadChessbase(id: Int): Fu[Option[PgnStr]] =
    ws.url(s"""http://www.chessgames.com/pgn/any.pgn?gid=$id""").get().dmap { res =>
      res.header("Content-Type").contains(pgnContentType).option(PgnStr(res.body))
    }
