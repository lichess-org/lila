"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.emails)window.i18n.emails={};let i=window.i18n.emails;i['common_contact']=s("ഞങ്ങളുമായി ബന്ധപ്പെടാൻ ദയവായി %s ഉപയോഗിക്കുക.");i['common_note']=s("നിങ്ങളുടെ %s -ന്റെ ഉപയോഗവുമായി ബന്ധപ്പെട്ട സർവീസ് email ആണിത്.");i['common_orPaste']="(മൗസ് അമർത്തിയിട്ട് ഒന്നും സംഭവിക്കുന്നില്ല? നിങ്ങളുടെ ബ്രൗസറിൽ paste ചെയ്യാൻ ശ്രമിച്ചു നോക്കൂ!)";i['emailChange_click']="ഈ email നിങ്ങളുടേതാണെന്ന് ഉറപ്പു വരുത്തുന്നതിനു വേണ്ടി, താഴെ കാണുന്ന ലിങ്ക് അമർത്തുക:";i['emailChange_intro']="നിങ്ങളുടെ email address മാറ്റാനുള്ള അപേക്ഷ ലഭിച്ചിരിക്കുന്നു.";i['emailChange_subject']=s("പുതിയ email address സ്ഥിരീകരിക്കുക, %s");i['emailConfirm_click']="ലിങ്ക് അമർത്തി അക്കൗണ്ട് സജ്ജമാക്കൂ:";i['emailConfirm_ignore']="Lichess ഉമായി നിങ്ങൾ രജിസ്റ്റർ ചെയ്തിട്ടില്ലെങ്കിൽ ഈ മെസ്സേജ് നിങ്ങൾക്ക് അവഗണിക്കാം.";i['emailConfirm_subject']=s("നിങ്ങളുടെ lichess.org അക്കൗണ്ട് confirm ചെയ്യൂ, %s");i['logInToLichess']=s("Lichess.org -ലേക്ക് ലോഗിൻ ചെയ്യുക, %s");i['passwordReset_clickOrIgnore']="നിങ്ങൾ ഈ അപേക്ഷ സമർപ്പിച്ചിട്ടുണ്ടെങ്കിൽ, താഴെ കാണുന്ന ലിങ്ക് അമർത്തൂ. അല്ലെങ്കിൽ, ഈ മെസ്സേജ് നിങ്ങൾക്ക് അവഗണിക്കാം.";i['passwordReset_intro']="നിങ്ങളുടെ പാസ്സ്‌വേർഡ് മാറ്റാനുള്ള അപേക്ഷ ഞങ്ങൾക്കു ലഭിച്ചിരിക്കുന്നു!";i['passwordReset_subject']=s("നിങ്ങളുടെ lichess.org പാസ്സ്‌വേഡ് മാറ്റുക, %s");i['welcome_subject']=s("Lichess.org -ലേക്ക് നിങ്ങൾക്കു സ്വാഗതം, %s");i['welcome_text']=s("നിങ്ങൾ വിജയകരമായി https://www.lichess.org -ൽ അക്കൗണ്ട് ഉണ്ടാക്കിയിരിക്കുന്നു.\n\nഇത് നിങ്ങളുടെ profile page: %1$s. അതിൽ മാറ്റങ്ങൾ വരുത്താൻ %2$s അമർത്തുക.\n\nആസ്വദിക്കൂ, നിങ്ങളുടെ കരുക്കൾ എപ്പൊഴും എതിർരാജാവിന്റെ ദിശയിലേക്കുള്ള വഴി കണ്ടെത്തട്ടേ!")})()