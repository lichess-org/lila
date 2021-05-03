package chess
package format.pgn

class TagTest extends ChessTest {

  "Tags" should {
    // http://www.saremba.de/chessgml/standards/pgn/pgn-complete.htm#c8.1.1
    "be sorted" in {
      Tags(
        List(
          Tag(Tag.Site, "https://lichess.org/QuCzSfxw"),
          Tag(Tag.Round, "-"),
          Tag(Tag.Date, "2018.05.04"),
          Tag(Tag.Black, "penguingim1"),
          Tag(Tag.White, "DrDrunkenstein"),
          Tag(Tag.Result, "1-0"),
          Tag(Tag.UTCDate, "2018.05.04"),
          Tag(Tag.UTCTime, "20:59:23"),
          Tag(Tag.WhiteElo, "2870"),
          Tag(Tag.BlackElo, "2862"),
          Tag(Tag.WhiteRatingDiff, "+12"),
          Tag(Tag.BlackRatingDiff, "-7"),
          Tag(Tag.Event, "Titled Arena 5")
        )
      ).sorted.value.map(_.name) must_== List(
        Tag.Event,
        Tag.Site,
        Tag.Date,
        Tag.Round,
        Tag.White,
        Tag.Black,
        Tag.Result,
        Tag.UTCDate,
        Tag.UTCTime,
        Tag.WhiteElo,
        Tag.BlackElo,
        Tag.WhiteRatingDiff,
        Tag.BlackRatingDiff
      )
    }
  }
}
