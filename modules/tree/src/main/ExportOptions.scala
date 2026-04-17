package lila.tree

// Options for exporting a game or study to pgn or json
case class ExportOptions(
    movetimes: Boolean = false,
    division: Boolean = false,
    clocks: Boolean = false,
    blurs: Boolean = false,
    rating: Boolean = true,
    puzzles: Boolean = false,
    nvui: Boolean = false,
    lichobileCompat: Boolean = false
)
object ExportOptions:
  val default = ExportOptions()
