"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.patron)window.i18n.patron={};let i=window.i18n.patron;i['becomePatron']="Lichess దాతగా అవ్వండి";i['donate']="దానం";i['freeAccount']="ఉచిత ఖాతా";i['freeChess']="ప్రతిఒక్కరికీ ఉచిత చెస్, ఎప్పటికీ!";i['lichessPatron']="Lichess కు విరాళం ఇచ్చిన వ్యక్తులు";i['newPatrons']="క్రొత్త దాతలు";i['noAdsNoSubs']="ప్రకటనలు లేవు, సభ్యత్వాలు లేవు, కానీ ఓపెన్ సోర్స్ మరియు అభిమానం.";i['thankYou']="మీ విరాళం కోసం ధన్యవాదాలు!";i['xBecamePatron']=s("%sLichess దాతగా అవ్వండి")})()