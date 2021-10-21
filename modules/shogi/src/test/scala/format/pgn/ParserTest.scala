package shogi
package format
package pgn

import variant.Standard

import cats.syntax.option._

class ParserTest extends ShogiTest {

  import Fixtures._

  val parser                 = Parser.full _
  def parseMove(str: String) = Parser.MoveParser(str, Standard)

  "basic" should {
    "drop" in {
      parser("P*e5") must beValid.like { case a =>
        a.parsedMoves.value.headOption must beSome.like { case d: Drop =>
          d.role must_== Pawn
          d.pos must_== Pos.SQ5E
        }
      }
    }
  }

  "promotion" should {
    "as a true" in {
      parser("Pb8+") must beValid.like { case a =>
        a.parsedMoves.value.headOption must beSome.like { case san: PGNStd =>
          san.promotion must_== true
        }
      }
    }
    "disambigued" in {
      parseMove("Be5g7+") must beValid.like { case a: PGNStd =>
        a.dest === Pos.SQ3C
        a.role === Bishop
        a.promotion === true
      }
    }
    "as a false" in {
      parser("Pb8=") must beValid.like { case a =>
        a.parsedMoves.value.headOption must beSome.like { case san: PGNStd =>
          san.promotion must_== false
        }
      }
    }
  }

  "glyphs" in {
    parseMove("Pe4") must beValid.like { case a =>
      a must_== PGNStd(Pos.SQ5F, Pawn)
    }
    parseMove("Pe4!") must beValid.like { case a: PGNStd =>
      a.dest === Pos.SQ5F
      a.role === Pawn
      a.metas.glyphs === Glyphs(Glyph.MoveAssessment.good.some, None, Nil)
    }

    parser("Pe7+!") must beValid
    parser("Pe7=!") must beValid

    parseMove("P*e4?!") must beValid.like { case a: Drop =>
      a.pos === Pos.SQ5F
      a.role === Pawn
      a.metas.glyphs === Glyphs(Glyph.MoveAssessment.dubious.some, None, Nil)
    }

    parseMove("Be5g7?!") must beValid.like { case a: PGNStd =>
      a.dest === Pos.SQ3C
      a.role === Bishop
      a.promotion === false
      a.metas.glyphs === Glyphs(Glyph.MoveAssessment.dubious.some, None, Nil)
    }
    parseMove("Be5g7+?!") must beValid.like { case a: PGNStd =>
      a.dest === Pos.SQ3C
      a.role === Bishop
      a.promotion === true
      a.metas.glyphs === Glyphs(Glyph.MoveAssessment.dubious.some, None, Nil)
    }
    parseMove("Be5g7=?!") must beValid.like { case a: PGNStd =>
      a.dest === Pos.SQ3C
      a.role === Bishop
      a.promotion === false
      a.metas.glyphs === Glyphs(Glyph.MoveAssessment.dubious.some, None, Nil)
    }
    parser("Be5g7+!") must beValid
  }

  "comments" in {
    parser("Ne5f7+! {such a neat comment}") must beValid.like {
      case ParsedNotation(_, _, ParsedMoves(List(san))) =>
        san.metas.comments must_== List("such a neat comment")
    }
  }

  "first move variation" in {
    parser("1. Pe4 (1. Pd4)") must beValid.like { case ParsedNotation(_, _, ParsedMoves(List(san))) =>
      san.metas.variations.headOption must beSome.like { case variation =>
        variation.value must haveSize(1)
      }
    }
  }

  "disambiguated" in {
    parser(disambiguated) must beValid.like { case a =>
      a.parsedMoves.value.length must_== 7
    }
  }

  "tag with nested quotes" in {
    parser("""[Gote "Schwarzenegger, Arnold \"The Terminator\""]""") must beValid.like { case a =>
      a.tags.value must contain { (tag: Tag) =>
        tag.name == Tag.Gote && tag.value == """Schwarzenegger, Arnold "The Terminator""""
      }
    }
  }

  "tag with inner brackets" in {
    parser("""[Gote "[=0040.34h5a4]"]""") must beValid.like { case a =>
      a.tags.value must contain { (tag: Tag) =>
        tag.name == Tag.Gote && tag.value == "[=0040.34h5a4]"
      }
    }
  }

}
