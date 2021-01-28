package com.nulabinc.backlog.migration

import com.nulabinc.backlog.migration.common.conf.BacklogConstantValue
import com.nulabinc.backlog.migration.common.domain.{BacklogCustomFieldSetting, _}
import com.nulabinc.backlog4j.CustomField.FieldType
import com.nulabinc.backlog4j.Issue
import com.nulabinc.backlog4j.Issue.{PriorityType, ResolutionType}
import com.nulabinc.backlog4j.internal.json.customFields.DateCustomFieldSetting.InitialValueType

/**
 * @author uchida
 */
trait SimpleFixture {

  val userId1       = 1
  val userIdString1 = "test1"
  val userName1     = "name1"

  val user1 = BacklogUser(
    optId = Some(userId1),
    optUserId = Some(userIdString1),
    optPassword = None,
    name = userName1,
    optMailAddress = None,
    roleType = BacklogConstantValue.USER_ROLE
  )

  val userId2       = 2
  val userIdString2 = "test2"
  val userName2     = "name2"

  val user2 = BacklogUser(
    optId = Some(userId2),
    optUserId = Some(userIdString2),
    optPassword = None,
    name = userName2,
    optMailAddress = None,
    roleType = BacklogConstantValue.USER_ROLE
  )

  val userId3       = 3
  val userIdString3 = "test3"
  val userName3     = "name3"

  val user3 = BacklogUser(
    optId = Some(userId3),
    optUserId = Some(userIdString3),
    optPassword = None,
    name = userName3,
    optMailAddress = None,
    roleType = BacklogConstantValue.USER_ROLE
  )

  val sharedFile = BacklogSharedFile(dir = "/test", name = "attachment1")

  val projectId                      = 11
  val issueId1                       = 12
  val issueId2                       = 13
  val issueKey                    = "TEST-13"
  val summary                        = "summary"
  val optParentIssueId: Option[Long] = Some(issueId2)
  val description                    = "description"
  val startDate                      = "2017-03-15"
  val dueDate                        = "2017-03-16"
  val estimatedHours: Float          = 0.17f
  val actualHours: Float             = 0.18f
  val issueTypeName                  = "Task"
  val issueTypeId                    = 30
  val statusId                       = Issue.StatusType.Open.getIntValue
  val status                         = BacklogDefaultStatus(Id.backlogStatusId(1), BacklogStatusName("Open"), 1000)
  val statusId1                      = Issue.StatusType.InProgress.getIntValue
  val status1                        = BacklogDefaultStatus(Id.backlogStatusId(2), BacklogStatusName("In progress"), 2000)
  val categoryName1                  = "Development"
  val categoryName2                  = "Test"
  val categoryId1                    = 31
  val categoryId2                    = 32
  val versionName1                   = "0.0.1"
  val versionName2                   = "0.0.2"
  val versionName3                   = "0.0.3"
  val versionName4                   = "0.0.4"
  val versionId1                     = 33
  val versionId2                     = 34
  val versionId3                     = 35
  val versionId4                     = 36
  val priorityName                   = PriorityType.Normal.toString
  val priorityId                     = PriorityType.Normal.getIntValue
  val resolutionId                   = ResolutionType.Fixed.getIntValue
  val resolutionName                 = ResolutionType.Fixed.name()

  val issueCreated   = "2015-05-01T16:01:51+09:00"
  val issueUpdated   = "2015-05-01T16:01:51+09:00"
  val commentCreated = "2016-05-01T16:01:51+09:00"

  val operation =
    BacklogOperation(
      optCreatedUser = Some(user2),
      optCreated = Some(issueCreated),
      optUpdatedUser = Some(user2),
      optUpdated = Some(issueUpdated)
    )

  val item1 = BacklogItem(Some(1), "a")
  val item2 = BacklogItem(Some(2), "b")

  //Text
  val textCustomFieldId    = 1
  val textCustomFieldName  = "text"
  val textCustomFieldValue = "text value"
  val textCustomField = BacklogCustomField(
    textCustomFieldName,
    FieldType.Text.getIntValue,
    Some(textCustomFieldValue),
    Seq.empty[String]
  )
  val textCustomFieldSetting =
    BacklogCustomFieldSetting(
      optId = Some(textCustomFieldId),
      rawName = textCustomFieldName,
      description = "",
      typeId = FieldType.Text.getIntValue,
      required = true,
      applicableIssueTypes = Seq("Task"),
      delete = false,
      property = BacklogCustomFieldTextProperty(typeId = FieldType.Text.getIntValue)
    )

