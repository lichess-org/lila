"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.faq)window.i18n.faq={};let i=window.i18n.faq;i['accounts']="lipu sina";i['aHourlyBulletTournament']="utala musi pi nasin Bullet pi tenpo Ola";i['areThereWebsitesBasedOnLichess']="pali ni li lon anu seme: ni li pali kepeken ilo Lichess?";i['breakdownOfOurCosts']="sona pi esun mi";i['canIChangeMyUsername']="mi ken ala ken ante e nimi jan mi?";i['configure']="ante";i['eightVariants']="nasin ante 8.";i['fairPlay']="musi pona";i['fairPlayPage']="lipu musi sama";i['faqAbbreviation']="lipu pi wile sona";i['fideHandbookX']=s("lipu %s lon lipu pi kulupu FIDE");i['findMoreAndSeeHowHelp']=s("sina ken sona mute e %1$s (e %2$s). sina wile pona e Lichess kepeken tenpo sina en pona ilo sina la, %3$s mute li lon.");i['frequentlyAskedQuestions']="lipu pi wile sona";i['gameplay']="musi";i['goodIntroduction']="pana pona pi sona lili";i['guidelines']="nasin pi nimi jan";i['hearItPronouncedBySpecialist']="o kute e jan sona. ona li toki e nimi ni.";i['howCanIBecomeModerator']="nasin seme la mi ken kama jan sewi?";i['howCanIContributeToLichess']="mi ken pona seme e lipu Lichess?";i['howToThreeDots']="...nasin seme?";i['isCorrespondenceDifferent']="musi pi tenpo suli li sama ala musi pi ante ala, anu seme?";i['leechess']="\\\"litesi\\\"";i['lichessCombinationLiveLightLibrePronounced']=s("nimi Lichess li kulupu pi nimi ni: \\\"live/light/libre\\\" en \\\"chess\\\". o toki %1$s e ni.");i['lichessPoweredByDonationsAndVolunteers']="lipu Lichess li kama wawa tan ni: jan li pana e mani tawa ni. jan wile li pali lon lipu Lichess.";i['lichessRatings']="nanpa wawa pi ilo Lichess";i['lichessSupportChessAnd']=s("sina ken musi e nasin pi ante ala e %1$s.");i['lichessTraining']="kama sona pi ilo Lichess";i['noUpperCaseDot']="ala.";i['otherWaysToHelp']="nasin pona ante";i['pleaseReadFairPlayPage']=s("tawa sona suli, o lukin mi %s");i['positions']="lon";i['preventLeavingGameWithoutResigning']="sina seme tawa ni: jan li weka ni tawa musi: ona li toki ala e \\\"mi anpa\\\"?";i['ratingLeaderboards']="lipu nanpa wawa";i['threefoldRepetition']="sama supa pi tenpo tu wan";i['threefoldRepetitionExplanation']=s("Supa li sama lon tenpo tu wan la jan li ken kama jo e pini sama tan %1$s. ilo Lichess li kepeken nasin lon pi kulupu FIDE. nasin ni li lon lipu Article 9.2 lon %2$s.");i['threefoldRepetitionLowerCase']="sama supa pi tenpo tu wan";i['titlesAvailableOnLichess']="nimi jan seme pi sona musi li lon ilo Lichess?";i['usernamesNotOffensive']=s("nimi jan o ike ala. nimi jan o sama lukin ala nimi pi jan ante. nimi jan o wile esun ala. %1$s la sina ken lukin e sitelen.");i['wayOfBerserkExplanation']=s("jan \\\"hiimgosu\\\" li kama jo e nimi wawa ni tan ni: ona li pini pona e musi ale lon %s kepeken nasin pi tenpo lili.");i['whatIsACPL']="nanpa insa CPL (ACPL) li seme?";i['whatUsernameCanIchoose']="nimi jan mi li ken seme?";i['whatVariantsCanIplay']="mi ken musi e musi ante seme?";i['whenAmIEligibleRatinRefund']="tenpo seme la mi ken kama jo e nanpa wawa sin tan jan pi musi ike?";i['whyIsLichessCalledLichess']="nimi pi lipu ni li \\\"Lichess\\\" tan seme?";i['whyIsLilaCalledLila']=s("sama la nimi pi toki ilo tan li \\\"%1$s\\\" tan ni: kon nimi ni li \\\"li[chess in sca]la\\\". jan li sitelen mute e lipu Lichess kepeken toki ilo %2$s. ona li pona tawa kama sona.");i['whyLiveLightLibre']="nimi \\\"live\\\" li lon tan ni: musi li lon lon tenpo suno ale. nimi \\\"light\\\" en nimi \\\"libre\\\" li lon tan ni: lipu Lichess li ijo pi tan open li jo ala e ijo ike pi esun lawa. lipu ante la ijo ike ni li lon."})()