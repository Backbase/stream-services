package com.backbase.stream.mapper;

import com.backbase.dbs.userprofile.model.CreateUserProfile;
import com.backbase.dbs.userprofile.model.GetUserProfile;
import com.backbase.dbs.userprofile.model.MultiValued;
import com.backbase.dbs.userprofile.model.Name;
import com.backbase.dbs.userprofile.model.ReplaceUserProfile;
import com.backbase.stream.legalentity.model.Multivalued;
import com.backbase.stream.legalentity.model.User;
import com.backbase.stream.legalentity.model.UserProfile;
import java.util.ArrayList;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper
public abstract class UserProfileMapper {

    @Mapping(source = "internalId", target = "userId")
    @Mapping(source = "externalId", target = "userName")
    @Mapping(source = "user", target = "name", qualifiedByName = "mapName")
    @Mapping(source = "fullName", target = "displayName")
    @Mapping(source = "userProfile.nickName", target = "nickName")
    @Mapping(source = "userProfile.profileUrl", target = "profileUrl")
    @Mapping(source = "userProfile.personalInformation", target = "personalInformation")
    @Mapping(source = "userProfile.identificationDetails", target = "identificationDetails")
    @Mapping(source = "userProfile.title", target = "title")
    @Mapping(source = "userProfile.userType", target = "userType")
    @Mapping(source = "userProfile.preferredLanguage", target = "preferredLanguage")
    @Mapping(source = "userProfile.locale", target = "locale")
    @Mapping(source = "userProfile.timezone", target = "timezone")
    @Mapping(source = "userProfile.active", target = "active")
    @Mapping(source = "user", target = "emails", qualifiedByName = "mapEmails")
    @Mapping(source = "user", target = "phoneNumbers", qualifiedByName = "mapPhones")
    @Mapping(source = "userProfile.ims", target = "ims")
    @Mapping(source = "userProfile.photos", target = "photos")
    @Mapping(source = "userProfile.addresses", target = "addresses")
    @Mapping(source = "userProfile.x509Certificates", target = "x509Certificates")
    @Mapping(source = "userProfile.extended", target = "extended")
    public abstract CreateUserProfile toCreate(User user);

    @Named("mapName")
    protected Name mapName(User user) {

        Name name = new Name();
        // if Name in UserProfile is null, split full name by 1st space
        if (user.getUserProfile().getName() == null) {
            int firstSpace = user.getFullName().indexOf(" ");
            name.setGivenName(user.getFullName().substring(0, firstSpace));
            name.setFamilyName(user.getFullName().substring(firstSpace).trim());
            name.setFormatted(user.getFullName());
        } else {
            name = map(user.getUserProfile().getName());
        }
        return name;
    }

    abstract Name map(com.backbase.stream.legalentity.model.Name name);

    @Named("mapEmails")
    protected List<MultiValued> mapEmails(User user) {

        List<MultiValued> emails = new ArrayList<>();
        MultiValued mainAddress = new MultiValued()
            .primary(true)
            .value(user.getEmailAddress().getAddress());
        emails.add(mainAddress);
        emails.addAll(mapAll(user.getUserProfile().getAdditionalEmails()));
        return emails;
    }

    @Named("mapPhones")
    protected List<MultiValued> mapPhones(User user) {

        List<MultiValued> phones = new ArrayList<>();
        MultiValued mainPhone = new MultiValued()
            .primary(true)
            .value(user.getMobileNumber().getNumber());
        phones.add(mainPhone);
        phones.addAll(mapAll(user.getUserProfile().getAdditionalPhoneNumbers()));
        return phones;
    }

    abstract MultiValued map(Multivalued multivalued);

    abstract List<MultiValued> mapAll(List<Multivalued> multivalued);

    public abstract ReplaceUserProfile toUpdate(CreateUserProfile userItem);

    public abstract UserProfile toUserProfile(GetUserProfile userItem);

}
