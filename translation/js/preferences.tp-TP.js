"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.preferences)window.i18n.preferences={};let i=window.i18n.preferences;i['boardCoordinates']="o lukin e nimi pi lon mute";i['boardHighlights']="o lukin e pali pini";i['bothClicksAndDrag']="ale";i['castleByMovingOntoTheRook']="o tawa e lawa mije tawa ma pi tomo utala";i['castleByMovingTheKingTwoSquaresOrOntoTheRook']="o tawa e tomo utala, e lawa mije lon tenpo tawa sama kepeken nasin seme?";i['castleByMovingTwoSquares']="o tawa e lawa mije lon ma pi weka tu";i['chessClock']="ilo tenpo";i['chessPieceSymbol']="sitelen pi ijo musi";i['claimDrawOnThreefoldRepetitionAutomatically']="tawa li sama lon tawa tu wan la o sama e musi";i['clickTwoSquares']="o pilin e selo tu";i['confirmResignationAndDrawOffers']="o toki e ni: sina wile pini ike anu sina wile sama e musi";i['correspondenceAndUnlimited']="musi pi tenpo suli en musi pi tenpo ale";i['displayBoardResizeHandle']="o pana lukin e ilo pi ante suli pi supa musi";i['dragPiece']="o tawa e ijo";i['explainPromoteToQueenAutomatically']="jan utala li ante la sina ken pali e ni: <ctrl>. ni la ona li ante ala e lawa meli. sina ken ante e ona";i['explainShowPlayerRatings']="sina ken weka e nanpa wawa ale kepeken ni. ni la sina ken lawa pona mute. musi li nanpa kin taso sina lukin ala e nanpa wawa.";i['gameBehavior']="nasin pali musi";i['giveMoreTime']="o pana e tenpo";i['horizontalGreenProgressBars']="laso jelo";i['howDoYouMovePieces']="sina tawa seme e ijo?";i['inCasualGamesOnly']="taso lon musi pi nanpa ala";i['inCorrespondenceGames']="musi pi tenpo suli";i['inputMovesWithTheKeyboard']="ilo sitelen la sina sitelen e pali";i['materialDifference']="ante pi nanpa ijo";i['moveConfirmation']="sina wile tawa la o sina wile toki e ni pi tenpo tu";i['moveListWhilePlaying']="lipu pi pali pi musi ni";i['notifications']="toki kama";i['notifyWeb']="ilo";i['onlyOnInitialPosition']="lon tenpo open taso";i['pgnLetter']="sitelen nimi (K, Q, R, B, N)";i['pgnPieceNotation']="nasin sitelen pi tawa ijo";i['pieceAnimation']="nasin lukin pi tawa ijo";i['pieceDestinations']="o lukin e pali ken";i['preferences']="wile sina pi lipu ni";i['premovesPlayingDuringOpponentTurn']="tawa lon tenpo kama (sina tawa e ijo lon tenpo musi pi jan ante)";i['promoteToQueenAutomatically']="o kama e lawa meli la ilo sona li tawa";i['sayGgWpAfterLosingOrDrawing']="mi pini ike anu pini pi anpa ala e musi la, o toki \\\"musi pona, sina pona\\\" (toki Inli)";i['scrollOnTheBoardToReplayMoves']="ilo sike li tawa la o lukin sin e tawa";i['showPlayerRatings']="o pana lukin e nanpa wawa jan";i['snapArrowsToValidMoves']="mi sitelen e palisa lon supa musi la, o pali ala pali e ni: palisa li nasin tawa tawa ken taso?";i['soundWhenTimeGetsCritical']="tenpo li lili la, o kalama";i['takebacksWithOpponentApproval']="o weka e tawa (la jan ante li pilin pona)";i['tenthsOfSeconds']="o pana lukin e wan lili pi tenpo Sekunta";i['whenPremoving']="tenpo sama la jan li pilin e tawa pi tenpo kama";i['whenTimeRemainingLessThanTenSeconds']="tenpo tawa li lili tawa tenpo Sekunta 10";i['whenTimeRemainingLessThanThirtySeconds']="tenpo tawa li lili tawa tenpo Sekunta 30";i['yourPreferencesHaveBeenSaved']="wile sina li awen.";i['zenMode']="nasin lukin pi musi taso"})()