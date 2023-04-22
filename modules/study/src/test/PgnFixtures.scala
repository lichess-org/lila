package lila.study

object PgnFixtures:

  val pgn  = """
  { Root comment }
1. e4! $16 $40 $32 (1. d4?? d5 $146 { d5 is a good move }) 1... e6?! { e6 is a naughty move } *
  """
  val pgn2 = """
[Event "nt9's Study: Chapter 2"]
[Site "https://lichess.org/study/Q41XcI0B/2LjSXwxW"]
[Result "*"]
[UTCDate "2023.04.22"]
[UTCTime "20:41:25"]
[Variant "Standard"]
[ECO "?"]
[Opening "?"]
[Annotator "https://lichess.org/@/nt9"]

{ this is a study without moves }
*
  """

  val all = List(pgn, pgn2)
