package com.nulabinc.backlog.migration.common.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.conf.BacklogConstantValue
import com.nulabinc.backlog.migration.common.convert.Writes
import com.nulabinc.backlog.migration.common.domain.BacklogUser
import com.nulabinc.backlog.migration.common.utils.Logging

/**
  * @author uchida
  */
class UserNameWrites @Inject()() extends Writes[String, BacklogUser] with Logging {

  override def writes(name: String): BacklogUser = {
    BacklogUser(None, None, None, name, None, BacklogConstantValue.USER_ROLE)
  }

}
