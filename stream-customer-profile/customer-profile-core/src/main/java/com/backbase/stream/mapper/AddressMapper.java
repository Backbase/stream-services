package com.backbase.stream.mapper;

import com.backbase.customerprofile.api.integration.v1.model.EmailUpsertDto;
import com.backbase.customerprofile.api.integration.v1.model.PhoneNumberUpsertDto;
import com.backbase.customerprofile.api.integration.v1.model.PostalAddressUpsertDto;
import com.backbase.customerprofile.api.integration.v1.model.UrlUpsertDto;
import com.backbase.stream.legalentity.model.EmailAddress;
import com.backbase.stream.legalentity.model.PartyPostalAddress;
import com.backbase.stream.legalentity.model.PhoneAddress;
import com.backbase.stream.legalentity.model.Url;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AddressMapper {

    AddressMapper INSTANCE = Mappers.getMapper(AddressMapper.class);

    PostalAddressUpsertDto postalAddressToPostalAddressUpsertDto(PartyPostalAddress source);

    List<PostalAddressUpsertDto> postalAddressListToPostalAddressUpsertDtoList(List<PartyPostalAddress> source);


    PhoneNumberUpsertDto phoneAddressToPhoneNumberUpsertDto(PhoneAddress source);

    List<PhoneNumberUpsertDto> phoneAddressListToPhoneNumberUpsertDtoList(List<PhoneAddress> source);

    EmailUpsertDto emailAddressToEmailUpsertDto(EmailAddress source);

    List<EmailUpsertDto> emailAddressListToEmailUpsertDtoList(List<EmailAddress> source);

    UrlUpsertDto urlToUrlUpsertDto(Url source);

    List<UrlUpsertDto> urlListToUrlUpsertDtoList(List<Url> source);
}