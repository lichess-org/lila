@echo off

mkdir public\compiled

set ts_apps=draughtsground common draughts ceval game chat tree nvui

call yarn install

call echo Building css
call cd ui
call gulp css-dev
call cd ..

for %%t in (%ts_apps%) do @(
    call echo Building TypeScript: %%t
    call cd ui\%%t
    call yarn run compile
    call cd ..\..
)

call echo Building: draughtsground stand alone
call cd ui\draughtsground
call gulp dev
call gulp prod
call cd ..\..
call xcopy /y public\compiled\draughtsground.min.js public\javascripts\vendor\

set apps=site challenge notify round analyse editor puzzle lobby tournament tournamentSchedule tournamentCalendar simul dasher cli speech palantir

for %%a in (%apps%) do @(
  call echo Building: %%a
  call cd ui\%%a
  call gulp dev
  call cd ..\..
)

call echo Building: editor.min
call cd ui\editor
call gulp prod
call cd ..\..
