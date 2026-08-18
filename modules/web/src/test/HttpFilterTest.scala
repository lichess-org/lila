package lila.web

import scalalib.net.UserAgent

class HttpFilterTest extends munit.FunSuite:

  def agent(ua: String, client: String)(using munit.Location): Unit =
    assertEquals(HttpFilter.apiAgent(UserAgent(ua)), client)

  test("api agent"):
    agent(
      "Lichess Mobile/0.25.5 as:someone283 sri:9utYa1xxX1rf8n9rf os:Android/11 dev:motorola one 5G ace",
      "lichess mobile"
    )
    agent("maia-bot/0.2.1 user:maia1", "maia-bot")
    agent("some-agent", "some-agent")
    agent("ChessEdge-StreamerMonitor/2.0", "chessedge-streamermonitor")
    agent(
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36",
      "mozilla"
    )
    agent(
      "\"Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
      "mozilla"
    )
    agent("lichess-bot/2024.9.19.1 user:Goldfish-Engine", "lichess-bot")
    agent("Endgame-Broadcast/1.0 (+https://endgame.ai/; contact:emmalie@endgame.ai)", "endgame-broadcast")
    agent("DiscoChess hello@discochess.com", "discochess hello")
    agent("Python-urllib/3.12", "python-urllib")
    agent("chessever.com", "chessever.com")
    agent("Lichess Ladders (lichessladders.com)", "lichess ladders")
    agent("LichessWidgetsExtension/002505 CFNetwork/3860.600.12 Darwin/25.5.0", "lichesswidgetsextension")
    agent("", "-")
    agent("38298", "-")
    agent("...", "-")
