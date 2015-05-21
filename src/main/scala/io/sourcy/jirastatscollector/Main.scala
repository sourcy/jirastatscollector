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
  print(s"\nscanning ${Settings.epics.size} epics...")

  private val allIssues = extractAllIssues(Settings.epics)
  print("changed files..")
  private val gitLogOutput = Process(listChangedFilesCmd(allIssues), new File(Settings.gitRepository)).!!
  private val changedFiles = filterLogOutput(gitLogOutput, line => !line.matches("^.{7} .*")).distinct
  print("commits..")
  private val commits = filterLogOutput(gitLogOutput, line => line.matches("^.{7} .*"))
  print("changed lines..")
  private val gitDiffOutput = Process(listChangedLinesCmd(allIssues), new File(Settings.gitRepository)).!!
  private val changedLines = filterLogOutput(gitDiffOutput, line => line.matches("^[\\+\\-] .*"))
  println("done.")

  println("")
  println(s"issues:           ${allIssues.size}")
  println(s"commits pushed:   ${commits.size}")
  println(s"files changed:    ${changedFiles.size}")
  println(s"lines changed:    ${changedLines.size}")

  def filterLogOutput(result: String, filter: (String) => Boolean): List[String] =
    result.split("\n").filter(filter).toList

  def listChangedFilesCmd(issues: Seq[String]) =
    Seq("git", "log", "--no-merges", "--name-only", "--oneline") ++ issues.map(issue => s"--grep=$issue")

  def listChangedLinesCmd(issues: Seq[String]) =
    Seq("git", "log", "--no-merges", "--full-diff", "-p", "--no-renames") ++ issues.map(issue => s"--grep=$issue")
}