  //TextArea
  val textAreaCustomFieldId    = 2
  val textAreaCustomFieldName  = "text area"
  val textAreaCustomFieldValue = "text area value"
  val textAreaCustomField =
    BacklogCustomField(
      textAreaCustomFieldName,
      FieldType.TextArea.getIntValue,
      Some(textAreaCustomFieldValue),
      Seq.empty[String]
    )
  val textAreaCustomFieldSetting =
    BacklogCustomFieldSetting(
      optId = Some(textAreaCustomFieldId),
      rawName = textAreaCustomFieldName,
      description = "",
      typeId = FieldType.TextArea.getIntValue,
      required = true,
      applicableIssueTypes = Seq("Task"),
      delete = false,
      property = BacklogCustomFieldTextProperty(typeId = FieldType.TextArea.getIntValue)
    )

  //Numeric
  val numericCustomFieldId    = 3
  val numericCustomFieldName  = "numeric"
  val numericCustomFieldValue = "12.12"
  val numericCustomField =
    BacklogCustomField(
      numericCustomFieldName,
      FieldType.Numeric.getIntValue,
      Some(numericCustomFieldValue),
      Seq.empty[String]
    )
  val numericCustomFieldSetting =
    BacklogCustomFieldSetting(
      optId = Some(numericCustomFieldId),
      rawName = numericCustomFieldName,
      description = "",
      typeId = FieldType.Numeric.getIntValue,
      required = true,
      applicableIssueTypes = Seq("Task"),
      delete = false,
      property = BacklogCustomFieldNumericProperty(
        typeId = FieldType.Numeric.getIntValue,
        optInitialValue = Some(0.1f),
        optUnit = Some("m"),
        optMin = Some(0.1f),
        optMax = Some(100.5f)
      )
    )

  //Date
  val dateCustomFieldId    = 4
  val dateCustomFieldName  = "date"
  val dateCustomFieldValue = "2017-03-20"
  val dateCustomField =
    BacklogCustomField(
      dateCustomFieldName,
      FieldType.Date.getIntValue,
      Some(dateCustomFieldValue),
      Seq.empty[String]
    )
  val dateCustomFieldSetting =
    BacklogCustomFieldSetting(
      optId = Some(dateCustomFieldId),
      rawName = dateCustomFieldName,
      description = "",
      typeId = FieldType.Date.getIntValue,
      required = true,
      applicableIssueTypes = Seq("Task"),
      delete = false,
      property = BacklogCustomFieldDateProperty(
        typeId = FieldType.Date.getIntValue,
        optInitialDate = Some(
          BacklogCustomFieldInitialDate(
            InitialValueType.FixedDate.getIntValue,
            optDate = Some("2017-01-20"),
            optShift = None
          )
        ),
        optMin = Some("2017-01-20"),
        optMax = Some("2017-11-20")
      )
    )

  //SingleList
  val singleListCustomFieldId   = 5
  val singleListCustomFieldName = "singleList"
  val singleListCustomField =
    BacklogCustomField(
      singleListCustomFieldName,
      FieldType.SingleList.getIntValue,
      Some(item1.name),
      Seq.empty[String]
    )
  val singleListCustomFieldSetting =
    BacklogCustomFieldSetting(
      optId = Some(singleListCustomFieldId),
      rawName = singleListCustomFieldName,
      description = "",
      typeId = FieldType.SingleList.getIntValue,
      required = true,
      applicableIssueTypes = Seq("Task"),
      delete = false,
      property = BacklogCustomFieldMultipleProperty(
        typeId = FieldType.SingleList.getIntValue,
        items = Seq(item1, item2),
        allowAddItem = true,
        allowInput = true
      )
    )

  //MultipleList
  val multipleListCustomFieldId   = 6
  val multipleListCustomFieldName = "multipleList"
  val multipleListCustomField =
    BacklogCustomField(
      multipleListCustomFieldName,
      FieldType.MultipleList.getIntValue,
      None,
      Seq(item1.name, item2.name)
    )
  val multipleListCustomFieldSetting =
    BacklogCustomFieldSetting(
      optId = Some(multipleListCustomFieldId),
      rawName = multipleListCustomFieldName,
      description = "",
      typeId = FieldType.MultipleList.getIntValue,
      required = true,
      applicableIssueTypes = Seq("Task"),
      delete = false,
      property = BacklogCustomFieldMultipleProperty(
        typeId = FieldType.MultipleList.getIntValue,
        items = Seq(item1, item2),
        allowAddItem = true,
        allowInput = true
      )
    )

