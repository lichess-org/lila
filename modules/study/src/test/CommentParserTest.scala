package lila.study

import org.specs2.mutable._
import org.specs2.specification._

import chess.Centis
import lila.tree.Node.Shape._
import lila.tree.Node.Shapes

class CommentParserTest extends Specification {

  val C = CommentParser

  "parse comment" should {
    "empty" in {
      C("").comment must_== ""
    }
    "without" in {
      C("Hello there").comment must_== "Hello there"
    }
    "at start" in {
      C("[%clk 10:40:33] Hello there").comment must_== "Hello there"
    }
    "at end" in {
      C("Hello there [%clk 10:40:33]").comment must_== "Hello there"
    }
    "multiple" in {
      C("Hello there [%clk 10:40:33][%clk 10:40:33]").comment must_== "Hello there"
    }
    "new lines" in {
      C("Hello there [%clk\n10:40:33]").comment must_== "Hello there"
    }
  }
  "parse clock" should {
    "empty" in {
      C("").clock must beNone
    }
    "without" in {
      C("Hello there").clock must beNone
    }
    "only" in {
      C("[%clk 10:40:33]").clock must_== Some(Centis(3843300))
    }
    "one hour" in {
      C("[%clk 1:40:33]").clock must_== Some(Centis(603300))
    }
    "at start" in {
      C("[%clk 10:40:33] Hello there").clock must_== Some(Centis(3843300))
    }
    "at end" in {
      C("Hello there [%clk 10:40:33]").clock must_== Some(Centis(3843300))
    }
    "in the middle" in {
      C("Hello there [%clk 10:40:33] something else").clock must_== Some(Centis(3843300))
    }
    "multiple" in {
      C("Hello there [%clk 10:40:33][%clk 10:40:33]").clock must_== Some(Centis(3843300))
    }
    "new lines" in {
      C("Hello there [%clk\n10:40:33]").clock must_== Some(Centis(3843300))
    }
    "no hours" in {
      C("Hello there [%clk 40:33] something else").clock must_== Some(Centis(243300))
    }
  }
  "parse shapes" should {
    "empty" in {
      C("") must_== C.ParsedComment(Shapes(Nil), None, "")
    }
    "without" in {
      C("Hello there") must_== C.ParsedComment(Shapes(Nil), None, "Hello there")
    }
    "at start" in {
      C("[%csl Gb4,Yd5,Rf6] Hello there") must beLike {
        case C.ParsedComment(shapes, None, "Hello there") => shapes.value must haveSize(3)
      }
    }
    "at end" in {
      C("Hello there [%csl Gb4,Yd5,Rf6]") must beLike {
        case C.ParsedComment(shapes, None, "Hello there") => shapes.value must haveSize(3)
      }
    }
    "multiple" in {
      C("Hello there [%csl Gb4,Yd5,Rf6][%cal Ge2e4,Ye2d4,Re2g4]") must beLike {
        case C.ParsedComment(shapes, None, "Hello there") => shapes.value must haveSize(6)
      }
    }
    "new lines" in {
      C("Hello there [%csl\nGb4,Yd5,Rf6]") must beLike {
        case C.ParsedComment(shapes, None, "Hello there") => shapes.value must haveSize(3)
      }
    }
    "multiple, one new line" in {
      C("Hello there [%csl\nGb4,Yd5,Rf6][%cal Ge2e4,Ye2d4,Re2g4]") must beLike {
        case C.ParsedComment(shapes, None, "Hello there") => shapes.value must haveSize(6)
      }
      C("Hello there [%csl Gb4,Yd5,Rf6][%cal\nGe2e4,Ye2d4,Re2g4]") must beLike {
        case C.ParsedComment(shapes, None, "Hello there") => shapes.value must haveSize(6)
      }
    }
    "multiple mess" in {
      C("Hello there [%csl \n\n Gb4,Yd5,Rf6][%cal\nGe2e4,Ye2d4,Re2g4]") must beLike {
        case C.ParsedComment(shapes, None, "Hello there") => shapes.value must haveSize(6)
      }
    }
  }
  "parse all" should {
    "multiple shapes + clock" in {
      C("Hello there [%clk 10:40:33][%csl \n\n Gb4,Yd5,Rf6][%cal\nGe2e4,Ye2d4,Re2g4]") must beLike {
        case C.ParsedComment(shapes, clock, "Hello there") =>
          shapes.value must haveSize(6)
          clock must_== Some(Centis(3843300))
      }
    }
  }
}
