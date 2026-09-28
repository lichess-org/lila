package lila.practice

import lila.core.i18n.I18nKey

private object PracticeSections:

  import I18nKey.practice.*

  val list = List(
    PracticeSection(
      name = secHeadCheckmates,
      id = "checkmates",
      studies = List(
        study("BJy6fEDf", stNamPieceCheckmatesI, stDesBasicCheckmates),
        study("fE4k21MW", stNamCheckmatePatternsI, stDesRecognizeThePatterns),
        study("8yadFPpU", stNamCheckmatePatternsII, stDesRecognizeThePatterns),
        study("PDkQDt6u", stNamCheckmatePatternsIII, stDesRecognizeThePatterns),
        study("96Lij7wH", stNamCheckmatePatternsIV, stDesRecognizeThePatterns),
        study("Rg2cMBZ6", stNamPieceCheckmatesII, stDesChallengingCheckmates),
        study("ByhlXnmM", stNamKnightAndBishopMate, stDesInteractiveLesson)
      )
    ),
    PracticeSection(
      name = secHeadFundamentalTactics,
      id = "fundamental-tactics",
      studies = List(
        study("9ogFv8Ac", stNamThePin, stDesPinItToWinIt),
        study("tuoBxVE5", stNamTheSkewer, stDesYumSkewers),
        study("Qj281y1p", stNamTheFork, stDesUseTheForkLuke),
        study("MnsJEWnI", stNamDiscoveredAttacks, stDesIncludingDiscoveredChecks),
        study("RUQASaZm", stNamDoubleCheck, stDesAVeryPowerfulTactic),
        study("o734CNqp", stNamOverloadedPieces, stDesTheyHaveTooMuchWork),
        study("ITWY4GN2", stNamZwischenzug, stDesInBetweenMoves),
        study("lyVYjhPG", stNamXRay, stDesAttackingThroughAnEnemyPiece)
      )
    ),
    PracticeSection(
      name = secHeadAdvancedTactics,
      id = "advanced-tactics",
      studies = List(
        study("9cKgYrHb", stNamZugzwang, stDesBeingForcedToMove),
        study("g1fxVZu9", stNamInterference, stDesInterposeAPieceToGreatEffect),
        study("s5pLU7Of", stNamGreekGift, stDesStudyTheGreekGiftSacrifice),
        study("kdKpaYLW", stNamDeflection, stDesDistractingADefender),
        study("jOZejFWk", stNamAttraction, stDesLureAPieceToABadSquare),
        study("49fDW0wP", stNamUnderpromotion, stDesPromoteButNotToAQueen),
        study("0YcGiH4Y", stNamDesperado, stDesAPieceIsLostButItCanStillHelp),
        study("CgjKPvxQ", stNamCounterCheck, stDesRespondToACheckWithACheck),
        study("udx042D6", stNamUndermining, stDesRemoveTheDefendingPiece),
        study("Grmtwuft", stNamClearance, stDesGetOutOfTheWay)
      )
    ),
    PracticeSection(
      name = secHeadPawnEndgames,
      id = "pawn-endgames",
      studies = List(
        study("xebrDvFe", stNamKeySquares, stDesReachAKeySquare),
        study("A4ujYOer", stNamOpposition, stDesTakeTheOpposition),
        study("pt20yRkT", stNam7thRankRookPawn, stDesVersusAQueen)
      )
    ),
    PracticeSection(
      name = secHeadRookEndgames,
      id = "rook-endgames",
      studies = List(
        study("MkDViieT", stNam7thRankRookPawn, stDesAndPassiveRookVsRook),
        study("pqUSUw8Y", stNamBasicRookEndgames, stDesLucenaAndPhilidor),
        study("heQDnvq7", stNamIntermediateRookEndings, stDesBroadenYourKnowledge),
        study("wS23j5Tm", stNamPracticalRookEndings, stDesRookEndingsWithSeveralPawns)
      )
    )
  )

  private def study(id: String, name: I18nKey, desc: I18nKey) =
    PracticeStudy(
      id = StudyId(id),
      name = name,
      desc = desc,
      chapters = Nil // Chapters will be filled later
    )
