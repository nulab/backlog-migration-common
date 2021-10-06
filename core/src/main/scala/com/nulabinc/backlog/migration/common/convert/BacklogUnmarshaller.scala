package com.nulabinc.backlog.migration.common.convert

import better.files.{File => Path}
import com.nulabinc.backlog.migration.common.conf.BacklogPaths
import com.nulabinc.backlog.migration.common.convert.BacklogUnmarshaller.logger
import com.nulabinc.backlog.migration.common.domain._
import com.nulabinc.backlog.migration.common.utils.{IOUtil, Logging}
import spray.json.JsonParser

/**
 * @author
 *   uchida
 */
object BacklogUnmarshaller extends Logging {

  import com.nulabinc.backlog.migration.common.formatters.BacklogJsonProtocol._

  def wiki(path: Path): Option[BacklogWiki] =
    IOUtil.input(path).map(JsonParser(_).convertTo[BacklogWiki])

  def versions(backlogPaths: BacklogPaths): Seq[BacklogVersion] =
    IOUtil
      .input(backlogPaths.versionsJson)
      .map(json => {
        val backlogVersionsWrapper: BacklogVersionsWrapper =
          JsonParser(json).convertTo[BacklogVersionsWrapper]
        backlogVersionsWrapper.versions
      })
      .getOrElse(Seq.empty[BacklogVersion])

  def projectUsers(backlogPaths: BacklogPaths): Seq[BacklogUser] =
    IOUtil
      .input(backlogPaths.projectUsersJson)
      .map(json => {
        val wrapper: BacklogProjectUsersWrapper =
          JsonParser(json).convertTo[BacklogProjectUsersWrapper]
        wrapper.users
      })
      .getOrElse(Seq.empty[BacklogUser])

  def project(backlogPaths: BacklogPaths): BacklogProject =
    IOUtil
      .input(backlogPaths.projectJson)
      .map(json => {
        val backlogProjectsWrapper: BacklogProjectWrapper =
          JsonParser(json).convertTo[BacklogProjectWrapper]
        backlogProjectsWrapper.project
      })
      .getOrElse(
        throw new NoSuchElementException(
          s"No such project file. (${backlogPaths.projectJson})"
        )
      )

  def issue(path: Path): Option[BacklogEvent] = {
    try {
      logger.warn(s"BLG_INTG-157 path:${path.toString()} exists:${path.exists}")
      val json = IOUtil.input(path)
      logger.warn(s"BLG_INTG-157 content:${json}")
      val obj = json.map(JsonParser(_).convertTo[BacklogEvent])
      logger.warn(s"BLG_INTG-157 parse ok")
      obj
    } catch {
      case e: Throwable =>
        logger.warn("BLG_INTG-157 load error", e)
        None
    }
  }

  def issueTypes(backlogPaths: BacklogPaths): Seq[BacklogIssueType] =
    IOUtil
      .input(backlogPaths.issueTypesJson)
      .map(json => {
        val backlogIssueTypesWrapper: BacklogIssueTypesWrapper =
          JsonParser(json).convertTo[BacklogIssueTypesWrapper]
        backlogIssueTypesWrapper.issueTypes
      })
      .getOrElse(Seq.empty[BacklogIssueType])

  def issueCategories(backlogPaths: BacklogPaths): Seq[BacklogIssueCategory] =
    IOUtil
      .input(backlogPaths.issueCategoriesJson)
      .map(json => {
        val issueCategoriesWrapper: BacklogIssueCategoriesWrapper =
          JsonParser(json).convertTo[BacklogIssueCategoriesWrapper]
        issueCategoriesWrapper.issueCategories
      })
      .getOrElse(Seq.empty[BacklogIssueCategory])

  def groups(backlogPaths: BacklogPaths): Seq[BacklogGroup] =
    IOUtil
      .input(backlogPaths.groupsJson)
      .map(json => {
        val backlogGroupsWrapper: BacklogGroupsWrapper =
          JsonParser(json).convertTo[BacklogGroupsWrapper]
        backlogGroupsWrapper.groups
      })
      .getOrElse(Seq.empty[BacklogGroup])

  def backlogCustomFieldSettings(
      backlogPaths: BacklogPaths
  ): Seq[BacklogCustomFieldSetting] =
    IOUtil
      .input(backlogPaths.customFieldSettingsJson)
      .map(json => JsonParser(json).convertTo[BacklogCustomFieldSettings].settings)
      .getOrElse(Seq.empty[BacklogCustomFieldSetting])

}
