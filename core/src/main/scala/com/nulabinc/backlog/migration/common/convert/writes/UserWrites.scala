package com.nulabinc.backlog.migration.common.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.Writes
import com.nulabinc.backlog.migration.common.domain.BacklogUser
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog4j.User

/**
 * @author
 *   uchida
 */
class UserWrites @Inject() () extends Writes[User, BacklogUser] with Logging {

  override def writes(user: User): BacklogUser = {
    BacklogUser(
      optId = Some(user.getId),
      optUserId = Option(user.getUserId),
      optPassword = None,
      name = user.getName,
      optMailAddress = Option(user.getMailAddress),
      roleType = user.getRoleType.getIntValue
    )
  }

}
