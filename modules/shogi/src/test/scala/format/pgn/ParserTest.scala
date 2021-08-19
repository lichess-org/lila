package shogi
package format.pgn
import variant.Standard

class ParserTest extends ShogiTest {

  import Fixtures._

  val parser                 = Parser.full _
  def parseMove(str: String) = Parser.MoveParser(str, Standard)

  "basic" should {
    "drop" in {
      parser("P*e5") must beSuccess.like { case a =>
        a.sans.value.headOption must beSome.like { case d: Drop =>
          d.role must_== Pawn
          d.pos must_== Pos.E5
        }
      }
    }
  }

  "promotion" should {
    "as a true" in {
      parser("Pb8+") must beSuccess.like { case a =>
        a.sans.value.headOption must beSome.like { case san: Std =>
          san.promotion must_== true
        }
      }
    }
    "disambigued" in {
      parseMove("Be5g7+") must beSuccess.like { case a: Std =>
        a.dest === Pos.G7
        a.role === Bishop
        a.promotion === true
      }
    }
    "as a false" in {
      parser("Pb8=") must beSuccess.like { case a =>
        a.sans.value.headOption must beSome.like { case san: Std =>
          san.promotion must_== false
        }
      }
    }
  }

  "glyphs" in {
    parseMove("Pe4") must beSuccess.like { case a =>
      a must_== Std(Pos.E4, Pawn)
    }
    parseMove("Pe4!") must beSuccess.like { case a: Std =>
      a.dest === Pos.E4
      a.role === Pawn
      a.metas.glyphs === Glyphs(Glyph.MoveAssessment.good.some, None, Nil)
    }

    parser("Pe7+!") must beSuccess
    parser("Pe7=!") must beSuccess

    parseMove("P*e4?!") must beSuccess.like { case a: Drop =>
      a.pos === Pos.E4
      a.role === Pawn
      a.metas.glyphs === Glyphs(Glyph.MoveAssessment.dubious.some, None, Nil)
    }

    parseMove("Be5g7?!") must beSuccess.like { case a: Std =>
      a.dest === Pos.G7
      a.role === Bishop
      a.promotion === false
      a.metas.glyphs === Glyphs(Glyph.MoveAssessment.dubious.some, None, Nil)
    }
    parseMove("Be5g7+?!") must beSuccess.like { case a: Std =>
      a.dest === Pos.G7
      a.role === Bishop
      a.promotion === true
      a.metas.glyphs === Glyphs(Glyph.MoveAssessment.dubious.some, None, Nil)
    }
    parseMove("Be5g7=?!") must beSuccess.like { case a: Std =>
      a.dest === Pos.G7
      a.role === Bishop
      a.promotion === false
      a.metas.glyphs === Glyphs(Glyph.MoveAssessment.dubious.some, None, Nil)
    }
    parser("Be5g7+!") must beSuccess
  }

  "comments" in {
    parser("Ne5f7+! {such a neat comment}") must beSuccess.like { case ParsedPgn(_, _, Sans(List(san))) =>
      san.metas.comments must_== List("such a neat comment")
    }
  }

  "first move variation" in {
    parser("1. Pe4 (1. Pd4)") must beSuccess.like { case ParsedPgn(_, _, Sans(List(san))) =>
      san.metas.variations.headOption must beSome.like { case variation =>
        variation.value must haveSize(1)
      }
    }
  }

  "disambiguated" in {
    parser(disambiguated) must beSuccess.like { case a =>
      a.sans.value.size must_== 7
    }
  }

  "tag with nested quotes" in {
    parser("""[Gote "Schwarzenegger, Arnold \"The Terminator\""]""") must beSuccess.like { case a =>
      a.tags.value must contain { (tag: Tag) =>
        tag.name == Tag.Gote && tag.value == """Schwarzenegger, Arnold "The Terminator""""
      }
    }
  }

  "tag with inner brackets" in {
    parser("""[Gote "[=0040.34h5a4]"]""") must beSuccess.like { case a =>
      a.tags.value must contain { (tag: Tag) =>
        tag.name == Tag.Gote && tag.value == "[=0040.34h5a4]"
      }
    }
  }

}