  //CheckBox
  val checkBoxCustomFieldId   = 7
  val checkBoxCustomFieldName = "checkBox"
  val checkBoxCustomField =
    BacklogCustomField(
      checkBoxCustomFieldName,
      FieldType.CheckBox.getIntValue,
      None,
      Seq(item1.name, item2.name)
    )
  val checkBoxCustomFieldSetting =
    BacklogCustomFieldSetting(
      optId = Some(checkBoxCustomFieldId),
      rawName = checkBoxCustomFieldName,
      description = "",
      typeId = FieldType.CheckBox.getIntValue,
      required = true,
      applicableIssueTypes = Seq("Task"),
      delete = false,
      property = BacklogCustomFieldMultipleProperty(
        typeId = FieldType.CheckBox.getIntValue,
        items = Seq(item1, item2),
        allowAddItem = true,
        allowInput = true
      )
    )

  //Radio
  val radioCustomFieldId   = 8
  val radioCustomFieldName = "radio"
  val radioCustomField =
    BacklogCustomField(
      radioCustomFieldName,
      FieldType.Radio.getIntValue,
      Some(item1.name),
      Seq.empty[String]
    )
  val radioCustomFieldSetting =
    BacklogCustomFieldSetting(
      optId = Some(radioCustomFieldId),
      rawName = radioCustomFieldName,
      description = "",
      typeId = FieldType.Radio.getIntValue,
      required = true,
      applicableIssueTypes = Seq("Task"),
      delete = false,
      property = BacklogCustomFieldMultipleProperty(
        typeId = FieldType.Radio.getIntValue,
        items = Seq(item1, item2),
        allowAddItem = true,
        allowInput = true
      )
    )

  val summaryChangeLog = BacklogChangeLog(
    field = BacklogConstantValue.ChangeLog.SUMMARY,
    optOriginalValue = None,
    optNewValue = Some("summary change"),
    optAttachmentInfo = None,
    optAttributeInfo = None,
    optNotificationInfo = None
  )

  val descriptionChangeLog = summaryChangeLog.copy(
    field = BacklogConstantValue.ChangeLog.DESCRIPTION,
    optNewValue = Some("description change")
  )

  val categoryChangeLog1 = summaryChangeLog.copy(
    field = BacklogConstantValue.ChangeLog.COMPONENT,
    optNewValue = Some(Seq(categoryName1, categoryName2).mkString(","))
  )

  val versionChangeLog1 = summaryChangeLog.copy(
    field = BacklogConstantValue.ChangeLog.VERSION,
    optNewValue = Some(Seq(versionName1, versionName2).mkString(","))
  )

  val milestoneChangeLog1 = summaryChangeLog.copy(
    field = BacklogConstantValue.ChangeLog.MILESTONE,
    optNewValue = Some(Seq(versionName3, versionName4).mkString(","))
  )

  val statusChangeLog1 = summaryChangeLog.copy(
    field = BacklogConstantValue.ChangeLog.STATUS,
    optOriginalValue = Some(status.name.trimmed),
    optNewValue = Some(status1.name.trimmed)
  )

  val assignerChangeLog1 = summaryChangeLog.copy(
    field = BacklogConstantValue.ChangeLog.ASSIGNER,
    optNewValue = Some(userIdString2)
  )

  val issueTypeChangeLog1 = summaryChangeLog.copy(
    field = BacklogConstantValue.ChangeLog.ISSUE_TYPE,
    optNewValue = Some(issueTypeName)
  )

  val startDateChangeLog1 = summaryChangeLog.copy(
    field = BacklogConstantValue.ChangeLog.START_DATE,
    optNewValue = Some(startDate)
  )

  val dueDateChangeLog1 = summaryChangeLog.copy(
    field = BacklogConstantValue.ChangeLog.LIMIT_DATE,
    optNewValue = Some(dueDate)
  )

  val priorityChangeLog1 = summaryChangeLog.copy(
    field = BacklogConstantValue.ChangeLog.PRIORITY,
    optNewValue = Some(priorityName)
  )

  val resolutionChangeLog1 = summaryChangeLog.copy(
    field = BacklogConstantValue.ChangeLog.RESOLUTION,
    optNewValue = Some(resolutionName)
  )

  val estimatedHoursChangeLog1 = summaryChangeLog.copy(
    field = BacklogConstantValue.ChangeLog.ESTIMATED_HOURS,
    optNewValue = Some(estimatedHours.toString)
  )

