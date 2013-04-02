forge.server
============

A RESTful JBoss Forge Sever that generates code based on a Template-Repository.

Installation
============

`forge git-plugin https://github.com/adorsys/forge.server.git`

`forge-server setup --gitTemplateRepoUrl https://github.com/adorsys/forge.server-templates.git --autostart true`

Commands
========

`forge-server start`

`forge-server stop`

Rest Interface
==============

List of possibble Teamplates: [http://localhost:8081/forge](http://localhost:8081/forge)

Show current job: [http://localhost:8081/forge](http://localhost:8081/forge/status)



