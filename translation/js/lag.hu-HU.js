"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.lag)window.i18n.lag={};let i=window.i18n.lag;i['andNowTheLongAnswerLagComposedOfTwoValues']="Most pedig a hosszú válasz. A \\\"lag\\\" két egymástól független értékből tevődik össze (a kevesebb jobb):";i['isLichessLagging']="Akadozik a Lichess?";i['lagCompensation']="Lag kompenzálása";i['lagCompensationExplanation']="A Lichess kompenzálja a hálózati késést. Beleértve az állandósult lagot és a kiugró értékeket is. Vannak határértékek és tapasztalati értékek különböző időkorlátokhoz, emiatt a végeredmény egyformán jónak várható mindkét fél számára. Ezért ha jobban laggolsz az ellenfelednél az semmilyen hátránnyal nem jár!";i['lichessServerLatency']="Lichess szerver késése";i['lichessServerLatencyExplanation']="Ez az az idő, amire a szervernek szüksége van a lépések feldolgozásához. Ez mindenkinek ugyanannyi és csak a szerverek terhelésétől függ. Több játékos után magasabb, de a Lichess fejlesztői mindent megtesznek, hogy alacsonyan tartsák. Ritkán magasabb 10ms-nál.";i['measurementInProgressThreeDot']="Mérések folyamatban...";i['networkBetweenLichessAndYou']="A hálózat sebessége a játékos és a Lichess között";i['networkBetweenLichessAndYouExplanation']="Ez az az idő ami ahhoz kell, hogy a lépésed eljusson a Lichess szerverig és vissza. Függ a földrajzi távolságtól a Lichess szerverig (Franciaország), és az internet elérés minőségétől. A Lichess fejlesztői nem tudják megjavítani a wifidet, se felgyorsítani a fényt.";i['noAndYourNetworkIsBad']="Nem. És a hálózati kapcsolatod rossz.";i['noAndYourNetworkIsGood']="Nem. És a hálózati kapcsolatod jó.";i['yesItWillBeFixedSoon']="Igen. És hamarosan kijavítjuk!";i['youCanFindTheseValuesAtAnyTimeByClickingOnYourUsername']="Mindkét értéket megtalálod, ha a felhasználói nevedre kattintasz a felső sávon."})()