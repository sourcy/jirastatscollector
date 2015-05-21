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

#### Example output 
```
scanning 3 epics...log output..changed files..commits..diff output..changed lines..done.

issues:           53
commits pushed:   266
files changed:    2178
lines changed:    323819
```