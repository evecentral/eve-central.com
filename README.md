# EVE-Central.com Code Base - AGPL Release #


## Introduction ##

Welcome to the source code to EVE-Central.com, the oldest and still
the most popular cross region EVE market browser site (since 2006!).

I have decided to release the source code behind the site in
order to foster new development activities and allow for community
contributions to the core of the site. I believe in open source
software, and hope this model can be successfully applied to
EVE-Central.com

The license chosen here is nothing unusual, and does not prevent
running the code on another system (should the main EVE-Central servers cease
to exist for whatever reason). The goal is to maintain eve-central.com
as the primary instance for this codebase. If you so choose to run it
on another system, please remember that the code is licensed under the
Affero GPL, REQUIRING you to release the source to users who simply
visit the network service, in addition to retaing all copyright
notices where a web-user will see them. If you have more questions,
see the LICENSE file for details.

EVE-Central.com still deeply respects data privacy of our users. There
are components which will not be made public in the form of database
dumps. For example, market transaction information, user login
information, e-mail addresses, API keys, and corporation website contents. These
all include sensitive personal information.

Best of luck,
Yann Ramin <atrus@stackworks.net>

Code is available on GitHub:
https://github.com/theatrus/eve-central.com

Discussion is available on the Google Group:
http://groups.google.com/eve-central

Development wiki:
http://dev.eve-central.com/

## Next Generation Project ##


EVE-Central is currently under a revitalization project, and is transitioning architectures (abliet slowly).
In a way, we're going "3.0", and the new version number reflects that.

The new code base is in Scala (http://scala-lang.org) with Akka (http://akka.io/),
and Spray (http://spray.cc).


### Requirements (new tech, API and upload ingester) ###

- Java 7u5 (JDK)
- SBT (Scala Simple Build Tool) 0.11.x
- PostgreSQL 9.1+

### Requirements (old tech, web UI stack) ###

- Python 2.7 (3.x not supported)
  - psycopg2
  - Pyro (for the pathfinder server)
  - numpy (for statistic calculations)
  - CherryPy 3.2 (primary web server)
- PostgreSQL 9.0+

### High level view of directories ###

core/
        - SBT Driven Scala project (version "3.0")
        - This is the new API core and API ingester, but doesn't drive any of the web front end.

db/
        - Database schemas

web/
        - CherryPy launcher, web front end classes (Python)

lib/evecentral
        - Utility classes (Python)

periodic/
        - Batch programs which make EVE-Central tick (scripts, Python)

static_web/
        - Static HTML files (served by front-end server on the main
        server)

pathfinder_src/
        - C++ implementation of a shortest path server with Pyro node



