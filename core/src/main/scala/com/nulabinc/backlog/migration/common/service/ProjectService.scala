package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.domain.BacklogProject

/**
 * @author uchida
 */
trait ProjectService {

  def create(project: BacklogProject): Either[Throwable, BacklogProject]

  def optProject(projectKey: String): Option[BacklogProject]

  def projectOfKey(projectKey: String): BacklogProject

}
