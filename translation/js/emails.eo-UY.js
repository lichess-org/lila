"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.emails)window.i18n.emails={};let i=window.i18n.emails;i['common_contact']=s("Por kontakti nin, bonvolu uzi %s.");i['common_note']=s("Ĉi tiu estas serva retpoŝto rilate al via uzo de %s.");i['common_orPaste']="(Ĉu alklaki ne funkcias? Provu elpoŝigi ĝin en vian retumilon!)";i['emailChange_click']="Por konfirmi ke vi havas aliron al ĉi tiu retpoŝtadreso, bonvolu alklaku la suban ligilon:";i['emailChange_intro']="Vi petas ŝanĝi vian retpoŝtadreso.";i['emailChange_subject']=s("Konfirmu vian novan retpoŝtadreson, %s");i['emailConfirm_click']="Alklaku la ligilon por ebligi vian konton ĉe Lichess:";i['emailConfirm_ignore']="Se vi ne registris konton ĉe Lichess, vi povas ignori ĉi tiun mesaĝon.";i['emailConfirm_subject']=s("Konfirmu vian konton ĉe lichess.org, %s");i['logInToLichess']=s("Ensalutu al lichess.org, %s");i['passwordReset_clickOrIgnore']="Se vi faris ĉi tiun peton, alklaku la suban ligilon. Se ne, vi povas ignori ĉi tiun retmesaĝon.";i['passwordReset_intro']="Ni ricevis peton por restarigi la pasvorton por via konto.";i['passwordReset_subject']=s("Restarigu vian pasvorton de lichess.org, %s");i['welcome_subject']=s("Bonvenon al lichess.org, %s");i['welcome_text']=s("Vi sukcese kreis vian konton ĉe https://lichess.org.\n\nJen via profilo: %1$s. Vi povas redakti ĝin ĉe %2$s.\n\nAmuziĝu, kaj viaj pecoj ĉiam trovu siajn vojojn al la kontraŭa reĝo!")})()