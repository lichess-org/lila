package lila.evaluation

import chess.Centis

import Statistics.*

class StatisticsTest extends munit.FunSuite:
  test("be highly consistent (1)"):
    assert(
      cvIndicatesHighlyFlatTimes(
        moveTimeCoefVariation(
          List(0, 560, 464, 496, 504, 352, 344, 392, 336, 448, 304, 336, 280, 328, 352, 272, 376, 352, 376,
            304, 264, 264, 296, 368, 312, 384, 328, 352, 368, 328, 392, 488, 384, 296, 408, 432, 228).map(
            Centis(_)
          )
        ).get
      )
    )

  test("be highly consistent (2)"):
    assert(
      cvIndicatesHighlyFlatTimes(
        moveTimeCoefVariation(
          List(0, 440, 392, 384, 304, 392, 424, 488, 432, 488, 496, 464, 344, 456, 320, 344, 520, 336, 424,
            560, 496, 512, 504, 392, 392, 504, 344, 408, 432, 480, 176, 334).map(Centis(_))
        ).get
      )
    )

  test("be highly consistent (3)"):
    assert(
      cvIndicatesHighlyFlatTimes(
        moveTimeCoefVariation(
          List(0, 312, 416, 520, 408, 520, 424, 456, 472, 536, 464, 664, 496, 448, 640, 856, 600, 448, 480,
            560, 536, 568, 464, 416, 616).map(Centis(_))
        ).get
      )
    )

  test("be highly consistent (4)"):
    assert(
      cvIndicatesHighlyFlatTimes(
        moveTimeCoefVariation(
          List(0, 384, 312, 432, 368, 336, 320, 360, 344, 592, 536, 552, 432, 392, 504, 520, 448, 584, 456,
            280, 560, 616, 408, 632, 416, 460).map(Centis(_))
        ).get
      )
    )

  test("not be highly consistent (1)"):
    assert(
      !cvIndicatesHighlyFlatTimes(
        moveTimeCoefVariation(
          List(0, 96, 120, 120, 64, 80, 120, 168, 56, 360, 120, 208, 128, 536, 248, 464, 104, 296, 200, 112,
            104, 136, 216, 304, 192, 184, 240, 176, 424, 656, 112, 160, 256, 288, 256, 105).map(Centis(_))
        ).get
      )
    )

  test("not be highly consistent (2)"):
    assert(
      !cvIndicatesHighlyFlatTimes(
        moveTimeCoefVariation(
          List(0, 784, 136, 72, 40, 296, 80, 96, 56, 80, 152, 72, 112, 472, 576, 0, 456, 416, 288, 216, 224,
            202).map(Centis(_))
        ).get
      )
    )

  test("not be highly consistent (3)"):
    assert(
      !cvIndicatesHighlyFlatTimes(
        moveTimeCoefVariation(
          List(0, 176, 224, 72, 80, 120, 80, 128, 136, 120, 400, 240, 112, 128, 232, 208, 136, 184, 264, 216,
            144, 144, 224, 122).map(Centis(_))
        ).get
      )
    )

  test("not be highly consistent (4)"):
    assert(
      !cvIndicatesHighlyFlatTimes(
        moveTimeCoefVariation(
          List(0, 56, 0, 8, 144, 64, 32, 80, 168, 136, 0, 80, 96, 80, 88, 80, 0, 64, 120, 104, 56, 64, 72, 64,
            64, 72, 56, 144, 80, 72, 88, 112, 56, 48, 152, 94).map(Centis(_))
        ).get
      )
    )
