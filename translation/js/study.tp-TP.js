"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.study)window.i18n.study={};let i=window.i18n.study;i['addMembers']="poka e jan kulupu";i['addNewChapter']="o pali e wan sin";i['allowCloning']="jan li ken ala ken tu e ijo";i['allStudies']="lipu sona ale";i['allSyncMembersRemainOnTheSamePosition']="jan li kepeken e nasin SYNC la, ona ale li lukin e nasin ma sama.";i['alphabetical']="o nasin tan sitelen nimi";i['analysisMode']="nasin pilin";i['annotateWithGlyphs']="o toki kepeken sitelen sin";i['attack']="tawa ni li utala";i['back']="o tawa open";i['blackIsBetter']="poka walo la poka pimeja li pona";i['blackIsSlightlyBetter']="poka walo la poka pimeja li pona lili";i['blackIsWinning']="poka walo la poka pimeja li pona mute";i['blunder']="tawa ni li ike mute";i['brilliantMove']="tawa ni li pona mute";i['chapterPgn']="lipu \\\"PGN\\\" pi wan ni";i['clearChat']="weka e toki lon tomo toki";i['cloneStudy']="o kama jo";i['commentThisMove']="o pana e toki tawa tawa ni";i['commentThisPosition']="o pana e toki tawa nasin ma pi tenpo ni";i['confirmDeleteStudy']=s("sina wile ala wile weka e lipu sona ale? ni li tawa tenpo ale a! o sitelen e nimi pi lipu sona: %s");i['contributor']="jan pali";i['createStudy']="pali e lipu sona";i['currentChapterUrl']="nimi nasin pi wan ni";i['dateAddedNewest']="tenpo kama (sin)";i['dateAddedOldest']="tenpo kama (sin ala)";i['deleteStudy']="o weka e lipu sona";i['downloadAllGames']="o kama jo e lipu ilo pi musi ale";i['downloadGame']="o kama jo e musi";i['dubiousMove']="tawa ni li musi, li ken ike";i['editor']="ilo pi kama ante";i['editStudy']="ante e lipu sona";i['empty']="ala";i['equalPosition']="poka walo en poki pimeja li sama pona";i['everyone']="jan ala";i['first']="o tawa e nanpa wan";i['goodMove']="tawa ni li pona";i['hideNextMoves']="o lukin ala e tawa lon tenpo tawa";i['hot']="suli";i['interestingMove']="tawa ni li musi, li ken pona";i['inviteToTheStudy']="o pana e wile kama tawa jan ante";i['kick']="o weka e jan";i['last']="o tawa monsi";i['leaveTheStudy']="o tawa weka tan nasin sona ni";i['like']="pona tawa mi";i['manageTopics']="o ante e kulupu lipu";i['members']="jan kulupu";i['mistake']="tawa ni li ike";i['mostPopular']="o nasin tan pilin pi jan mute";i['myFavoriteStudies']="lipu sona pona a";i['myPrivateStudies']="lipu sona kulupu ala mi";i['myPublicStudies']="lipu sona kulupu mi";i['myStudies']="pali sona mi";i['myTopics']="kulupu lipu mi";i['nbChapters']=p({"one":"wan %s","other":"wan %s"});i['nbGames']=p({"one":"musi %s","other":"musi %s"});i['nbMembers']=p({"one":"jan kulupu %s","other":"jan kulupu %s"});i['newChapter']="wan sin";i['newTag']="nomo sona sin";i['next']="tawa";i['nextChapter']="wan pi nanpa kama";i['nobody']="jan ala";i['noneYet']="tenpo ni la ala.";i['noPinnedComment']="ala";i['novelty']="tawa sin";i['onlyMe']="mi taso";i['onlyMove']="tawa ni li tawa wan ken taso";i['open']="o tawa";i['orientation']="poka pi sinpin ona";i['pgnTags']="nimi sona pi nasin \\\"PGN\\\"";i['playAgain']="o musi sin";i['playing']="musi";i['pleaseOnlyInvitePeopleYouKnow']="o pali e ni tawa jan ni taso: sina sona e ona. ona li wile kama.";i['popularTopics']="kulupu lipu pi jan mute";i['prevChapter']="wan pi nanpa pini";i['previous']="monsi";i['private']="sina wile e nimi pi sona ala";i['public']="ken lukin tawa jan ante";i['recentlyUpdated']="tenpo lili la ni li sin";i['save']="awen";i['searchByUsername']="o alasa kepeken nimi jan";i['shareAndExport']="o pana tawa jan ante / o pali e lipu ilo";i['spectator']="jan lukin";i['start']="o open";i['startAtX']=s("open lon %s");i['studiesCreatedByX']=s("lipu sona pi jan %s");i['studiesIContributeTo']="mi pana e sona tawa lipu sona ni";i['studyActions']="pali ken pi lipu sona";i['studyPgn']="kama sona e PGN";i['studyUrl']="kama sona e URL";i['theChapterIsTooShortToBeAnalysed']="mi ken ala lukin sona e wan ni tan ni: ona li lili.";i['timeTrouble']="tawa ni li kama tan tenpo lili";i['topics']="kulupu lipu";i['unclearPosition']="musi nasa";i['unlike']="o weka e \\\"pilin pona\\\" mi";i['unlisted']="pi lukin ala tawa jan ale";i['visibility']="lukin ala lukin";i['whatAreStudies']="lipu sona li seme?";i['whatWouldYouPlay']="sina seme lon musi ni?";i['whereDoYouWantToStudyThat']="sina wile kama sona e ni lon ma seme?";i['whiteIsBetter']="poka pimeja la poka walo li pona";i['whiteIsSlightlyBetter']="poka pimeja la poka walo li pona lili";i['whiteIsWinning']="poka pimeja la poka walo li pona mute";i['youAreNowAContributor']="tenpo ni la, sina jan pali pi lipu sona";i['youAreNowASpectator']="tenpo ni la, sina jan lukin pi lipu sona";i['zugzwang']="tawa ale kama li ike"})()