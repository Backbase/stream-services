package com.backbase.stream.contact.repository;

import com.backbase.stream.contact.ContactsTask;
import com.backbase.stream.worker.repository.UnitOfWorkRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactsUnitOfWorkRepository extends UnitOfWorkRepository<ContactsTask, String> {
}
