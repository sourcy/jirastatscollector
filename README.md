### Jira Git Stats Collector

This tiny tool will look up epics which you provide in 
the config file in the jira API, get all child issues 
and their subtasks, git --grep your git repository for
those issues and print out some statistics.

#### Configure and run

```bash
cd src/main/resources/io/sourcy/jirastatscollector
cp application.properties.example application.properties
vim application.properties
cd ../../../../../..
sbt run
```

