"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.dgt)window.i18n.dgt={};let i=window.i18n.dgt;i['boardWillAutoConnect']="Дошка аўтаматычна падключыцца да любой бягучай ці новай партыі. Магчымасць выбіраць партыю для падключэння з’явіцца неўзабаве.";i['configurationSection']="Раздзеле Канфігурацыі";i['configure']="Канфігурацыя";i['debug']="Адладка";i['dgtBoard']="DGT дошка";i['dgtBoardConnectivity']="Злучэнне з DGT дошкай";i['dgtBoardLimitations']="Абмежаванні DGT дошкі";i['dgtBoardRequirements']="Патрабаванні да DGT дошкі";i['dgtConfigure']="DGT - Канфігурацыя";i['downloadHere']=s("Вы можаце спампаваць праграмнае забеспячэнне тут: %s.");i['ifLiveChessRunningElsewhere']=s("Калі %1$s запушчана на іншым камп\\'ютары ці на іншым порце, вы павінны задаць IP адрас і порт тут у %2$s.");i['ifLiveChessRunningOnThisComputer']=s("Калі %1$s запушчана на гэтым камп’ютары, вы можаце праверыць злучэнне з нім %2$s.");i['ifMoveNotDetected']="Калі ход не быў распазнаны";i['keepPlayPageOpen']="Старонка гульні павінна ўвесь час быць адкрыта ў браўзеры. Неабвязкова, каб яна была бачная: вы можаце яе паменьшыць ці адкрыць побач з партыяй Lichess, але не закрывайце яе, інакш дошка спыніць працу.";i['keywords']="Ключавыя словы";i['lichessAndDgt']="Lichess & DGT";i['lichessConnectivity']="Злучэнне з Lichess";i['openingThisLink']="адкрываючы гэтую спасылку";i['playWithDgtBoard']="Гуляць на дошцы DGT";i['reloadThisPage']="Абнавіць гэту старонку";i['textToSpeech']="Сінтэз маўлення";i['thisPageAllowsConnectingDgtBoard']="Гэтая старонка дазваляе падключыць вашу DGT дошку да Lichess і выкарыстоўваць яе ў гульнях.";i['timeControlsForCasualGames']="Абмежаванне часу для бязрэйтынгавых гульняў: толькі класічныя, па ліставанні і рапід.";i['timeControlsForRatedGames']="Абмежаванне часу для рэйтынгавых гульняў: класічныя, па ліставанні і некаторыя рапіды, уключаючы 15+10 і 20+0";i['toConnectTheDgtBoard']=s("Каб падключыцца да электроннай DGT дошкі вам спатрэбіцца ўсталяваць %s.");i['verboseLogging']="Падрабязны часопіс";i['webSocketUrl']=s("Адрас WebSocket для %s");i['whenReadySetupBoard']=s("Калі ўсе будзе гатова, наладзьце вашу дошку і націсніце %s.")})()