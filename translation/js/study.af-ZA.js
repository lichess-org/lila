"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.study)window.i18n.study={};let i=window.i18n.study;i['addMembers']="Voeg iemand by";i['addNewChapter']="Voeg \\'n nuwe hoofstuk by";i['allowCloning']="Laat kloning toe";i['allStudies']="Alle studies";i['allSyncMembersRemainOnTheSamePosition']="Alle SYNC lede bly op dieselfde posisie";i['alphabetical']="Alfabeties";i['analysisMode']="Analiseer mode";i['annotateWithGlyphs']="Annoteer met karakters";i['attack']="Aanval";i['automatic']="Outomaties";i['back']="Terug";i['blackIsBetter']="Swart is beter";i['blackIsSlightlyBetter']="Swart is effens beter";i['blackIsWinning']="Swart is beter";i['blunder']="Flater";i['brilliantMove']="Skitterende skuif";i['chapterPgn']="Hoofstuk PGN";i['chapterX']=s("Hoofstuk %s");i['clearAllCommentsInThisChapter']="Vee al die kommentaar, karakters en getekende vorms in die hoofstuk uit?";i['clearAnnotations']="Vee annotasies uit";i['clearChat']="Maak die gesprek skoon";i['clearVariations']="Verwyder variasies";i['cloneStudy']="Kloneer";i['commentThisMove']="Lewer kommentaar op hierdie skuif";i['commentThisPosition']="Lewer kommentaar op hierdie posisie";i['confirmDeleteStudy']=s("Skrap die hele studie? Daar is geen terugkeer nie! Tik die naam van die studie om te bevesting: %s");i['contributor']="Bydraer";i['contributors']="Bydraers";i['copyChapterPgn']="Kopieer PGN";i['counterplay']="Teenstoot";i['createChapter']="Skep \\'n hoofstuk";i['createStudy']="Skep \\'n studie";i['currentChapterUrl']="Huidige hoofstuk URL";i['dateAddedNewest']="Datum bygevoeg (nuutste)";i['dateAddedOldest']="Datum bygevoeg (oudste)";i['deleteChapter']="Vee hoofstuk uit";i['deleteStudy']="Vee die studie uit";i['deleteTheStudyChatHistory']="Vee die gesprek uit? Onthou, jy kan dit nie terug kry nie!";i['deleteThisChapter']="Vee die hoofstuk uit? Jy gaan dit nie kan terugvat nie!";i['development']="Ontwikkeling";i['downloadAllGames']="Laai alle speletjies af";i['downloadGame']="Aflaai spel";i['dubiousMove']="Twyfelagte skuif";i['editChapter']="Verander die hoofstuk";i['editor']="Redakteur";i['editStudy']="Verander studie";i['embedInYourWebsite']="Bed in u webwerf of blog";i['empty']="Leeg";i['enableSync']="Maak sync beskikbaar";i['equalPosition']="Gelyke posisie";i['everyone']="Almal";i['first']="Eerste";i['getAFullComputerAnalysis']="Kry \\'n vol-bediener rekenaar analise van die hooflyn.";i['goodMove']="Goeie skuif";i['hideNextMoves']="Versteek die volgende skuiwe";i['hot']="Gewild";i['importFromChapterX']=s("Voer in vanaf %s");i['initiative']="Inisiatief";i['interactiveLesson']="Interaktiewe les";i['interestingMove']="Interesante skuif";i['inviteOnly']="Slegs op uitnodiging";i['inviteToTheStudy']="Nooi uit om deel te wees van die studie";i['kick']="Verwyder";i['last']="Laaste";i['leaveTheStudy']="Verlaat die studie";i['like']="Hou van";i['loadAGameByUrl']="Laai \\'n wedstryd op deur die URL";i['loadAGameFromPgn']="Laai wedstryd vanaf PGN";i['loadAGameFromXOrY']=s("Laai \\'n wedstryd van %1$s of %2$s");i['loadAPositionFromFen']="Laai posisie vanaf FEN";i['makeSureTheChapterIsComplete']="Maak seker dat die hoofstuk volledig is. Jy kan slegs eenkeer \\'n analise versoek.";i['manageTopics']="Bestuur onderwerpe";i['members']="Lede";i['mistake']="Fout";i['mostPopular']="Mees gewilde";i['myFavoriteStudies']="My gunsteling studies";i['myPrivateStudies']="My privaat studies";i['myPublicStudies']="My publieke studies";i['myStudies']="My studies";i['myTopics']="My onderwerpe";i['nbChapters']=p({"one":"%s Hoofstuk","other":"%s Hoofstukke"});i['nbGames']=p({"one":"%s Wedstryd","other":"%s Wedstryde"});i['nbMembers']=p({"one":"%s Lid","other":"%s Lede"});i['newChapter']="Nuwe hoofstuk";i['newTag']="Nuwe etiket";i['next']="Volgende";i['nextChapter']="Volgende hoofstuk";i['nobody']="Niemand";i['noLetPeopleBrowseFreely']="Nee: laat mense toe om vrylik deur te gaan";i['noneYet']="Nog geen.";i['noPinnedComment']="Geen";i['normalAnalysis']="Normale analise";i['novelty']="Nuwigheid";i['onlyContributorsCanRequestAnalysis']="Slegs die studie bydraers kan versoek om \\'n rekenaar analise te doen.";i['onlyMe']="Net ek";i['onlyMove']="Eenigste skuif";i['onlyPublicStudiesCanBeEmbedded']="Slegs openbare studies kan ingebed word!";i['open']="Maak oop";i['orientation']="Oriëntasie";i['pasteYourPgnTextHereUpToNbGames']=p({"one":"Plak jou PGN teks hier, tot by %s spel","other":"Plak jou PGN teks hier, tot by %s spelle"});i['pgnTags']="PGN etikette";i['pinnedChapterComment']="Vasgepende hoofstuk kommentaar";i['pinnedStudyComment']="Vasgepende studie opmerking";i['playAgain']="Speel weer";i['playing']="Besig om te speel";i['pleaseOnlyInvitePeopleYouKnow']="Nooi asseblief net mense uit wat jy ken of wat aktief wil deelneem aan die studie.";i['popularTopics']="Gewilde onderwerpe";i['prevChapter']="Vorige hoofstuk";i['previous']="Vorige";i['private']="Privaat";i['public']="Publiek";i['readMoreAboutEmbedding']="Lees meer oor inbedding";i['recentlyUpdated']="Onlangs opgedateer";i['rightUnderTheBoard']="Reg onder die bord";i['save']="Stoor";i['saveChapter']="Stoor hoofstuk";i['searchByUsername']="Soek vir gebruikersnaam";i['shareAndExport']="Deel & voer uit";i['shareChanges']="Deel veranderinge met toeskouers en stoor dit op die bediener";i['spectator']="Toeskouer";i['start']="Begin";i['startAtInitialPosition']="Begin by die oorspronklike posisie";i['startAtX']=s("Begin by %s");i['startFromCustomPosition']="Begin vanaf eie posisie";i['startFromInitialPosition']="Begin vanaf oorspronklike posisie";i['studiesCreatedByX']=s("Studies gemaak deur %s");i['studiesIContributeTo']="Studies waartoe ek bydra";i['studyActions']="Studie aksie";i['studyNotFound']="Studie kon nie gevind word nie";i['studyPgn']="Studie PGN";i['studyUrl']="Bestudeer URL";i['theChapterIsTooShortToBeAnalysed']="Die hoofstuk is te kort om geanaliseer te word.";i['timeTrouble']="Tydskommer";i['topics']="Onderwerpe";i['unclearPosition']="Onduidelike posise";i['unlike']="Afkeur";i['unlisted']="Ongelys";i['urlOfTheGame']="URL van die wedstryd";i['visibility']="Sigbaarheid";i['whatAreStudies']="Wat is studies?";i['whatWouldYouPlay']="Wat sal jy in hierdie posisie speel?";i['whereDoYouWantToStudyThat']="Waar wil jy dit bestudeer?";i['whiteIsBetter']="Wit is beter";i['whiteIsSlightlyBetter']="Wit is effens beter";i['whiteIsWinning']="Wit is beter";i['withCompensation']="Met vergoeding";i['withTheIdea']="Met die idee";i['xBroughtToYouByY']=s("%1$s, aan jou beskikbaar gestel deur %2$s");i['yesKeepEveryoneOnTheSamePosition']="Ja: hou almal op dieselfde posisie";i['youAreNowAContributor']="Jy is nou \\'n bydraer";i['youAreNowASpectator']="Jy is nou \\'n toeskouer";i['youCanPasteThisInTheForumToEmbed']="U kan dit in die forum plak om in te bed";i['youCompletedThisLesson']="Geluk! Jy het hierdie les voltooi.";i['zugzwang']="Zugzwang"})()