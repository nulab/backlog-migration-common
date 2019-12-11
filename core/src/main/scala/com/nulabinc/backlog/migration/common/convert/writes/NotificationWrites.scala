package com.nulabinc.backlog.migration.common.convert.writes

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.{Convert, Writes}
import com.nulabinc.backlog.migration.common.domain.BacklogNotification
import com.nulabinc.backlog.migration.common.utils.Logging
import com.nulabinc.backlog4j.Notification

/**
  * @author uchida
  */
private[common] class NotificationWrites @Inject()(implicit val userWrites: UserWrites)
    extends Writes[Notification, BacklogNotification]
    with Logging {

  override def writes(notification: Notification): BacklogNotification = {
    BacklogNotification(optUser = Option(notification.getUser).map(Convert.toBacklog(_)),
                        optSenderUser = Option(notification.getSender).map(Convert.toBacklog(_)))
  }

}
