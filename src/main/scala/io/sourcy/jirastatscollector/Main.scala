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

  print(s"\ncalculating stats for issues ${Settings.epics.mkString(", ")}\n")
  print(s"\nscanning ${Settings.epics.size} epics..")

  private val allIssues = extractAllIssues(Settings.epics)
  print("changed files..")
  private val gitLogOutput = Process(listChangedFilesCmd(allIssues), new File(Settings.gitRepository)).!!
  private val changedFiles = filterLogOutput(gitLogOutput, line => !line.matches("^.{7} .*")).distinct
  print("commits..")
  private val commits = filterLogOutput(gitLogOutput, line => line.matches("^.{7} .*"))
  print("changed lines..")
  private val gitDiffOutput = Process(listChangedLinesCmd(allIssues), new File(Settings.gitRepository)).!!
  private val changedLines = filterLogOutput(gitDiffOutput, _.matches("^[\\+\\-] .*"))
    .map(str => str.replaceFirst("^[\\+\\-]\\s+", ""))
    .distinct
  print("added vs removed..")
  private val addedRemoved = parseAddedRemoved(Process(listAddedRemovedCommand(allIssues), new File(Settings.gitRepository)).!!)
  private val netChange = addedRemoved._1 - addedRemoved._2
  println("done.")

  println("")
  println(s"issues:                ${pad(allIssues.size)}")
  println(s"commits pushed:        ${pad(commits.size)}")
  println(s"files changed:         ${pad(changedFiles.size)}")
  println(s"change of lines +/-:   ${pad(netChange)}")
  println(s"lines changed:         ${pad(changedLines.size)}")

  println(s"\nStats generated with JiraGitStatsCollector: https://github.com/sourcy/jirastatscollector")

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

  def pad(number: Int) = number.toString.reverse.padTo(10, " ").reverse.mkString
}
