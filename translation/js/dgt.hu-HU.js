"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.dgt)window.i18n.dgt={};let i=window.i18n.dgt;i['announceAllMoves']="Összes lépés bejelentése";i['announceMoveFormat']="Lépésformátum";i['clickToGenerateOne']="Token létrehozása";i['configure']="Beállítás";i['configureVoiceNarration']="A lépések narrációjának beállításával a tektintetedet a táblán tarthatod.";i['debug']="Hibakeresés";i['dgtBoard']="DGT tábla";i['dgtBoardConnectivity']="DGT tábla csatlakoztatás";i['dgtBoardLimitations']="DGT tábla limitációk";i['dgtBoardRequirements']="DGT tábla követelmények";i['dgtConfigure']="DGT - Beállítások";i['downloadHere']=s("A szoftver innen letölthető: %s.");i['keywordFormatDescription']="A kulcsszavak JSON formátumban, melyekkel az általad használt nyelvre fordítjuk a lépéseket és eredményeket. Alapértelmezetten Angol, de ezt bármikor megváltoztathatod.";i['keywords']="Kulcsszavak";i['lichessAndDgt']="Lichess és DGT";i['lichessConnectivity']="Lichess csatlakoztatás";i['moveFormatDescription']="A Lichess a SAN jelölést használja, például \\\"Nf6\\\". Az UCI jelölést általában sakkmotorok használják, például \\\"g8f6\\\".";i['noSuitableOauthToken']="Nem található OAuth token.";i['openingThisLink']="az alábbi link megnyitásával";i['playWithDgtBoard']="Játék DGT táblával";i['reloadThisPage']="Oldal frissítése";i['selectAnnouncePreference']="Az IGEN opció bejelenti mindkét fél lépését, a NEM csak az ellenfeled lépését jelenti be.";i['speechSynthesisVoice']="Beszédszintetizátor";i['textToSpeech']="Szövegfelolvasás";i['thisPageAllowsConnectingDgtBoard']="Az alábbi oldal segítséget nyújt a DGT táblád csatlakoztatásához, hogy azt használhasd a játszmákban.";i['toConnectTheDgtBoard']=s("A DGT táblád csatlakoztatásához telepítened kell: %s.");i['validDgtOauthToken']="DGT játékhoz megfelelő OAuth token rendelkezésre áll.";i['verboseLogging']="Részletes naplózás";i['webSocketUrl']=s("%s WebSocket URL")})()