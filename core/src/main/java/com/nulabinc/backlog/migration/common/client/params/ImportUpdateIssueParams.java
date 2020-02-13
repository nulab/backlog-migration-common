package com.nulabinc.backlog.migration.common.client.params;

import com.nulabinc.backlog4j.api.option.UpdateIssueParams;
import com.nulabinc.backlog4j.http.NameValuePair;

/**
 * Parameters for import update issue API.
 *
 * @author nulab-inc
 */
public class ImportUpdateIssueParams extends UpdateIssueParams {

    public ImportUpdateIssueParams(long issueId) {
        super(issueId);
    }

    public ImportUpdateIssueParams(String issueKey) {
        super(issueKey);
    }

    public ImportUpdateIssueParams createdUserId(long createdUserId) {
        parameters.add(new NameValuePair("createdUserId", String.valueOf(createdUserId)));
        return this;
    }

    public ImportUpdateIssueParams created(String created) {
        parameters.add(new NameValuePair("created", created));
        return this;
    }

    public ImportUpdateIssueParams updatedUserId(long updatedUserId) {
        parameters.add(new NameValuePair("updatedUserId", String.valueOf(updatedUserId)));
        return this;
    }

    public ImportUpdateIssueParams updated(String updated) {
        parameters.add(new NameValuePair("updated", updated));
        return this;
    }

    public UpdateIssueParams singleEmptyListCustomField(long customFieldId) {
        parameters.add(new NameValuePair("customField_" + String.valueOf(customFieldId), ""));
        return this;
    }

}
