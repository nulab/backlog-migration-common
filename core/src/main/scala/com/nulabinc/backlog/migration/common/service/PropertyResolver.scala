package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.domain.{BacklogCustomFieldSetting, _}

/**
 * 
 * @author
 *   uchida
 */
trait PropertyResolver {

  def optResolvedVersionId(name: String): Option[Long]

  def optResolvedCustomFieldSetting(
      name: String
  ): Option[BacklogCustomFieldSetting]

  def optResolvedCategoryId(name: String): Option[Long]

  def optResolvedIssueTypeId(name: String): Option[Long]

  def tryDefaultIssueTypeId(): Long

  def optResolvedUserId(userId: String): Option[Long]

  def tryResolvedStatusId(name: BacklogStatusName): Id[BacklogStatus]

  def optResolvedResolutionId(name: String): Option[Long]

  def optResolvedPriorityId(name: String): Option[Long]

}
