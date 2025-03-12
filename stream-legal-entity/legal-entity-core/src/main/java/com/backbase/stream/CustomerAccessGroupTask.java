package com.backbase.stream;

import com.backbase.accesscontrol.customeraccessgroup.api.service.v1.model.CustomerAccessGroupItem;
import com.backbase.stream.worker.model.StreamTask;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
public class CustomerAccessGroupTask extends StreamTask {

    private CustomerAccessGroupItem customerAccessGroup;

    public CustomerAccessGroupTask(CustomerAccessGroupItem data) {
        super(data.getName());
        this.customerAccessGroup = data;
    }

   @Override
   public String getName() {
       return customerAccessGroup.getName();
   } 
    
}
