package com.optimagrowth.license.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import com.optimagrowth.license.config.ServiceConfig;
import com.optimagrowth.license.model.License;
import com.optimagrowth.license.repository.LicenseRepository;

@Service
public class LicenseService {

    @Autowired
    MessageSource messages;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    ServiceConfig config;


    public License getLicense(String licenseId, String organizationId) {
        License license =
                licenseRepository.findByOrganizationIdAndLicenseId(
                        organizationId,
                        licenseId
                );

        if (license == null) {
            throw new IllegalArgumentException(
                    String.format(
                            messages.getMessage(
                                    "license.search.error.message",
                                    null,
                                    null
                            ),
                            licenseId,
                            organizationId
                    )
            );
        }

        return license.withComment(config.getProperty());
    }


    public License createLicense(License license) {
        license.setLicenseId(UUID.randomUUID().toString());
        licenseRepository.save(license);

        return license.withComment(config.getProperty());
    }


    public License updateLicense(License license) {

        License existingLicense =
                licenseRepository.findByOrganizationIdAndLicenseId(
                        license.getOrganizationId(),
                        license.getLicenseId()
                );

        if (existingLicense == null) {
            throw new IllegalArgumentException(
                    String.format(
                            messages.getMessage(
                                    "license.search.error.message",
                                    null,
                                    null
                            ),
                            license.getLicenseId(),
                            license.getOrganizationId()
                    )
            );
        }

        licenseRepository.save(license);

        return license.withComment(config.getProperty());
    }


    public String deleteLicense(String licenseId, String organizationId) {

        License license =
                licenseRepository.findByOrganizationIdAndLicenseId(
                        organizationId,
                        licenseId
                );

        if (license == null) {
            throw new IllegalArgumentException(
                    String.format(
                            messages.getMessage(
                                    "license.search.error.message",
                                    null,
                                    null
                            ),
                            licenseId,
                            organizationId
                    )
            );
        }

        licenseRepository.delete(license);

        return String.format(
                messages.getMessage(
                        "license.delete.message",
                        null,
                        null
                ),
                licenseId
        );
    }
}