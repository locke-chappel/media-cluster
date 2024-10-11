package io.github.lc.oss.mc.scheduler.app.service;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.lc.oss.commons.api.services.JsonService;
import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.ServiceResponse;
import io.github.lc.oss.mc.scheduler.app.model.Profile;
import io.github.lc.oss.mc.scheduler.app.repository.ProfileRepository;
import io.github.lc.oss.mc.scheduler.app.validation.ProfileValidator;

@Service
public class ProfileService extends AbstractService {
    @Autowired
    private ProfileRepository profileRepo;
    @Autowired
    private JsonService jsonService;
    @Autowired
    private ProfileValidator profileValidator;

    @Transactional(readOnly = true)
    public ServiceResponse<Profile> getDefaultProfile() {
        ServiceResponse<Profile> response = new ServiceResponse<>();
        io.github.lc.oss.mc.scheduler.app.entity.Profile profile = this.profileRepo.findAll().iterator().next();
        response.setEntity(this.toModel(profile));
        return response;
    }

    @Transactional(readOnly = true)
    public ServiceResponse<Profile> getProfile(String id) {
        ServiceResponse<Profile> response = new ServiceResponse<>();

        io.github.lc.oss.mc.scheduler.app.entity.Profile existing = this.profileRepo.findById(id).orElse(null);
        if (existing == null) {
            response.setMessages(this.toMessages(Messages.Application.NotFound));
            return response;
        }

        response.setEntity(this.toModel(existing));

        return response;
    }

    protected Profile toModel(io.github.lc.oss.mc.scheduler.app.entity.Profile profile) {
        Profile p = this.jsonService.from(profile.getJson(), Profile.class);
        p.setId(profile.getId());
        p.setModified(profile.getModified());
        p.setName(profile.getName());
        return p;
    }

    @Transactional
    public ServiceResponse<Profile> saveProfile(Profile request) {
        ServiceResponse<Profile> response = new ServiceResponse<>();

        io.github.lc.oss.mc.scheduler.app.entity.Profile profile;
        if (StringUtils.isBlank(request.getId())) {
            profile = new io.github.lc.oss.mc.scheduler.app.entity.Profile();
        } else {
            profile = this.profileRepo.findById(request.getId()).orElse(null);
            if (profile == null) {
                this.rollback();
                response.setMessages(this.toMessages(Messages.Application.NotFound));
                return response;
            }
        }

        Profile p = new Profile();
        p.setExt(request.getExt());
        p.setAudioArgs(request.getAudioArgs());
        p.setVideoArgs(request.getVideoArgs());
        p.setCommonArgs(request.getCommonArgs());
        p.setSliceLength(request.getSliceLength());

        profile.setName(request.getName());
        profile.setJson(this.jsonService.to(p));

        if (StringUtils.isBlank(request.getId())) {
            this.profileRepo.saveAndFlush(profile);
        }

        response.setEntity(this.toModel(profile));
        return response;
    }

    public ServiceResponse<Profile> validateJson(String json) {
        ServiceResponse<Profile> response = new ServiceResponse<>();

        Profile profile = this.jsonService.from(json, Profile.class);
        Set<Message> messages = this.profileValidator.validate(profile);
        if (!messages.isEmpty()) {
            response.setMessages(messages);
        } else {
            response.setEntity(profile);
        }

        return response;
    }
}
