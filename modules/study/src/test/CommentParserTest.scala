package lila.study

import org.specs2.mutable._

import chess.Centis
import lila.tree.Node.Shapes

class CommentParserTest extends Specification {

  import chess.format.pgn.Comment
  import scala.language.implicitConversions
  given Conversion[String, Comment] = Comment(_)

  val C = CommentParser

  "parse comment" >> {
    "empty" >> {
      C("").comment === ""
    }
    "without" >> {
      C("Hello there").comment === "Hello there"
    }
    "at start" >> {
      C("[%clk 10:40:33] Hello there").comment === "Hello there"
    }
    "at end" >> {
      C("Hello there [%clk 10:40:33]").comment === "Hello there"
    }
    "multiple" >> {
      C("Hello there [%clk 10:40:33][%clk 10:40:33]").comment === "Hello there"
    }
    "new lines" >> {
      C("Hello there [%clk\n10:40:33]").comment === "Hello there"
    }
  }
  "parse clock" >> {
    "empty" >> {
      C("").clock must beNone
    }
    "without" >> {
      C("Hello there").clock must beNone
    }
    "only" >> {
      C("[%clk 10:40:33]").clock === Some(Centis(3843300))
    }
    "one hour" >> {
      C("[%clk 1:40:33]").clock === Some(Centis(603300))
    }
    "at start" >> {
      C("[%clk 10:40:33] Hello there").clock === Some(Centis(3843300))
    }
    "at end" >> {
      C("Hello there [%clk 10:40:33]").clock === Some(Centis(3843300))
    }
    "in the middle" >> {
      C("Hello there [%clk 10:40:33] something else").clock === Some(Centis(3843300))
    }
    "multiple" >> {
      C("Hello there [%clk 10:40:33][%clk 10:40:33]").clock === Some(Centis(3843300))
    }
    "new lines" >> {
      C("Hello there [%clk\n10:40:33]").clock === Some(Centis(3843300))
    }
    "no seconds" >> {
      C("Hello there [%clk 2:10] something else").clock === Some(Centis(780000))
    }
    "alt format" >> {
      C("Hello there [%clk 2:10.33] something else").clock === Some(Centis(783300))
    }
    "TCEC" >> {
      C(
        "d=29, pd=Bb7, mt=00:01:01, tl=00:37:47, s=27938 kN/s, n=1701874274, pv=e4 Bb7 exf5 exf5 d5 Qf6 Rab1 Rae8 g3 Bc8 Kg2 Rxe1 Rxe1, tb=0, R50=49, wv=0.45,"
      ).clock ===
        Some(Centis(100 * 47 + 37 * 60 * 100))
    }
    "fractional second" >> {
      C("Hello there [%clk 10:40:33.55] something else").clock === Some(Centis(3843355))
    }
    "fractional second rounded to centisecond" >> {
      C("Hello there [%clk 10:40:33.556] something else").clock === Some(Centis(3843356))
    }
  }
  "parse shapes" >> {
    "empty" >> {
      C("") === C.ParsedComment(Shapes(Nil), None, "")
    }
    "without" >> {
      C("Hello there") === C.ParsedComment(Shapes(Nil), None, "Hello there")
    }
    "at start" >> {
      C("[%csl Gb4,Yd5,Rf6] Hello there") must beLike { case C.ParsedComment(shapes, None, "Hello there") =>
        shapes.value must haveSize(3)
      }
    }
    "at end" >> {
      C("Hello there [%csl Gb4,Yd5,Rf6]") must beLike { case C.ParsedComment(shapes, None, "Hello there") =>
        shapes.value must haveSize(3)
      }
    }
    "multiple" >> {
      C("Hello there [%csl Gb4,Yd5,Rf6][%cal Ge2e4,Ye2d4,Re2g4]") must beLike {
        case C.ParsedComment(shapes, None, "Hello there") => shapes.value must haveSize(6)
      }
    }
    "new lines" >> {
      C("Hello there [%csl\nGb4,Yd5,Rf6]") must beLike { case C.ParsedComment(shapes, None, "Hello there") =>
        shapes.value must haveSize(3)
      }
    }
    "multiple, one new line" >> {
      C("Hello there [%csl\nGb4,Yd5,Rf6][%cal Ge2e4,Ye2d4,Re2g4]") must beLike {
        case C.ParsedComment(shapes, None, "Hello there") => shapes.value must haveSize(6)
      }
      C("Hello there [%csl Gb4,Yd5,Rf6][%cal\nGe2e4,Ye2d4,Re2g4]") must beLike {
        case C.ParsedComment(shapes, None, "Hello there") => shapes.value must haveSize(6)
      }
    }
    "multiple mess" >> {
      C("Hello there [%csl \n\n Gb4,Yd5,Rf6][%cal\nGe2e4,Ye2d4,Re2g4]") must beLike {
        case C.ParsedComment(shapes, None, "Hello there") => shapes.value must haveSize(6)
      }
    }
  }
  "parse all" >> {
    "multiple shapes + clock" >> {
      C("Hello there [%clk 10:40:33][%csl \n\n Gb4,Yd5,Rf6][%cal\nGe2e4,Ye2d4,Re2g4]") must beLike {
        case C.ParsedComment(shapes, clock, "Hello there") =>
          shapes.value must haveSize(6)
          clock === Some(Centis(3843300))
      }
    }
  }
}
