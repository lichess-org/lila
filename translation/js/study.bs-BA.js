"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.study)window.i18n.study={};let i=window.i18n.study;i['addMembers']="Dodajte članove";i['addNewChapter']="Dodajte novo poglavlje";i['allowCloning']="Dozvolite kloniranje";i['allStudies']="Sve studije";i['allSyncMembersRemainOnTheSamePosition']="Svi sinhronizovani članovi ostaju na istoj poziciji";i['alphabetical']="Abecedno";i['analysisMode']="Tip analize";i['annotateWithGlyphs']="Obilježite poteze simbolima";i['attack']="Napad";i['automatic']="Automatska";i['back']="Nazad";i['blackIsBetter']="Crni je bolji";i['blackIsSlightlyBetter']="Crni je u blagoj prednosti";i['blackIsWinning']="Crni dobija";i['blunder']="Grubi previd";i['brilliantMove']="Briljantan potez";i['chapterPgn']="PGN poglavlja";i['chapterX']=s("Poglavlje %s");i['clearAllCommentsInThisChapter']="Da li želite izbrisati sve komentare, simbole i nacrtane oblike u ovom poglavlju?";i['clearAnnotations']="Izbrišite bilješke";i['clearChat']="Izbrišite dopisivanje";i['clearVariations']="Ukloni varijante";i['cloneStudy']="Klonirajte";i['commentThisMove']="Komentirajte ovaj potez";i['commentThisPosition']="Komentirajte ovu poziciju";i['confirmDeleteStudy']=s("Izbrisati cijelu studiju? Nema povratka! Ukucajte naziv studije da potvrdite: %s");i['contributor']="Saradnik";i['contributors']="Saradnici";i['copyChapterPgn']="Kopirajte PGN";i['counterplay']="Protivnapad";i['createChapter']="Kreirajte poglavlje";i['createStudy']="Kreirajte studiju";i['currentChapterUrl']="Link trenutnog poglavlja";i['dateAddedNewest']="Datum dodavanja (najnovije)";i['dateAddedOldest']="Datum dodavanja (najstarije)";i['deleteChapter']="Izbrišite poglavlje";i['deleteStudy']="Izbrišite studiju";i['deleteTheStudyChatHistory']="Da li želite izbrisati svo dopisivanje vezano za ovu studiju? Nakon ove akcije, obrisani tekst se ne može vratiti!";i['deleteThisChapter']="Da li želite izbrisati ovo poglavlje? Nakon ove akcije, poglavlje se ne može vratiti!";i['development']="Razvoj";i['downloadAllGames']="Skinite sve partije";i['downloadGame']="Skini partiju";i['dubiousMove']="Sumnjiv potez";i['editChapter']="Uredite poglavlje";i['editor']="Uređivač";i['editStudy']="Uredite studiju";i['embedInYourWebsite']="Ugradite na Vaš sajt";i['empty']="Prazno";i['enableSync']="Omogućite sinhronizaciju";i['equalPosition']="Jednaka pozicija";i['everyone']="Svi";i['first']="Prva strana";i['getAFullComputerAnalysis']="Dobijte potpunu serversku analizu glavne varijacije.";i['goodMove']="Dobar potez";i['hideNextMoves']="Sakrijte sljedeće poteze";i['hot']="U trendu";i['importFromChapterX']=s("Uvezite iz %s");i['initiative']="Inicijativa";i['interactiveLesson']="Interaktivna lekcija";i['interestingMove']="Zanimljiv potez";i['inviteOnly']="Samo po pozivu";i['inviteToTheStudy']="Pozovite na studiju";i['kick']="Izbaci";i['last']="Posljednja strana";i['leaveTheStudy']="Napustite studiju";i['like']="Sviđa mi se";i['loadAGameByUrl']="Učitajte partiju pomoću linka";i['loadAGameFromPgn']="Učitajte partiju pomoću PGN formata";i['loadAGameFromXOrY']=s("Učitajte partiju sa %1$s ili %2$s");i['loadAPositionFromFen']="Učitajte partiju pomoću FEN koda";i['makeSureTheChapterIsComplete']="Budite sigurni da je poglavlje gotovo. Računarsku analizu možete zahtjevati samo jednom.";i['manageTopics']="Upravljajte temama";i['members']="Članovi";i['mistake']="Greška";i['mostPopular']="Najpopularnije";i['myFavoriteStudies']="Moje omiljene studije";i['myPrivateStudies']="Moje privatne studije";i['myPublicStudies']="Moje javne studije";i['myStudies']="Moje studije";i['myTopics']="Moje teme";i['nbChapters']=p({"one":"%s Poglavlje","few":"%s Poglavlja","other":"%s Poglavlja"});i['nbGames']=p({"one":"%s Partija","few":"%s Partije","other":"%s Partija"});i['nbMembers']=p({"one":"%s Član","few":"%s Člana","other":"%s Članova"});i['newChapter']="Novo poglavlje";i['newTag']="Nova oznaka";i['next']="Sljedeća strana";i['nextChapter']="Sljedeće poglavlje";i['nobody']="Niko";i['noLetPeopleBrowseFreely']="Ne: Dozvolite ljudima da slobodno pregledaju";i['noneYet']="Još nijedna.";i['noPinnedComment']="Nijedan";i['normalAnalysis']="Normalna analiza";i['novelty']="Nov potez";i['onlyContributorsCanRequestAnalysis']="Samo saradnici u studiji mogu zahtijevati računarsku analizu.";i['onlyMe']="Samo ja";i['onlyMove']="Jedini potez";i['onlyPublicStudiesCanBeEmbedded']="Samo javne studije mogu biti ugrađene!";i['open']="Otvorite";i['orientation']="Orijentacija";i['pasteYourPgnTextHereUpToNbGames']=p({"one":"Ovdje zalijepite svoj PGN tekst, do %s partije","few":"Ovdje zalijepite svoj PGN tekst, do %s partije","other":"Ovdje zalijepite svoj PGN tekst, do %s partija"});i['pgnTags']="PGN oznake";i['pinnedChapterComment']="Stalni komentar poglavlja";i['pinnedStudyComment']="Stalni komentar studije";i['playAgain']="Igrajte ponovo";i['playing']="U toku";i['pleaseOnlyInvitePeopleYouKnow']="Molimo Vas da pozovete samo ljude koje znate i koji su zainteresovani da aktivno učustvuju u ovoj studiji.";i['popularTopics']="Popularne teme";i['prevChapter']="Prethodno poglavlje";i['previous']="Prethodna strana";i['private']="Privatna";i['public']="Javna";i['readMoreAboutEmbedding']="Pročitajte više o ugrađivanju";i['recentlyUpdated']="Nedavno ažurirane";i['rightUnderTheBoard']="Odmah ispod ploče";i['save']="Sačuvaj";i['saveChapter']="Sačuvajte poglavlje";i['searchByUsername']="Pretraga prema korisničkom imenu";i['shareAndExport']="Podijelite i izvezite";i['shareChanges']="Podijelite promjene sa posmatračima i sačuvajte ih na server";i['showEvalBar']="Evaluacijske trake";i['spectator']="Posmatrač";i['start']="Pokreni";i['startAtInitialPosition']="Krenite sa inicijalnom pozicijom";i['startAtX']=s("Krenite sa %s");i['startFromCustomPosition']="Krenite sa željenom pozicijom";i['startFromInitialPosition']="Krenite sa inicijalnom pozicijom";i['studiesCreatedByX']=s("Studije koje je kreirao/la %s");i['studiesIContributeTo']="Studije kojima doprinosim";i['studyActions']="Opcije za studiju";i['studyNotFound']="Studija nije pronađena";i['studyPgn']="Studirajte PGN";i['studyUrl']="Link studije";i['theChapterIsTooShortToBeAnalysed']="Poglavlje je prekratko za analizu.";i['timeTrouble']="Cajtnot";i['topics']="Teme";i['unclearPosition']="Nejasna pozicija";i['unlike']="Ne sviđa mi se";i['unlisted']="Neizlistane";i['urlOfTheGame']="Link partije";i['visibility']="Vidljivost";i['whatAreStudies']="Šta su studije?";i['whatWouldYouPlay']="Šta biste odigrali u ovoj poziciji?";i['whereDoYouWantToStudyThat']="Gdje želite da ovu poziciju prostudirate?";i['whiteIsBetter']="Bijeli je bolji";i['whiteIsSlightlyBetter']="Bijeli je u blagoj prednosti";i['whiteIsWinning']="Bijeli dobija";i['withCompensation']="S kompenzacijom";i['withTheIdea']="S idejom";i['xBroughtToYouByY']=s("%1$s vam je donio %2$s");i['yesKeepEveryoneOnTheSamePosition']="Da: zadržite sve na istoj poziciji";i['youAreNowAContributor']="Sada ste saradnik";i['youAreNowASpectator']="Sada ste posmatrač";i['youCanPasteThisInTheForumToEmbed']="Možete ovo zalijepiti na forumu ili Vašem blogu na Lichessu kako biste ugradili poglavlje";i['youCompletedThisLesson']="Čestitamo! Kompletirali ste ovu lekciju.";i['zugzwang']="Iznudica"})()