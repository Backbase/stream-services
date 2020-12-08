package com.backbase.stream.contacts;

import com.backbase.dbs.contact.service.model.ContactsbulkingestionPostResponseBody;
import com.backbase.dbs.contact.service.model.ContactsbulkingestionRequest;
import com.backbase.dbs.limit.service.model.CreateLimitRequest;
import com.backbase.dbs.limit.service.model.CreateLimitResponse;
import com.backbase.stream.worker.model.StreamTask;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ContactsTask extends StreamTask {

    public ContactsTask(String unitOfWorkId, ContactsbulkingestionRequest data) {
        super(unitOfWorkId);
        this.data = data;
    }

    private ContactsbulkingestionRequest data;
    private ContactsbulkingestionPostResponseBody response;

    @Override
    public String getName() {
        return "contact";
    }
}
