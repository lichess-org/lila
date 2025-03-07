package lila.relay

import chess.format.pgn.{ PgnStr, Tags }

import lila.core.study.data.StudyChapterName
import lila.study.{ Chapter, MultiPgn }
import lila.tree.Root

private object RelayPlanUpdateFixtures:

  def mkChapter(order: Chapter.Order, tags: Tags): Chapter =
    Chapter(
      id = StudyChapterId(s"chapterId$order"),
      studyId = StudyId("studyId"),
      name = StudyChapterName(s"chapterName$order"),
      setup = Chapter.Setup(gameId = none, variant = chess.variant.Standard, orientation = chess.Color.White),
      root = Root.default(chess.variant.Standard),
      tags = tags,
      order = order,
      ownerId = UserId.lichess,
      createdAt = nowInstant
    )

  val initialChapter = mkChapter(1, Tags.empty)

  val games: RelayGames = RelayGame.iso.to:
    MultiPgn.split(
      PgnStr("""
[Event "SixDays Budapest June GMA"]
[Site "Budapest"]
[Round "9.1"]
[White "Banh Gia Huy"]
[Black "Yaniv, Yuval"]
[WhiteElo "2400"]
[WhiteTitle "FM"]
[WhiteFideId "12424714"]
[BlackElo "2335"]
[BlackTitle "FM"]
[BlackFideId "2823900"]

1. c4 { [%eval 0.13] } 1... g6 


[Event "SixDays Budapest June GMA"]
[Site "Budapest"]
[Round "9.2"]
[White "Ezra Paul Chambers"]
[Black "Panesar Vedant"]
[WhiteElo "2346"]
[WhiteTitle "FM"]
[WhiteFideId "20300204"]
[BlackElo "2460"]
[BlackTitle "FM"]
[BlackFideId "35033018"]

1. e4 { [%eval 0.15] [%clk 1:20:10] } 1... c5 { [%eval 0.25] [%clk 1:30:56] } 


[Event "SixDays Budapest June GMA"]
[Site "Budapest"]
[Round "9.3"]
[White "Aczel, Gergely"]
[Black "Ramoutar, Alan-Safar"]
[WhiteElo "2502"]
[WhiteTitle "GM"]
[WhiteFideId "727709"]
[BlackElo "2357"]
[BlackTitle "IM"]
[BlackFideId "7704224"]

1. c4 { [%eval 0.13] [%clk 1:27:40] } 1... e6 


[Event "SixDays Budapest June GMA"]
[Site "Budapest"]
[Round "9.4"]
[White "Berczes, David"]
[Black "Pap, Misa"]
[WhiteElo "2425"]
[WhiteTitle "GM"]
[WhiteFideId "722960"]
[BlackElo "2374"]
[BlackTitle "GM"]
[BlackFideId "921610"]

1. Nf3 { [%eval 0.14] } 1... Nf6 { [%eval 0.22] } 


[Event "SixDays Budapest June GMA"]
[Site "Budapest"]
[Round "9.5"]
[White "Moksh Amit Doshi"]
[Black "Paszewski, Mateusz"]
[WhiteElo "2358"]
[WhiteTitle "IM"]
[WhiteFideId "25064967"]
[BlackElo "2349"]
[BlackTitle "FM"]
[BlackFideId "1141058"]

1. d4 { [%eval 0.16] [%clk 1:27:11] } 1... f5 { [%eval 0.5] [%clk 1:30:31] } 
"""),
      Max(64)
    )

  lazy val chapters: List[Chapter] = games.zipWithIndex.toList.map: (game, i) =>
    mkChapter(i + 1, game.tags)

  object repeatedPairings:
    val games: RelayGames = RelayGame.iso.to:
      MultiPgn.split(
        PgnStr("""
[White "Banh Gia Huy"]
[Black "Yaniv, Yuval"]
[Round "1.1"]

1. c4 { [%eval 0.13] } 1... g6 


[White "Banh Gia Huy"]
[Black "Yaniv, Yuval"]
[Round "1.2"]

1. e4 { [%eval 0.15] [%clk 1:20:10] } 1... c5 { [%eval 0.25] [%clk 1:30:56] } 


[White "Banh Gia Huy"]
[Black "Yaniv, Yuval"]
[Round "1.3"]

1. c4 { [%eval 0.13] [%clk 1:27:40] } 1... e6 


[White "Berczes, David"]
[Black "Pap, Misa"]
[Round "1.1"]

1. Nf3 { [%eval 0.14] } 1... Nf6 { [%eval 0.22] } 


[White "Berczes, David"]
[Black "Pap, Misa"]
[Round "1.2"]

1. d4 { [%eval 0.16] [%clk 1:27:11] } 1... f5 { [%eval 0.5] [%clk 1:30:31] } 
  """),
        Max(64)
      )

    lazy val chapters: List[Chapter] = games.zipWithIndex.toList.map: (game, i) =>
      mkChapter(i + 1, game.tags)
