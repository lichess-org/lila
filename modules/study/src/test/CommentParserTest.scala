package lila.study

import org.specs2.mutable._
import org.specs2.specification._
import lila.socket.tree.Node.Shapes
import lila.socket.tree.Node.Shape._

class CommentParserTest extends Specification {

  val C = CommentParser

  "remove clk" should {
    "empty" in {
      C.removeClk("") must_== ""
    }
    "without" in {
      C.removeClk("Hello there") must_== "Hello there"
    }
    "at start" in {
      C.removeClk("[%clk 10:40:33] Hello there") must_== "Hello there"
    }
    "at end" in {
      C.removeClk("Hello there [%clk 10:40:33]") must_== "Hello there"
    }
    "multiple" in {
      C.removeClk("Hello there [%clk 10:40:33][%clk 10:40:33]") must_== "Hello there"
    }
    "new lines" in {
      C.removeClk("Hello there [%clk\n10:40:33]") must_== "Hello there"
    }
  }
  "parse shapes" should {
    "empty" in {
      C.extractShapes("") must_== (Shapes(Nil) -> "")
    }
    "without" in {
      C.extractShapes("Hello there") must_== (Shapes(Nil) -> "Hello there")
    }
    "at start" in {
      C.extractShapes("[%csl Gb4,Yd5,Rf6] Hello there") must beLike {
        case (shapes, "Hello there") => shapes.value must haveSize(3)
      }
    }
    "at end" in {
      C.extractShapes("Hello there [%csl Gb4,Yd5,Rf6]") must beLike {
        case (shapes, "Hello there") => shapes.value must haveSize(3)
      }
    }
    "multiple" in {
      C.extractShapes("Hello there [%csl Gb4,Yd5,Rf6][%cal Ge2e4,Ye2d4,Re2g4]") must beLike {
        case (shapes, "Hello there") => shapes.value must haveSize(6)
      }
    }
    "new lines" in {
      C.extractShapes("Hello there [%csl\nGb4,Yd5,Rf6]") must beLike {
        case (shapes, "Hello there") => shapes.value must haveSize(3)
      }
    }
    "multiple, one new line" in {
      C.extractShapes("Hello there [%csl\nGb4,Yd5,Rf6][%cal Ge2e4,Ye2d4,Re2g4]") must beLike {
        case (shapes, "Hello there") => shapes.value must haveSize(6)
      }
      C.extractShapes("Hello there [%csl Gb4,Yd5,Rf6][%cal\nGe2e4,Ye2d4,Re2g4]") must beLike {
        case (shapes, "Hello there") => shapes.value must haveSize(6)
      }
    }
    "multiple mess" in {
      C.extractShapes("Hello there [%csl \n\n Gb4,Yd5,Rf6][%cal\nGe2e4,Ye2d4,Re2g4]") must beLike {
        case (shapes, "Hello there") => shapes.value must haveSize(6)
      }
    }
  }
}