  val actualHoursHoursChangeLog1 = summaryChangeLog.copy(
    field = BacklogConstantValue.ChangeLog.ACTUAL_HOURS,
    optNewValue = Some(actualHours.toString)
  )

  val parentIssueChangeLog1 = summaryChangeLog.copy(
    field = BacklogConstantValue.ChangeLog.PARENT_ISSUE,
    optNewValue = Some(issueId2.toString)
  )

  val categoryChangeLog2 = summaryChangeLog.copy(
    field = BacklogConstantValue.ChangeLog.COMPONENT,
    optNewValue = Some("")
  )

  val versionChangeLog2 = summaryChangeLog.copy(
    field = BacklogConstantValue.ChangeLog.VERSION,
    optNewValue = Some("")
  )

  val milestoneChangeLog2 = summaryChangeLog.copy(
    field = BacklogConstantValue.ChangeLog.MILESTONE,
    optNewValue = Some("")
  )

  val assignerChangeLog2 = summaryChangeLog.copy(
    field = BacklogConstantValue.ChangeLog.ASSIGNER,
    optNewValue = Some("")
  )

  val startDateChangeLog2 = summaryChangeLog.copy(
    field = BacklogConstantValue.ChangeLog.START_DATE,
    optNewValue = Some("")
  )

  val dueDateChangeLog2 = summaryChangeLog.copy(
    field = BacklogConstantValue.ChangeLog.LIMIT_DATE,
    optNewValue = Some("")
  )

  val estimatedHoursChangeLog2 = summaryChangeLog.copy(
    field = BacklogConstantValue.ChangeLog.ESTIMATED_HOURS,
    optNewValue = Some("")
  )

  val actualHoursHoursChangeLog2 = summaryChangeLog.copy(
    field = BacklogConstantValue.ChangeLog.ACTUAL_HOURS,
    optNewValue = Some("")
  )

  val parentIssueChangeLog2 = summaryChangeLog.copy(
    field = BacklogConstantValue.ChangeLog.PARENT_ISSUE,
    optNewValue = None
  )

  val textCustomFieldChangeLog1 = BacklogChangeLog(
    field = textCustomFieldName,
    optOriginalValue = None,
    optNewValue = Some(textCustomFieldValue),
    optAttachmentInfo = None,
    optAttributeInfo = Some(
      BacklogAttributeInfo(
        optId = Some(textCustomFieldId),
        typeId = FieldType.Text.getIntValue.toString
      )
    ),
    optNotificationInfo = None
  )

  val textAreaCustomFieldChangeLog1 = BacklogChangeLog(
    field = textAreaCustomFieldName,
    optOriginalValue = None,
    optNewValue = Some(textAreaCustomFieldValue),
    optAttachmentInfo = None,
    optAttributeInfo = Some(
      BacklogAttributeInfo(
        optId = Some(textAreaCustomFieldId),
        typeId = FieldType.TextArea.getIntValue.toString
      )
    ),
    optNotificationInfo = None
  )

  val numericCustomFieldChangeLog1 = BacklogChangeLog(
    field = numericCustomFieldName,
    optOriginalValue = None,
    optNewValue = Some(numericCustomFieldValue),
    optAttachmentInfo = None,
    optAttributeInfo = Some(
      BacklogAttributeInfo(
        optId = Some(numericCustomFieldId),
        typeId = FieldType.Numeric.getIntValue.toString
      )
    ),
    optNotificationInfo = None
  )

  val dateCustomFieldChangeLog1 = BacklogChangeLog(
    field = dateCustomFieldName,
    optOriginalValue = None,
    optNewValue = Some(dateCustomFieldValue),
    optAttachmentInfo = None,
    optAttributeInfo = Some(
      BacklogAttributeInfo(
        optId = Some(dateCustomFieldId),
        typeId = FieldType.Date.getIntValue.toString
      )
    ),
    optNotificationInfo = None
  )

  val singleListCustomFieldChangeLog1 = BacklogChangeLog(
    field = singleListCustomFieldName,
    optOriginalValue = None,
    optNewValue = Some(item1.name),
    optAttachmentInfo = None,
    optAttributeInfo = Some(
      BacklogAttributeInfo(
        optId = Some(singleListCustomFieldId),
        typeId = FieldType.SingleList.getIntValue.toString
      )
    ),
    optNotificationInfo = None
  )

