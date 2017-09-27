@echo off

mkdir public\compiled

set ts_apps=common chess ceval game tree chat

for %%t in (%ts_apps%) do @(
    call echo Building TypeScript: %%t
    call cd ui\%%t
    call yarn install --non-interactive
    call yarn run compile --non-interactive
    call cd ..\..
)

set apps=site challenge notify learn insight editor puzzle round analyse lobby tournament tournamentSchedule simul perfStat dasher

for %%a in (%apps%) do @(
  call echo Building: %%a
  call cd ui\%%a
  call yarn install --non-interactive
  call gulp dev
  call cd ..\..
)
