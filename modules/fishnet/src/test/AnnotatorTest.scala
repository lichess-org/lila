package lila.fishnet

final class AnnotatorTest extends munit.FunSuite:

  test("annotated game with fishnet input"):
    TestFixtures.testCases.foreach { tc =>
      val (output, expected) = tc.test
      assertEquals(output, expected)
    }
