# Scheduler

Legend:
- O : repeat
- >|: end condition
  
## Group Scheduler

## Match Scheduler

FRI 20:02 Pre Match Job
-> CM_GAMEDAY_ID
<- MATCH LINEUP
<- MEMBER LINEUP
    - `GET https://stats.comunio.de/xhr/lineup.php?cid=13742756&s=2026&gid={CM_GAMEDAY_ID}&com=1`
    >| once
    - `GET https://stats.comunio.de/xhr/matchDetails.php?mid={CM_MATCH_ID}`
     O every 2 minutes
    >| max 20 minutes (20:22)
    >| first successful official lineup response
FRI 20:30 Running Match Job
-> CM_GAMEDAY_ID
<- MATCH LINEUP
    - `GET https://stats.comunio.de/xhr/matchDetails.php?mid={CM_MATCH_ID}`
     O every 5 minutes
    >| max 45 + 15 + 45 (22:15)
FRI 22:15 Post Match Job
-> CM_GAMEDAY_ID
<- MATCH LINEUP
    - `GET https://stats.comunio.de/xhr/matchDetails.php?mid={CM_MATCH_ID}`
     0 every 5 minutes
    >| hash of last n responses did not change OR
    >| max 15 minutes (22:30)
    