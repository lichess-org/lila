"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.study)window.i18n.study={};let i=window.i18n.study;i['addMembers']="അംഗങ്ങളെ ചേര്‍ക്കുക";i['addNewChapter']="പുതിയ അധ്യായം തുടങ്ങുക";i['allowCloning']="ക്ളോണിങ് സാധ്യമാക്കുക";i['allStudies']="എല്ലാ പഠനങ്ങളും";i['allSyncMembersRemainOnTheSamePosition']="എല്ലാ sync ചെയ്ത അംഗങ്ങളും ഒരേ പൊസിഷനിൽ സ്ഥിതി ചെയ്യുക";i['alphabetical']="അക്ഷരക്രമത്തിലുള്ള";i['analysisMode']="പരിശോധന മോഡ്";i['annotateWithGlyphs']="ഗ്ലിഫ്സ് ഉപയോഗിച്ചു കുറിപ്പെഴുതുക";i['attack']="കയ്യേറ്റം";i['automatic']="സ്വയമേ";i['back']="തിരിച്ച്";i['blackIsWinning']="കറുത്ത കളിക്കാരൻ ജയിക്കുന്നു";i['blunder']="അബദ്ധം";i['brilliantMove']="വളരെ നല്ല നീക്കം";i['chapterPgn']="PGN പാഠം";i['chapterX']=s("അധ്യായം %s");i['clearAllCommentsInThisChapter']="ഈ അധ്യായത്തിലെ എല്ലാ കമന്റുകളും രൂപങ്ങളും നീക്കം ചെയ്യണോ?";i['clearAnnotations']="അനോടേഷൻ ഇല്ലാതാക്കുക";i['clearChat']="ചാറ്റ് അപ്രത്യക്ഷമാക്കുക";i['cloneStudy']="ക്ലോൺ";i['commentThisMove']="ഈ നീക്കം കമന്റ് ചെയ്യുക";i['commentThisPosition']="ഈ പൊസിഷനു കമന്റ് ചെയ്യുക";i['confirmDeleteStudy']=s("മൊത്തം പഠനം കളയണോ? ഇതിൽ നിന്ന് തിരിച്ച് പേവ്വാൻ ഒക്കത്തില്ല! ഉറപ്പിക്കാൻ ഈ പഠനത്തിന്റെ പേര് എഴുത്തുക: %s");i['contributor']="സംഭാവകന്‍";i['contributors']="സംഭാവകർ";i['copyChapterPgn']="PGN പകർത്തുക";i['counterplay']="പ്രത്യാക്രമണം";i['createChapter']="അദ്ധ്യായം നിർമിക്കുക";i['createStudy']="പാഠം നിർമിക്കുക";i['currentChapterUrl']="നിലവിലുള്ള പാഠത്തിന്റെ URL";i['dateAddedNewest']="ചേർത്ത തീയതി (പുതിയത്)";i['dateAddedOldest']="ചേർത്ത തീയതി (പഴയത്)";i['deleteChapter']="അധ്യായം നീക്കം ചെയ്യുക";i['deleteStudy']="പഠനം നീക്കം ചെയ്യുക";i['deleteTheStudyChatHistory']="അധ്യായത്തിന്റെ ചാറ്റ് ഹിസ്റ്ററി നീക്കം ചെയ്യണോ? പിന്നെ തിരിച്ചു പോകാൻ പറ്റില്ല!";i['deleteThisChapter']="ഈ അധ്യായം നീക്കം ചെയ്യണോ? പിന്നെ തിരിച്ചു പോകാൻ പറ്റില്ല ട്ടോ!";i['development']="വികസനം";i['downloadAllGames']="എല്ലാ ഗെയിമും ഡൌണ്‍ലോഡ് ചെയ്യുക";i['downloadGame']="ഗെയിം ഡൌണ്‍ലോഡ് ചെയ്യുക";i['dubiousMove']="ചഞ്ചലമായ നീക്കം";i['editChapter']="പാഠം തിരുത്തുക";i['editor']="എഡിറ്റർ";i['editStudy']="പാഠം തിരുത്തുക";i['empty']="ശൂന്യമാക്കുക";i['enableSync']="Sync പ്രാപ്തമാക്കുക";i['equalPosition']="ഒരുപോലെയുള്ള കരുനില";i['everyone']="എല്ലാവരും";i['first']="ഒന്നാമത്തെ";i['getAFullComputerAnalysis']="സെർവർ-സൈഡിൽ ഉള്ള കമ്പ്യൂട്ടറിന്റെ സഹായത്താൽ പ്രധാന ലൈനിന്റെ പൂർണ വിശകലനം ലഭിക്കുക.";i['goodMove']="നല്ല നീക്കം";i['hideNextMoves']="അടുത്ത നീക്കം മറയ്ക്കുക";i['hot']="ചൂടുള്ള";i['importFromChapterX']=s("%s-നിന്ന് ഇറക്കുമതിക്കുക");i['initiative']="മുൻകൈയെടുക്കൽ";i['interactiveLesson']="പാരസ്പര്യ ശാസനം";i['interestingMove']="താൽപര്യമുണർത്തുന്ന നീക്കം";i['inviteOnly']="ക്ഷണിക്കപ്പെട്ടവർ മാത്രം";i['inviteToTheStudy']="പഠനത്തിന് ക്ഷണിക്കുക";i['kick']="പുറത്താക്കുക";i['last']="അവസാനത്തെ";i['leaveTheStudy']="പഠനം വിടുക";i['like']="ലൈക്ക്";i['loadAGameByUrl']="URL ഉപയോഗിച്ചു കളി ലോഡ് ചെയ്യുക";i['loadAGameFromPgn']="PGN-ൽ നിന്നും കളി ലോഡ് ചെയ്യുക";i['loadAGameFromXOrY']=s("%1$s-ൽ നിന്നോ %2$s-ൽ നിന്നോ കളി ലോഡ് ചെയ്യുക");i['loadAPositionFromFen']="FEN-ൽ നിന്നും പൊസിഷൻ ലോഡ് ചെയ്യുക";i['makeSureTheChapterIsComplete']="പാഠം മുഴുവനായെന്ന് ഉറപ്പു വരുത്തുക. നിങ്ങൾക്ക് ഒരിക്കൽ മാത്രമേ വിശകലനത്തിന് അപേക്ഷിക്കാൻ സാധിക്കുകയുള്ളൂ.";i['manageTopics']="വിശയങ്ങൾ നിയന്ത്രക്കുക";i['members']="അംഗങ്ങള്‍";i['mistake']="തെറ്റു്";i['mostPopular']="ഏറ്റവും ജനകീയമായത്";i['myFavoriteStudies']="എന്റെ പ്രിയപ്പെട്ട പഠനങ്ങൾ";i['myPrivateStudies']="എന്‍റെ സ്വകാര്യ പഠനങ്ങൾ";i['myPublicStudies']="എന്റെ പൊതുപഠനങ്ങൾ";i['myStudies']="എന്റെ പഠനങ്ങൾ";i['myTopics']="എന്റെ വിശയങ്ങൾ";i['nbChapters']=p({"one":"%s അധ്യായം","other":"%s അധ്യായങ്ങള്‍"});i['nbGames']=p({"one":"%s കളി","other":"%s കളികള്‍"});i['nbMembers']=p({"one":"%s അംഗം","other":"%s അംഗങ്ങള്‍"});i['newChapter']="പുതിയ അദ്ധ്യായം";i['newTag']="പുതിയ ടാഗ്";i['next']="അടുത്തത്";i['nextChapter']="അടുത്ത പാഠം";i['nobody']="ആരുമില്ല";i['noLetPeopleBrowseFreely']="അല്ല; ആളുകൾക്ക് സ്വതന്ത്രമായി തിരയാൻ സമ്മതിക്കുക";i['noneYet']="ഇതുവരെ ഒന്നുമില്ല.";i['noPinnedComment']="ഒന്നുമില്ല";i['normalAnalysis']="സാധാരണ വിശകലനം";i['novelty']="പുതുമ";i['onlyContributorsCanRequestAnalysis']="പഠനത്തിൽ സംഭാവന ചെയ്തവർക്ക് മാത്രമേ കമ്പ്യൂട്ടർ വിശകലനം ചെയ്യാൻ സാധിക്കുകയുള്ളൂ.";i['onlyMe']="ഞാൻ മാത്രം";i['onlyMove']="ഒറ്റ നീക്കം";i['open']="തുറക്കുക";i['orientation']="വിന്യാസം";i['pasteYourPgnTextHereUpToNbGames']=p({"one":"നിങ്ങളുടെ PGN ടെക്സ്റ്റ് ഇവിടെ പേസ്റ്റ് ചെയ്യുക, %s കളി വരെ","other":"നിങ്ങളുടെ PGN ടെക്സ്റ്റ് ഇവിടെ പേസ്റ്റ് ചെയ്യുക, %s കളികൾ വരെ"});i['pgnTags']="PGN ടാഗുകൾ";i['pinnedChapterComment']="അധ്യായത്തിലെ കമന്റ് പിൻ ചെയ്യുക";i['pinnedStudyComment']="പിൻ ചെയ്ത പഠന കമന്റ്";i['playAgain']="പിന്നേയും കളിക്കുക";i['playing']="കളിക്കുന്നു";i['pleaseOnlyInvitePeopleYouKnow']="ദയവായി നിങ്ങൾക്ക് പരിചയമുള്ള, നിങ്ങളുടെ പഠനത്തിൽ പങ്കെടുക്കുവാൻ താത്പര്യമുള്ള ആളുകളെ മാത്രം ക്ഷണിക്കുക.";i['popularTopics']="ജനപ്രീയമായ വിശയങ്ങൾ";i['prevChapter']="കഴിഞ്ഞ പാഠം";i['previous']="മുൻപുള്ള";i['private']="സ്വകാര്യം";i['public']="പൊതുവായത്";i['recentlyUpdated']="സമീപകാലത്തു പുതുക്കിയത്";i['rightUnderTheBoard']="ബോർഡിന് നേരെ താഴെ";i['save']="സംരക്ഷിക്കുക";i['saveChapter']="അദ്ധ്യായം സംരക്ഷിക്കുക";i['searchByUsername']="യൂസര്‍നെയിം വച്ച് തിരയുക";i['shareAndExport']="&amp പങ്കുവെക്കുക; എക്സ്പോർട്ട്";i['shareChanges']="മാറ്റങ്ങൾ സെർവറിൽ സേവ് ചെയ്ത് അവ കാണികൾക്ക് പങ്കുവെക്കുക";i['showEvalBar']="വിലയിരുത്തൽ വരികൾ";i['spectator']="കാഴ്ചക്കാര്‍";i['start']="തുടങ്ങുക";i['startAtInitialPosition']="ആദ്യ പൊസിഷനില്‍ തുടങ്ങുക";i['startAtX']=s("%s-ൽ തുടങ്ങുക");i['startFromCustomPosition']="കസ്റ്റം പൊസിഷനില്‍ തുടങ്ങുക";i['startFromInitialPosition']="ആദ്യ പൊസിഷനില്‍ തുടങ്ങുക";i['studiesCreatedByX']=s("%s നിർമിച്ച പഠനങ്ങൾ");i['studiesIContributeTo']="ഞാൻ സംഭാവന ചെയ്യുന്ന പഠനങ്ങൾ";i['studyNotFound']="പാഠം കണ്ടെത്തിയില്ല";i['studyPgn']="PGN പഠിക്കുക";i['studyUrl']="URL പഠിക്കുക";i['theChapterIsTooShortToBeAnalysed']="ഈ പാഠം വിശകലനം ചെയ്യാൻ പറ്റാത്ത വിധം ചെറുതാണ്.";i['topics']="വിശയങ്ങൾ";i['unclearPosition']="അവ്യക്തമായ കരുനില";i['unlike']="ഇഷ്ടപെട്ടില്ല";i['unlisted']="ലിസ്റ്റ് ചെയ്യപ്പെടാത്തത്";i['urlOfTheGame']="കളിയുടെ URL";i['visibility']="ദൃശ്യത";i['whatAreStudies']="എന്താണ് പഠനങ്ങൾ?";i['whatWouldYouPlay']="താങ്ങൾ ഈ കരുനിലയിൽ എന്ത് ചെയ്യും?";i['whereDoYouWantToStudyThat']="താങ്ങൾക്കു് ഇതു് എവിടേ പഠിക്കണം?";i['whiteIsWinning']="വെള്ള കളിക്കാരൻ ജയിക്കുന്നു";i['withCompensation']="നഷ്ടപരിഹാരം കൂടേ";i['xBroughtToYouByY']=s("%1$s നിങ്ങള്ക്ക് കൊണ്ടു വന്നത് %2$s");i['yesKeepEveryoneOnTheSamePosition']="അതെ; എല്ലാവരെയും ഒരേ പൊസിഷനിൽ നിറുത്തുക";i['youAreNowAContributor']="നിങ്ങൾ ഇപ്പോൾ ഒരു സംഭാവകൻ ആയിരിക്കുന്നു";i['youAreNowASpectator']="നിങ്ങള്‍ ഇപ്പോള്‍ ഒരു കാണിയാണ്";i['youCompletedThisLesson']="അഭിനന്ദനങ്ങൾ! താങ്ങൾ ഈ പാഠം പൂർത്തീകരിച്ചിരിക്കുന്നു."})()