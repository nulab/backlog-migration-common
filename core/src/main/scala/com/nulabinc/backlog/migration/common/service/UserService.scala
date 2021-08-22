package com.nulabinc.backlog.migration.common.service

import com.nulabinc.backlog.migration.common.domain.BacklogUser

/**
 * @author
 *   uchida
 */
trait UserService {

  def allUsers(): Seq[BacklogUser]

  def myself(): BacklogUser

}
