package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.exception.AddressBookBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class AddressBookServiceImpl implements AddressBookService {

    private static final String ADDRESS_NOT_FOUND_OR_FORBIDDEN = "地址不存在或无权访问";

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Override
    public List<AddressBook> list(AddressBook addressBook) {
        return addressBookMapper.list(addressBook);
    }

    @Override
    public void save(AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBook.setIsDefault(0);
        addressBookMapper.insert(addressBook);
    }

    @Override
    public AddressBook getById(Long id) {
        AddressBook addressBook = requireOwnedAddress(id);
        return addressBook;
    }

    @Override
    public void update(AddressBook addressBook) {
        requireOwnedAddress(addressBook.getId());
        addressBook.setUserId(null);
        addressBookMapper.update(addressBook);
    }

    @Override
    @Transactional
    public void setDefault(AddressBook addressBook) {
        requireOwnedAddress(addressBook.getId());

        addressBook.setIsDefault(0);
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBookMapper.updateIsDefaultByUserId(addressBook);

        addressBook.setIsDefault(1);
        addressBookMapper.update(addressBook);
    }

    @Override
    public void deleteById(Long id) {
        requireOwnedAddress(id);
        addressBookMapper.deleteById(id);
    }

    private AddressBook requireOwnedAddress(Long id) {
        if (id == null) {
            throw new AddressBookBusinessException(ADDRESS_NOT_FOUND_OR_FORBIDDEN);
        }
        AddressBook addressBook = addressBookMapper.getById(id);
        Long currentUserId = BaseContext.getCurrentId();
        if (addressBook == null || currentUserId == null || !currentUserId.equals(addressBook.getUserId())) {
            throw new AddressBookBusinessException(ADDRESS_NOT_FOUND_OR_FORBIDDEN);
        }
        return addressBook;
    }
}
