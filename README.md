# EVE-Central.com Code Base - AGPL Release #


## Introduction ##

Welcome to the source code to EVE-Central.com, the oldest and still
the most popular cross region EVE market browser site (since 2006!).

Code is available on GitHub:
https://github.com/theatrus/eve-central.com

Discussion is available on the Google Group:
http://groups.google.com/eve-central

Development wiki:
http://dev.eve-central.com/


### Requirements (Scala core data API and upload ingester) ###

- Java 7 or 8 (JDK)
- SBT (Scala Simple Build Tool) 0.13.x
- PostgreSQL 9.1+

### Requirements (Python web UI stack) ###

- Python 2.7 (3.x not supported)
  - psycopg2
  - Pyro (for the pathfinder server)
  - numpy (for statistic calculations)
  - CherryPy 3.2 (primary web server)
- PostgreSQL 9.0+
  
A "requirements.txt" file is present to help you build a virtualenv environment.

### High level view of directories ###

core/
- SBT Driven Scala project
- This is the API core and API ingester, but doesn't drive any of the
  web front end directly (though the web front end makes copious calls
  to the API)

db/
- Database schemas

web/
- CherryPy launcher, web front end classes (Python)

lib/evecentral
- Utility classes (Python)

periodic/
- Batch programs which make EVE-Central tick (scripts, Python)

static_web/
- Static HTML files (served by front-end server on the main server)