  val multipleListCustomFieldChangeLog1 = BacklogChangeLog(
    field = multipleListCustomFieldName,
    optOriginalValue = None,
    optNewValue = Some(Seq(item1.name, item2.name).mkString(",")),
    optAttachmentInfo = None,
    optAttributeInfo = Some(
      BacklogAttributeInfo(
        optId = Some(multipleListCustomFieldId),
        typeId = FieldType.MultipleList.getIntValue.toString
      )
    ),
    optNotificationInfo = None
  )

  val checkBoxCustomFieldChangeLog1 = BacklogChangeLog(
    field = checkBoxCustomFieldName,
    optOriginalValue = None,
    optNewValue = Some(Seq(item1.name, item2.name).mkString(",")),
    optAttachmentInfo = None,
    optAttributeInfo = Some(
      BacklogAttributeInfo(
        optId = Some(checkBoxCustomFieldId),
        typeId = FieldType.CheckBox.getIntValue.toString
      )
    ),
    optNotificationInfo = None
  )

  val radioCustomFieldChangeLog1 = BacklogChangeLog(
    field = radioCustomFieldName,
    optOriginalValue = None,
    optNewValue = Some(item1.name),
    optAttachmentInfo = None,
    optAttributeInfo = Some(
      BacklogAttributeInfo(
        optId = Some(radioCustomFieldId),
        typeId = FieldType.Radio.getIntValue.toString
      )
    ),
    optNotificationInfo = None
  )

  val commentContent = "test comment"

  val comment1 = BacklogComment(
    eventType = "comment",
    optIssueId = Some(issueId1),
    optContent = Some(commentContent),
    changeLogs = Seq(
      summaryChangeLog,
      descriptionChangeLog,
      categoryChangeLog1,
      versionChangeLog1,
      milestoneChangeLog1,
      statusChangeLog1,
      assignerChangeLog1,
      issueTypeChangeLog1,
      startDateChangeLog1,
      dueDateChangeLog1,
      priorityChangeLog1,
      resolutionChangeLog1,
      estimatedHoursChangeLog1,
      actualHoursHoursChangeLog1,
      parentIssueChangeLog1,
      textCustomFieldChangeLog1,
      textAreaCustomFieldChangeLog1,
      numericCustomFieldChangeLog1,
      dateCustomFieldChangeLog1,
      singleListCustomFieldChangeLog1,
      multipleListCustomFieldChangeLog1,
      checkBoxCustomFieldChangeLog1,
      radioCustomFieldChangeLog1
    ),
    notifications = Seq(
      BacklogNotification(optUser = Some(user3), optSenderUser = Some(user1))
    ),
    optCreatedUser = Some(user1),
    optCreated = Some(commentCreated)
  )

  val comment2 = BacklogComment(
    eventType = "comment",
    optIssueId = Some(issueId1),
    optContent = Some(commentContent),
    changeLogs = Seq(
      categoryChangeLog2,
      versionChangeLog2,
      milestoneChangeLog2,
      assignerChangeLog2,
      startDateChangeLog2,
      dueDateChangeLog2,
      estimatedHoursChangeLog2,
      actualHoursHoursChangeLog2,
      parentIssueChangeLog2
    ),
    notifications = Seq(
      BacklogNotification(optUser = Some(user3), optSenderUser = Some(user1))
    ),
    optCreatedUser = Some(user1),
    optCreated = Some(commentCreated)
  )

  val issue1 = BacklogIssue(
    eventType = "issue",
    id = issueId1,
    issueKey = issueKey,
    summary = BacklogIssueSummary(value = summary, original = summary),
    optParentIssueId = optParentIssueId,
    description = description,
    optStartDate = Some(startDate),
    optDueDate = Some(dueDate),
    optEstimatedHours = Some(estimatedHours),
    optActualHours = Some(actualHours),
    optIssueTypeName = Some(issueTypeName),
    status = status,
    categoryNames = Seq(categoryName1, categoryName2),
    versionNames = Seq(versionName1, versionName2),
    milestoneNames = Seq(versionName3, versionName4),
    priorityName = priorityName,
    optAssignee = Some(user1),
    attachments = Seq.empty[BacklogAttachment],
    sharedFiles = Seq.empty[BacklogSharedFile],
    customFields = Seq(
      textCustomField,
      textAreaCustomField,
      numericCustomField,
      dateCustomField,
      singleListCustomField,
      multipleListCustomField,
      checkBoxCustomField,
      radioCustomField
    ),
    notifiedUsers = Seq(user3),
    operation = operation
  )

  val issue2 = issue1.copy(id = issueId2)

}
