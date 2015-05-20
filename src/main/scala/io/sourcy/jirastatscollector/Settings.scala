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

import java.util.Properties

object Settings {
  private val props = loadProps

  def loadProps: Map[String, String] = {
    val props = new Properties()
    props.load(getClass.getResourceAsStream("application.properties"))
    import scala.collection.JavaConverters._
    Map() ++ props.asScala
  }

  def jiraUser = props(Property.jiraUser)

  def jiraPassword = props(Property.jiraPassword)

  def jiraUrl = props(Property.jiraUrl)

  def gitRepository = props(Property.gitRepository)

  def epics = props(Property.epics).split(",").toSeq
}

private object Property {
  val jiraUser = "jira_user"
  val jiraPassword = "jira_password"
  val jiraUrl = "jira_url"
  val gitRepository = "git_repository"
  val epics = "epics"
}
