/*
 * Copyright (c) Sourcy Software & Services GmbH 2015.
 *
 *     _____ ____   __  __ _____ _____ __  __    (_)____
 *    / ___// __ \ / / / // ___// ___// / / /   / // __ \
 *   (__  )/ /_/ // /_/ // /   / /__ / /_/ /_  / // /_/ /
 *  /____/ \____/ \__,_//_/    \___/ \__, /(_)/_/ \____/
 *                                  /____/
 *
 * Created by armin walland <a.walland@sourcy.io> on 2015-05-20.
 */

package io.sourcy.jirastatscollector

import java.io.File

import io.sourcy.jirastatscollector.JiraClient.extractAllIssues

import scala.sys.process.Process

object Main extends App {

  println(generateStats(Settings.epics))
  Settings.epics.foreach(epic => println(generateStats(Seq(epic))))
  println(s"\nStats generated with JiraGitStatsCollector: https://github.com/sourcy/jirastatscollector\n")

  def generateStats(epics: Seq[String]): String = {
    def pad(number: Int) = number.toString.reverse.padTo(10, " ").reverse.mkString
    val allIssues = extractAllIssues(epics)
    val gitLogOutput = Process(listChangedFilesCmd(allIssues), new File(Settings.gitRepository)).!!
    val changedFiles = filterLogOutput(gitLogOutput, line => !line.matches("^.{7} .*")).distinct
    val commits = filterLogOutput(gitLogOutput, line => line.matches("^.{7} .*"))
    val gitDiffOutput = Process(listChangedLinesCmd(allIssues), new File(Settings.gitRepository)).!!
    val changedLines = filterLogOutput(gitDiffOutput, _.matches("^[\\+\\-] .*"))
      .map(str => str.replaceFirst("^[\\+\\-]\\s+", ""))
      .distinct
    val addedRemoved = parseAddedRemoved(Process(listAddedRemovedCommand(allIssues), new File(Settings.gitRepository)).!!)
    val netChange = addedRemoved._1 - addedRemoved._2

    val output = new StringBuilder
    output.append(s"\nstats for issues ${epics.mkString(", ")}\n")
    output.append("\n")
    output.append(s"issues:                ${pad(allIssues.size)}\n")
    output.append(s"commits pushed:        ${pad(commits.size)}\n")
    output.append(s"files changed:         ${pad(changedFiles.size)}\n")
    output.append(s"change of lines +/-:   ${pad(netChange)}\n")
    output.append(s"lines changed:         ${pad(changedLines.size)}\n")
    output.toString()
  }

  def filterLogOutput(result: String, filter: (String) => Boolean): List[String] =
    result.split(System.lineSeparator).filter(filter).toList

  def listChangedFilesCmd(issues: Seq[String]) =
    Seq("git", "log", "--no-merges", "--name-only", "--oneline") ++ issues.map(issue => s"--grep=$issue")

  def listChangedLinesCmd(issues: Seq[String]) =
    Seq("git", "log", "--no-merges", "--full-diff", "-p", "--no-renames") ++ issues.map(issue => s"--grep=$issue")

  def listAddedRemovedCommand(issues: Seq[String]) =
    Seq("git", "log", "--numstat", "--pretty=\"%H\"") ++ issues.map(issue => s"--grep=$issue")

  def parseAddedRemoved(result: String): (Int, Int) = {
    val regex = """([0-9]+)""".r
    filterLogOutput(result, _.matches("[0-9]+\\s+[0-9]+\\s.+"))
      .map(regex.findAllIn(_))
      .map(it => (it.next().toInt, it.next().toInt))
      .unzip match {
      case (added, deleted) => (added.sum, deleted.sum)
    }
  }
}
