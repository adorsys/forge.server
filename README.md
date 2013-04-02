JBoss Forge RESTful Gernerator Server
=====================================

A RESTful JBoss Forge Sever that generates code based on a Template-Repository.
This reduces the startup time dramatically.
The generation result will be pushed in the given git repository.


Installation
============

`forge git-plugin https://github.com/adorsys/forge.server.git`

`forge-server setup --gitTemplateRepoUrl https://github.com/adorsys/forge.server-templates.git --autostart true`

Commands
========

`forge-server start`

`forge-server stop`

Template configuration
======================

`forge.server-templates.git/index.json`

    [ {path:"simplewebapp/simplewebapp.fsh", title:"Simplewebapp", description:"generates a app with a index.html"}]

Rest Interface
==============

List of possibble templates: [http://localhost:8081/forge](http://localhost:8081/forge)

Show the current job: [http://localhost:8081/forge/status](http://localhost:8081/forge/status)

Run the generator:

`curl -X POST -d "key=value\n" -H "Content-Type:application/json" \`
`http://localhost:8081/forge/JOBID/simplewebapp/simplewebapp.fsh?giturl=https://user:password@github.com/adorsys/forge.server.git`
