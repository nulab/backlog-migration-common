package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.domain.{
  BacklogCustomFieldSetting,
  BacklogStatusName,
  BacklogVersion
}
import com.nulabinc.backlog.migration.common.utils.Logging

/**
 * @author uchida
 */
class PropertyResolverImpl(
    customFieldSettingService: CustomFieldSettingService,
    issueTypeService: IssueTypeService,
    categoryService: IssueCategoryService,
    versionService: VersionService,
    resolutionService: ResolutionService,
    userService: UserService,
    statusService: StatusService,
    priorityService: PriorityService
) extends PropertyResolver
    with Logging {

  private[this] val customFieldSettings =
    customFieldSettingService.allCustomFieldSettings()
  private[this] val issueTypes  = issueTypeService.allIssueTypes()
  private[this] val categories  = categoryService.allIssueCategories()
  private[this] val versions    = versionService.allVersions()
  private[this] val resolutions = resolutionService.allResolutions()
  private[this] val users       = userService.allUsers()
  private[this] val statuses    = statusService.allStatuses()
  private[this] val priorities  = priorityService.allPriorities()

  private[this] def findVersion(name: String): Option[BacklogVersion] = {
    versions.find(_.name.trim == name.trim)
  }

  override def optResolvedVersionId(name: String): Option[Long] = {
    val optVersion = findVersion(name)
    if (optVersion.isEmpty) {
      logger.debug(
        s"[Version not found.]:${name}:${versions.map(_.name).mkString(",")}"
      )
    }
    optVersion.flatMap(_.optId)
  }

  override def optResolvedCustomFieldSetting(
      name: String
  ): Option[BacklogCustomFieldSetting] = {
    val optCustomFieldSetting = customFieldSettings.findByName(name)
    if (optCustomFieldSetting.isEmpty) {
      logger.debug(
        s"[Custom Field not found.]:${name}:${customFieldSettings.settings.map(_.name).mkString(",")}"
      )
    }
    optCustomFieldSetting
  }

  override def optResolvedIssueTypeId(name: String): Option[Long] = {
    val optIssueType = issueTypes.find(_.name.trim == name.trim)
    if (optIssueType.isEmpty) {
      logger.debug(
        s"[Issue Type not found.]:${name}:${issueTypes.map(_.name).mkString(",")}"
      )
    }
    optIssueType.flatMap(_.optId)
  }

  override def optResolvedCategoryId(name: String): Option[Long] = {
    val optIssueCategory = categories.find(_.name.trim == name.trim)
    if (optIssueCategory.isEmpty) {
      logger.debug(
        s"[Issue Category not found.]:${name}:${categories.map(_.name).mkString(",")}"
      )
    }
    optIssueCategory.flatMap(_.optId)
  }

  override def tryDefaultIssueTypeId(): Long = {
    val defaultIssueType = issueTypes.headOption match {
      case Some(head) => head
      case _          => throw new RuntimeException
    }
    defaultIssueType.optId match {
      case Some(issueTypeId) => issueTypeId
      case _                 => throw new RuntimeException
    }
  }

  override def optResolvedUserId(userId: String): Option[Long] = {
    val optUser = users.find(user => user.optUserId.getOrElse("") == userId)
    if (optUser.isEmpty) {
      logger.debug(
        s"[User not found.]:${userId}:${users.flatMap(_.optUserId).mkString(",")}"
      )
    }
    optUser.map(_.id)
  }

  override def tryResolvedStatusId(name: BacklogStatusName): Int =
    statuses.findByName(name).map(_.id).getOrElse {
      logger.debug(
        s"[Status not found.]:$name:${statuses.availableStatusNames.map(_.trimmed).mkString(",")}"
      )
      throw new RuntimeException(s"Status not found. ${name.trimmed}")
    }

  override def optResolvedResolutionId(name: String): Option[Long] = {
    val optResolution = resolutions.find(_.getName.trim == name.trim)
    if (optResolution.isEmpty)
      logger.debug(
        s"[Resolution not found.]:${name}:${resolutions.map(_.getName).mkString(",")}"
      )
    optResolution.map(_.getId)
  }

  override def optResolvedPriorityId(name: String): Option[Long] = {
    val optPriority =
      priorities.find(priority => priority.getName.trim == name.trim)
    if (optPriority.isEmpty) {
      logger.debug(
        s"[Priority not found.]:${name}:${priorities.map(_.getName).mkString(",")}"
      )
    }
    optPriority.map(_.getId)
  }

}
