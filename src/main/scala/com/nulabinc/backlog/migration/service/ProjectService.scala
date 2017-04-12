package com.nulabinc.backlog.migration.service

import com.nulabinc.backlog.migration.domain.BacklogProject

/**
  * @author uchida
  */
trait ProjectService {

  def create(project: BacklogProject): Either[Throwable, BacklogProject]

  def optProject(projectKey: String): Option[BacklogProject]

  def projectOfKey(projectKey: String): BacklogProject

}
