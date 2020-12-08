package com.backbase.stream.contacts.repository;

import com.backbase.stream.contacts.ContactsTask;
import com.backbase.stream.worker.repository.UnitOfWorkRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactsUnitOfWorkRepository extends UnitOfWorkRepository<ContactsTask, String> {
}
