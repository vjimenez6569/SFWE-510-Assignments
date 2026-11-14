
package com.optimagrowth.license.events.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;

import com.optimagrowth.license.events.CustomChannels;
import com.optimagrowth.license.events.model.OrganizationChangeModel;
import com.optimagrowth.license.repository.OrganizationRedisRepository;

@EnableBinding(CustomChannels.class)
public class OrganizationChangeHandler {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationChangeHandler.class);

    @Autowired
    private OrganizationRedisRepository redisRepository;

    @StreamListener("inboundOrgChanges")
    public void loggerSink(OrganizationChangeModel organization) {

        logger.debug("Received a message of type {}", organization.getType());

        String action = organization.getAction();
        String organizationId = organization.getOrganizationId();

        switch (action) {
            case "GET":
                logger.debug("Received a GET event for organization {}", organizationId);
                break;

            case "SAVE":
            case "UPDATE":
            case "DELETE":
                logger.debug("Received a {} event for organization {}", action, organizationId);
                redisRepository.deleteById(organizationId);
                logger.info("Removed organization {} from Redis cache", organizationId);
                break;

            default:
                logger.error("Received an UNKNOWN event of type {}", action);
                break;
        }
    }
}
