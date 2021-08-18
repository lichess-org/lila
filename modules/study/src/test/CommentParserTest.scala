package lila.study

import org.specs2.mutable._

import shogi.Centis
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
      C("[%csl Gb4,Yd5,Rf6] Hello there").comment must_== "Hello there"
    }
    "at end" in {
      C("Hello there [%csl Gb4,Yd5,Rf6]").comment must_== "Hello there"
    }
    "multiple" in {
      C("Hello there [%csl Gb4,Yd5,Rf6][%csl Gb4,Yd5,Rf6]").comment must_== "Hello there"
    }
    "new lines" in {
      C("Hello there [%csl\n Gb4,Yd5,Rf6]").comment must_== "Hello there"
    }
  }
  "parse shapes" should {
    "empty" in {
      C("") must_== C.ParsedComment(Shapes(Nil), "")
    }
    "without" in {
      C("Hello there") must_== C.ParsedComment(Shapes(Nil), "Hello there")
    }
    "at start" in {
      C("[%csl Gb4,Yd5,Rf6] Hello there") must beLike { case C.ParsedComment(shapes, "Hello there") =>
        shapes.value must haveSize(3)
      }
    }
    "at end" in {
      C("Hello there [%csl Gb4,Yd5,Rf6]") must beLike { case C.ParsedComment(shapes, "Hello there") =>
        shapes.value must haveSize(3)
      }
    }
    "multiple" in {
      C("Hello there [%csl Gb4,Yd5,Rf6][%cal Ge2e4,Ye2d4,Re2g4]") must beLike {
        case C.ParsedComment(shapes, "Hello there") => shapes.value must haveSize(6)
      }
    }
    "new lines" in {
      C("Hello there [%csl\nGb4,Yd5,Rf6]") must beLike { case C.ParsedComment(shapes, "Hello there") =>
        shapes.value must haveSize(3)
      }
    }
    "multiple, one new line" in {
      C("Hello there [%csl\nGb4,Yd5,Rf6][%cal Ge2e4,Ye2d4,Re2g4]") must beLike {
        case C.ParsedComment(shapes, "Hello there") => shapes.value must haveSize(6)
      }
      C("Hello there [%csl Gb4,Yd5,Rf6][%cal\nGe2e4,Ye2d4,Re2g4]") must beLike {
        case C.ParsedComment(shapes, "Hello there") => shapes.value must haveSize(6)
      }
    }
    "multiple mess" in {
      C("Hello there [%csl \n\n Gb4,Yd5,Rf6][%cal\nGe2e4,Ye2d4,Re2g4]") must beLike {
        case C.ParsedComment(shapes, "Hello there") => shapes.value must haveSize(6)
      }
    }
  }
  "parse all" should {
    "multiple shapes" in {
      C("Hello there [%csl \n\n Gb4,Yd5,Rf6][%cal\nGe2e4,Ye2d4,Re2g4]") must beLike {
        case C.ParsedComment(shapes, "Hello there") =>
          shapes.value must haveSize(6)
      }
    }
  }
}
